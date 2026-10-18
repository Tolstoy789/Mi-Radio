package cl.miradio.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.miradio.app.R
import cl.miradio.app.ui.theme.Accent
import cl.miradio.app.ui.theme.LiveRed

private val BarHeights = listOf(5, 9, 6)

/**
 * Badge "EN VIVO" con un mini ecualizador (el motivo del ícono Cordillera).
 * Las barras se mueven mientras [pulsing] es `true`, es decir, mientras suena la radio.
 */
@Composable
fun LiveBadge(pulsing: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "live")
    val phases = BarHeights.indices.map { index ->
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 380 + index * 140),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "liveBar$index",
        )
    }
    Row(
        modifier = modifier
            .background(Accent.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.height(9.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        ) {
            BarHeights.forEachIndexed { index, maxHeight ->
                val phase by phases[index]
                val fraction = if (pulsing) phase else 1f
                Box(
                    Modifier
                        .width(2.dp)
                        .height((maxHeight * fraction).dp)
                        .background(LiveRed, RoundedCornerShape(1.dp)),
                )
            }
        }
        Text(
            text = stringResource(R.string.live_badge),
            style = MaterialTheme.typography.labelSmall,
            color = LiveRed,
        )
    }
}
