package cl.miradio.app.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo
import kotlin.math.min

/**
 * Primera línea de defensa ante cortes de red: reintenta la carga del stream
 * dentro de ExoPlayer con backoff exponencial (1s, 2s, 4s, 8s, 8s...), sin que
 * el usuario vea un error. Los errores que no son de red (404, formato no
 * soportado, HTTP bloqueado) se descartan igual que en la política por defecto.
 *
 * Si se agotan los reintentos, el error llega al reproductor y lo toma
 * [StreamReconnectionHandler].
 */
@OptIn(UnstableApi::class)
class LiveStreamLoadErrorPolicy : DefaultLoadErrorHandlingPolicy(MIN_RETRY_COUNT) {

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorInfo): Long {
        if (super.getRetryDelayMsFor(loadErrorInfo) == C.TIME_UNSET) return C.TIME_UNSET
        val attempt = (loadErrorInfo.errorCount - 1).coerceIn(0, MAX_BACKOFF_EXPONENT)
        return min(BASE_DELAY_MS shl attempt, MAX_DELAY_MS)
    }

    private companion object {
        const val MIN_RETRY_COUNT = 6
        const val BASE_DELAY_MS = 1_000L
        const val MAX_DELAY_MS = 8_000L
        const val MAX_BACKOFF_EXPONENT = 3
    }
}
