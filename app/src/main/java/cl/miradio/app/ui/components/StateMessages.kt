package cl.miradio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.miradio.app.ui.theme.Accent
import cl.miradio.app.ui.theme.PillShape
import cl.miradio.app.ui.theme.TextPrimary
import cl.miradio.app.ui.theme.TextSecondary

private const val WAVE_COUNT = 3

/**
 * Mensaje centrado para listas vacías o errores, con acción opcional. El ícono
 * emite ondas como una antena y el bloque entra subiendo suavemente.
 */
@Composable
fun StateMessage(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(tween(durationMillis = 400)) + slideInVertically(tween(durationMillis = 400)) { it / 10 },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RadioWavesIcon(icon)
            Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, textAlign = TextAlign.Center)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

/** Ícono dentro de un círculo con ondas que se expanden y desvanecen. */
@Composable
private fun RadioWavesIcon(icon: ImageVector) {
    val transition = rememberInfiniteTransition(label = "radioWaves")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2_700, easing = LinearEasing)),
        label = "radioWavesProgress",
    )
    Box(
        modifier = Modifier
            .size(150.dp)
            .drawBehind {
                val inner = 38.dp.toPx()
                val outer = size.minDimension / 2f
                repeat(WAVE_COUNT) { wave ->
                    val p = (progress + wave.toFloat() / WAVE_COUNT) % 1f
                    drawCircle(
                        color = Accent.copy(alpha = 0.4f * (1f - p)),
                        radius = inner + (outer - inner) * p,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(Accent.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(38.dp))
        }
    }
}
