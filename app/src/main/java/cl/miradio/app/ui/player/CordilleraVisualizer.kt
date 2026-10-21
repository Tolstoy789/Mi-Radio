package cl.miradio.app.ui.player

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cl.miradio.app.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private const val BAR_COUNT = 34

/** Duración de un ciclo completo de la onda, en nanosegundos. */
private const val CYCLE_NANOS = 2_400_000_000L

/** Altura mínima de las barras, como fracción del alto: la "base" de los cerros. */
private const val FLOOR = 0.1f

/**
 * Ecualizador decorativo con silueta de cordillera: las barras suben y bajan
 * mientras suena la radio y se aquietan en pausa. No analiza el audio real.
 * Si el usuario desactivó las animaciones del sistema, queda quieto.
 */
@Composable
fun CordilleraVisualizer(isPlaying: Boolean, color: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val animationsEnabled = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val envelope = remember { FloatArray(BAR_COUNT) { ridge(it / (BAR_COUNT - 1f)) } }
    var phase by remember { mutableFloatStateOf(0.25f) }
    val energy by animateFloatAsState(if (isPlaying) 1f else 0f, tween(durationMillis = 700), label = "visualizerEnergy")

    // La onda avanza cuadro a cuadro solo mientras suena; en pausa no se redibuja.
    LaunchedEffect(isPlaying, animationsEnabled) {
        if (!isPlaying || !animationsEnabled) return@LaunchedEffect
        val startPhase = phase
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                phase = (startPhase + (now - start).toFloat() / CYCLE_NANOS) % 1f
            }
        }
    }

    Canvas(modifier) {
        val gap = 3.dp.toPx()
        val barWidth = (size.width - gap * (BAR_COUNT - 1)) / BAR_COUNT
        val brush = Brush.verticalGradient(listOf(TextPrimary, color), startY = 0f, endY = size.height)
        val angle = phase * 2f * PI.toFloat()
        for (i in 0 until BAR_COUNT) {
            val wave = 0.55f + 0.3f * sin(angle * 2f + i * 0.6f) + 0.15f * sin(angle * 3f - i * 1.3f)
            val level = FLOOR + (1f - FLOOR) * envelope[i] * (0.25f + 0.75f * energy * wave)
            val height = (size.height * level).coerceAtLeast(barWidth)
            drawRoundRect(
                brush = brush,
                topLeft = Offset(i * (barWidth + gap), size.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f),
                alpha = 0.5f + 0.5f * energy,
            )
        }
    }
}

/** Silueta de los cerros: un pico principal y uno más bajo hacia la derecha. */
private fun ridge(position: Float): Float {
    val main = exp(-((position - 0.36f) * (position - 0.36f)) / 0.03f)
    val second = 0.72f * exp(-((position - 0.74f) * (position - 0.74f)) / 0.02f)
    return 0.3f + 0.7f * maxOf(main, second)
}
