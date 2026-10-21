package cl.miradio.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import cl.miradio.app.ui.theme.Motion

/** Encoge un poco el elemento mientras se mantiene presionado, como un botón físico. */
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.96f): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, Motion.press, label = "pressScale")
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Deja que una fila horizontal ocupe también el margen lateral de su contenedor,
 * para que al desplazarse llegue hasta el borde de la pantalla. La fila debe
 * devolver ese margen como `contentPadding`.
 */
fun Modifier.bleedHorizontally(bleed: Dp): Modifier = layout { measurable, constraints ->
    val extra = bleed.roundToPx() * 2
    val placeable = measurable.measure(constraints.offset(horizontal = extra))
    layout(placeable.width - extra, placeable.height) {
        placeable.place(-extra / 2, 0)
    }
}

/** Desvanece el borde superior del contenido que se desplaza bajo los filtros. */
fun Modifier.fadingTopEdge(height: Dp = 12.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = height.toPx()),
            blendMode = BlendMode.DstIn,
        )
    }
