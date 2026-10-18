package cl.miradio.app.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.annotation.OptIn
import androidx.core.content.getSystemService
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Segunda línea de defensa: cuando el reproductor termina en error por un problema
 * de red, vuelve a conectar el stream en vivo con backoff exponencial. Si no hay
 * red, espera a que vuelva (vía [ConnectivityManager]) y reconecta de inmediato.
 *
 * Solo reconecta si el usuario quería estar escuchando (`playWhenReady`).
 * [scope] debe correr en el hilo principal, igual que el [player].
 */
@OptIn(UnstableApi::class)
class StreamReconnectionHandler(
    context: Context,
    private val player: Player,
    private val scope: CoroutineScope,
    private val onReconnectingChanged: (Boolean) -> Unit,
) : Player.Listener {

    private val connectivityManager = requireNotNull(context.getSystemService<ConnectivityManager>())

    private var attempts = 0
    private var retryJob: Job? = null
    private var waitingForNetwork = false

    var isReconnecting: Boolean = false
        private set(value) {
            if (field != value) {
                field = value
                onReconnectingChanged(value)
            }
        }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // Llega en un hilo del sistema; el reproductor solo se toca en el principal.
            scope.launch {
                if (waitingForNetwork) {
                    waitingForNetwork = false
                    reconnectNow()
                }
            }
        }
    }

    init {
        player.addListener(this)
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    override fun onPlayerError(error: PlaybackException) {
        if (!player.playWhenReady || !error.isRecoverableNetworkError()) {
            reset()
            return
        }
        if (attempts >= MAX_ATTEMPTS) {
            // Se rinde: la UI muestra el error y el usuario puede reintentar con Play.
            reset()
            return
        }
        isReconnecting = true
        if (!hasInternet()) {
            waitingForNetwork = true
            return
        }
        scheduleRetry()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) reset()
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (!playWhenReady) reset()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        reset()
    }

    fun release() {
        reset()
        player.removeListener(this)
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }

    private fun scheduleRetry() {
        retryJob?.cancel()
        val delayMs = min(BASE_DELAY_MS shl attempts.coerceAtMost(MAX_BACKOFF_EXPONENT), MAX_DELAY_MS)
        retryJob = scope.launch {
            delay(delayMs)
            reconnectNow()
        }
    }

    private fun reconnectNow() {
        retryJob?.cancel()
        retryJob = null
        if (!player.playWhenReady || player.playerError == null) return
        attempts++
        // En un stream en vivo hay que volver al borde en directo, no a la posición vieja.
        player.seekToDefaultPosition()
        player.prepare()
    }

    private fun reset() {
        retryJob?.cancel()
        retryJob = null
        attempts = 0
        waitingForNetwork = false
        isReconnecting = false
    }

    private fun hasInternet(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun PlaybackException.isRecoverableNetworkError(): Boolean = when (errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,
        PlaybackException.ERROR_CODE_TIMEOUT,
        -> true
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
            (cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode?.let { it >= 500 } == true
        else -> false
    }

    private companion object {
        const val MAX_ATTEMPTS = 8
        const val BASE_DELAY_MS = 2_000L
        const val MAX_DELAY_MS = 30_000L
        const val MAX_BACKOFF_EXPONENT = 4
    }
}
