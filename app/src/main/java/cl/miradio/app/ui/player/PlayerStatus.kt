package cl.miradio.app.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cl.miradio.app.R
import cl.miradio.app.playback.PlayerError
import cl.miradio.app.playback.PlayerUiState

/** Línea secundaria del reproductor: error, reconexión o lo que suena ahora. */
@Composable
fun playerStatusText(state: PlayerUiState): String? = when {
    state.isReconnecting -> stringResource(R.string.player_reconnecting)
    state.error != null -> stringResource(state.error.messageRes)
    state.nowPlaying != null -> state.nowPlaying
    else -> state.frequency
}

val PlayerError.messageRes: Int
    get() = when (this) {
        PlayerError.NETWORK -> R.string.player_error_network
        PlayerError.STREAM_UNAVAILABLE -> R.string.player_error_unavailable
        PlayerError.UNSUPPORTED_FORMAT -> R.string.player_error_format
        PlayerError.UNKNOWN -> R.string.player_error_unknown
    }
