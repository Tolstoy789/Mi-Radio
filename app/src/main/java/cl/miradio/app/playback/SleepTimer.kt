package cl.miradio.app.playback

import android.os.SystemClock
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pausa la radio después de N minutos, bajando el volumen gradualmente en los
 * últimos segundos para no cortar de golpe. Vive en el servicio, así que sigue
 * corriendo aunque se cierre la UI. [scope] debe correr en el hilo principal.
 */
class SleepTimer(
    private val player: Player,
    private val scope: CoroutineScope,
    private val onChanged: (endElapsedRealtime: Long?) -> Unit,
) {
    private var job: Job? = null
    private var volumeBeforeFade: Float? = null

    /** Instante de término en `SystemClock.elapsedRealtime()`, o `null` si no hay timer. */
    var endElapsedRealtime: Long? = null
        private set

    fun start(minutes: Int) {
        require(minutes > 0) { "La duración debe ser positiva" }
        cancel()
        val durationMs = minutes * 60_000L
        val end = SystemClock.elapsedRealtime() + durationMs
        endElapsedRealtime = end
        onChanged(end)
        job = scope.launch {
            delay((durationMs - FADE_DURATION_MS).coerceAtLeast(0))
            fadeOutAndPause()
            job = null
            clear()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        volumeBeforeFade?.let { player.volume = it }
        volumeBeforeFade = null
        clear()
    }

    private suspend fun fadeOutAndPause() {
        val startVolume = player.volume
        volumeBeforeFade = startVolume
        for (step in 1..FADE_STEPS) {
            player.volume = startVolume * (1f - step.toFloat() / FADE_STEPS)
            delay(FADE_DURATION_MS / FADE_STEPS)
        }
        player.pause()
        player.volume = startVolume
        volumeBeforeFade = null
    }

    private fun clear() {
        if (endElapsedRealtime != null) {
            endElapsedRealtime = null
            onChanged(null)
        }
    }

    private companion object {
        const val FADE_DURATION_MS = 10_000L
        const val FADE_STEPS = 20
    }
}
