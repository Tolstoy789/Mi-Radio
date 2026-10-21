package cl.miradio.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.miradio.app.R
import cl.miradio.app.ui.theme.Accent
import cl.miradio.app.ui.theme.LiveRed

/** Alto de cada barra respecto de la más alta, como en el ícono Cordillera. */
private val BarHeights = listOf(0.55f, 1f, 0.67f)

/**
 * Badge "EN VIVO" con un mini ecualizador (el motivo del ícono Cordillera).
 * Las barras se mueven mientras [pulsing] es `true`, es decir, mientras suena la radio.
 */
@Composable
fun LiveBadge(pulsing: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Accent.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        EqualizerBars(animating = pulsing, color = LiveRed, modifier = Modifier.size(9.dp))
        Text(
            text = stringResource(R.string.live_badge),
            style = MaterialTheme.typography.labelSmall,
            color = LiveRed,
        )
    }
}

/** Tres barras de ecualizador que laten mientras [animating] es `true`. */
@Composable
fun EqualizerBars(animating: Boolean, color: Color, modifier: Modifier = Modifier) {
    // Solo hay animación (y redibujo por cuadro) mientras suena.
    val phases = if (animating) {
        val transition = rememberInfiniteTransition(label = "equalizer")
        BarHeights.indices.map { index ->
            transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 380 + index * 140),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "equalizerBar$index",
            )
        }
    } else {
        null
    }
    Canvas(modifier) {
        val gap = size.width * 0.18f
        val barWidth = (size.width - gap * (BarHeights.size - 1)) / BarHeights.size
        BarHeights.forEachIndexed { index, maxHeight ->
            val fraction = phases?.get(index)?.value ?: 1f
            val height = size.height * maxHeight * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (barWidth + gap), size.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}
