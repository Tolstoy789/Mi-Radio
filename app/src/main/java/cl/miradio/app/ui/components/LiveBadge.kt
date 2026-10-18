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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.miradio.app.R
import cl.miradio.app.ui.theme.LiveRed

/** Badge "EN VIVO"; el punto late mientras [pulsing] es `true` (radio sonando). */
@Composable
fun LiveBadge(pulsing: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "live")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "livePulse",
    )
    Row(
        modifier = modifier
            .background(LiveRed.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .alpha(if (pulsing) pulse else 1f)
                .background(LiveRed, CircleShape),
        )
        Text(
            text = stringResource(R.string.live_badge),
            style = MaterialTheme.typography.labelSmall,
            color = LiveRed,
        )
    }
}
