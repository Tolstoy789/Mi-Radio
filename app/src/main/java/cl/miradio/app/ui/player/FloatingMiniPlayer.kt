package cl.miradio.app.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.miradio.app.playback.PlayerUiState
import cl.miradio.app.ui.components.LiveBadge
import cl.miradio.app.ui.components.PlayPauseButton
import cl.miradio.app.ui.components.StationLogo
import cl.miradio.app.ui.theme.CardBorder
import cl.miradio.app.ui.theme.MiniPlayerShape
import cl.miradio.app.ui.theme.MiniPlayerSurface
import cl.miradio.app.ui.theme.TextPrimary
import cl.miradio.app.ui.theme.TextSecondary

/** Reproductor flotante sobre la barra de navegación. Tocarlo abre Now Playing. */
@Composable
fun FloatingMiniPlayer(
    state: PlayerUiState,
    onClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = MiniPlayerShape,
        color = MiniPlayerSurface,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StationLogo(
                url = state.artworkUri?.toString(),
                contentDescription = state.stationName,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(48.dp),
                fallbackIconSize = 24.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = state.stationName,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    LiveBadge(pulsing = state.isPlaying)
                    playerStatusText(state)?.let { status ->
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            PlayPauseButton(
                isPlaying = state.isPlaying,
                isLoading = state.showLoading,
                onClick = onTogglePlayPause,
            )
        }
    }
}
