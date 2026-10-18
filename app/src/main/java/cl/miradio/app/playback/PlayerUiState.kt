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
    /** Bitrate declarado por la radio, para estimar el consumo de datos. */
    val bitrateKbps: Int? = null,
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

    /**
     * El error es de la radio y no de la conexión del usuario. Un error de red con
     * internet disponible significa que el servidor de la radio no responde.
     */
    fun isStationFault(isOnline: Boolean): Boolean = when (error) {
        null -> false
        PlayerError.STREAM_UNAVAILABLE, PlayerError.UNSUPPORTED_FORMAT -> !isReconnecting
        PlayerError.NETWORK, PlayerError.UNKNOWN -> isOnline && !isReconnecting
    }
}

enum class PlayerError { NETWORK, STREAM_UNAVAILABLE, UNSUPPORTED_FORMAT, UNKNOWN }
