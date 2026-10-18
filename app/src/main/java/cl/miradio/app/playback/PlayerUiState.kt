package cl.miradio.app.playback

import android.net.Uri

/** Estado del reproductor tal como lo necesita la UI. */
data class PlayerUiState(
    val stationId: String? = null,
    val stationName: String = "",
    /** Canción o programa actual según los metadatos del stream, si los emite. */
    val nowPlaying: NowPlaying? = null,
    val frequency: String? = null,
    val artworkUri: Uri? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isReconnecting: Boolean = false,
    val volume: Float = 1f,
    /** Término del Sleep Timer en `SystemClock.elapsedRealtime()`, o `null` si no hay. */
    val sleepTimerEndElapsed: Long? = null,
    val error: PlayerError? = null,
) {
    val hasStation: Boolean get() = stationId != null

    /** La UI muestra un spinner en vez de Play/Pause. */
    val showLoading: Boolean get() = isBuffering || isReconnecting
}

enum class PlayerError { NETWORK, STREAM_UNAVAILABLE, UNSUPPORTED_FORMAT, UNKNOWN }
