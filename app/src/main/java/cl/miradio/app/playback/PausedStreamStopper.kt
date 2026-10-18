package cl.miradio.app.playback

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Un stream en vivo pausado sigue con la conexión abierta y llenando el buffer.
 * Tras un rato en pausa se detiene la carga ([Player.stop]); al volver a Play, el
 * reproductor se prepara de nuevo en el borde en vivo. Con ahorro de datos activo
 * el corte llega antes.
 */
class PausedStreamStopper(
    private val player: Player,
    private val scope: CoroutineScope,
    private val isDataSaverActive: () -> Boolean,
) : Player.Listener {

    private var stopJob: Job? = null

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (playWhenReady) cancel() else schedule()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_IDLE) cancel()
    }

    fun cancel() {
        stopJob?.cancel()
        stopJob = null
    }

    private fun schedule() {
        cancel()
        if (player.playbackState == Player.STATE_IDLE) return
        val delayMs = if (isDataSaverActive()) SAVER_DELAY_MS else DEFAULT_DELAY_MS
        stopJob = scope.launch {
            delay(delayMs)
            if (!player.playWhenReady && player.playbackState != Player.STATE_IDLE) player.stop()
        }
    }

    private companion object {
        const val DEFAULT_DELAY_MS = 2 * 60_000L
        const val SAVER_DELAY_MS = 15_000L
    }
}
