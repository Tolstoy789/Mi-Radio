package cl.miradio.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import cl.miradio.app.ui.theme.CardBorder
import cl.miradio.app.ui.theme.CardSurface

/**
 * Pinta un brillo diagonal que recorre el elemento, para placeholders mientras
 * la lista carga. Aplícalo junto a un `clip` para respetar las esquinas.
 */
fun Modifier.shimmerEffect(
    baseColor: Color = CardSurface,
    highlightColor: Color = CardBorder,
    durationMillis: Int = 1_200,
): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    val width = size.width.toFloat().coerceAtLeast(1f)
    val height = size.height.toFloat().coerceAtLeast(1f)
    val startX = progress * width

    background(
        brush = Brush.linearGradient(
            colors = listOf(baseColor, highlightColor, baseColor),
            start = Offset(startX, 0f),
            end = Offset(startX + width / 2f, height),
        ),
    ).onGloballyPositioned { size = it.size }
}
