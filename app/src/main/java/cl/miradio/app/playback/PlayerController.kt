package cl.miradio.app.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import cl.miradio.app.domain.model.Station
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await

/**
 * Puente entre la UI y [PlaybackService] a través de un [MediaController].
 *
 * La conexión sigue el ciclo de vida visible de la app ([connect] en `onStart`,
 * [disconnect] en `onStop`); la reproducción continúa en el servicio aunque la UI
 * se desconecte. Todas las llamadas deben hacerse desde el hilo principal.
 */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publishState()
    }

    private val controllerListener = object : MediaController.Listener {
        override fun onExtrasChanged(controller: MediaController, extras: Bundle) = publishState()
        override fun onDisconnected(controller: MediaController) {
            this@PlayerController.controller = null
            controllerFuture = null
        }
    }

    fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token)
            .setListener(controllerListener)
            .buildAsync()
        controllerFuture = future
        future.addListener(
            {
                if (controllerFuture !== future || future.isCancelled) return@addListener
                runCatching { future.get() }.getOrNull()?.let { connected ->
                    controller = connected
                    connected.addListener(playerListener)
                    publishState()
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun disconnect() {
        controller?.removeListener(playerListener)
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        controller = null
    }

    suspend fun play(station: Station) = withController {
        setMediaItem(station.toMediaItem())
        prepare()
        play()
    }

    suspend fun togglePlayPause() = withController {
        when {
            playbackState == Player.STATE_IDLE -> {
                // Tras un error: vuelve al borde en vivo y reintenta.
                seekToDefaultPosition()
                prepare()
                play()
            }
            playWhenReady -> pause()
            else -> play()
        }
    }

    suspend fun setVolume(volume: Float) = withController {
        this.volume = volume.coerceIn(0f, 1f)
    }

    /** Programa el Sleep Timer en el servicio; `0` lo cancela. */
    suspend fun setSleepTimer(minutes: Int) = withController {
        sendCustomCommand(
            SessionCommand(PlaybackContract.ACTION_SET_SLEEP_TIMER, Bundle.EMPTY),
            bundleOf(PlaybackContract.EXTRA_SLEEP_MINUTES to minutes),
        )
    }

    /** Espera la conexión si todavía está en curso (o la inicia) antes de ejecutar [block]. */
    private suspend fun withController(block: MediaController.() -> Unit) {
        val connected = controller ?: run {
            connect()
            try {
                controllerFuture?.await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        } ?: return
        connected.block()
    }

    private fun publishState() {
        val player = controller ?: return
        val item = player.currentMediaItem
        // currentMediaItem conserva la metadata original de la estación; mediaMetadata
        // del reproductor trae además el título ICY que emite el stream.
        val stationMetadata = item?.mediaMetadata ?: MediaMetadata.EMPTY
        val stationName = stationMetadata.title?.toString().orEmpty()
        val streamTitle = player.mediaMetadata.title?.toString()?.trim()
        val extras = player.sessionExtras

        _state.value = PlayerUiState(
            stationId = item?.mediaId?.takeIf { it.isNotEmpty() },
            stationName = stationName,
            nowPlaying = streamTitle?.takeIf { it.isNotEmpty() && !it.equals(stationName, ignoreCase = true) },
            frequency = stationMetadata.frequency,
            artworkUri = stationMetadata.artworkUri,
            isPlaying = player.isPlaying,
            isBuffering = player.playWhenReady && player.playbackState == Player.STATE_BUFFERING,
            isReconnecting = extras.getBoolean(PlaybackContract.KEY_IS_RECONNECTING, false),
            volume = player.volume,
            sleepTimerEndElapsed = extras.getLong(PlaybackContract.KEY_SLEEP_TIMER_END_ELAPSED, 0L)
                .takeIf { it > 0L },
            error = player.playerError?.toPlayerError(),
        )
    }

    private fun PlaybackException.toPlayerError(): PlayerError = when (errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_TIMEOUT,
        -> PlayerError.NETWORK
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
        -> PlayerError.STREAM_UNAVAILABLE
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        -> PlayerError.UNSUPPORTED_FORMAT
        else -> PlayerError.UNKNOWN
    }
}
