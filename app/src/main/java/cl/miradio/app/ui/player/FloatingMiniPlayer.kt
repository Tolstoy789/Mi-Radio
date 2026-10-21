package cl.miradio.app.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.miradio.app.playback.PlayerUiState
import cl.miradio.app.ui.components.LiveBadge
import cl.miradio.app.ui.components.PlayPauseButton
import cl.miradio.app.ui.components.StationLogo
import cl.miradio.app.ui.components.pressScale
import cl.miradio.app.ui.components.rememberArtworkColor
import cl.miradio.app.ui.theme.GlassBorder
import cl.miradio.app.ui.theme.MiniPlayerShape
import cl.miradio.app.ui.theme.MiniPlayerSurface
import cl.miradio.app.ui.theme.Motion
import cl.miradio.app.ui.theme.TextPrimary
import cl.miradio.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/** Velocidad hacia arriba (px/s) que basta para abrir Now Playing aunque el gesto sea corto. */
private const val OPEN_FLING_VELOCITY = 1_200f

/**
 * Reproductor flotante sobre la barra de navegación, teñido con el color del logo.
 * Tocarlo o deslizarlo hacia arriba abre Now Playing.
 */
@Composable
fun FloatingMiniPlayer(
    state: PlayerUiState,
    onClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artColor by rememberArtworkColor(state.artworkUri?.toString(), fallback = MiniPlayerSurface)
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    val openDistance = with(LocalDensity.current) { 56.dp.toPx() }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .graphicsLayer { translationY = dragOffset.value }
            .pressScale(interactionSource, pressedScale = 0.98f)
            .draggable(
                state = rememberDraggableState { delta ->
                    scope.launch {
                        // Sigue al dedo hacia arriba, con resistencia; hacia abajo no se mueve.
                        dragOffset.snapTo((dragOffset.value + delta * 0.5f).coerceIn(-openDistance, 0f))
                    }
                },
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (dragOffset.value <= -openDistance * 0.5f || velocity < -OPEN_FLING_VELOCITY) onClick()
                    dragOffset.animateTo(0f, Motion.bouncy)
                },
            ),
        shape = MiniPlayerShape,
        color = MiniPlayerSurface,
        border = BorderStroke(1.dp, GlassBorder),
        shadowElevation = 12.dp,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(artColor.copy(alpha = 0.5f), Color.Transparent)))
                .padding(8.dp),
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
