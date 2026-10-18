package cl.miradio.app.ui.intro

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Geometría del ícono "Cordillera" (estrella + ecualizador con forma de cerros),
 * en las mismas coordenadas que `ic_launcher_foreground.xml`: un lienzo de 108
 * unidades cuyo contenido ocupa el centro de 72. Así la animación arranca
 * exactamente donde la deja el splash del sistema.
 */
internal object CordilleraMark {

    data class Bar(val x: Float, val top: Float, val height: Float, val color: Color)

    val bars = listOf(
        Bar(23.5f, 64f, 22f, Color(0xFF8FA2FF)),
        Bar(36.5f, 50f, 36f, Color(0xFFC5D0FF)),
        Bar(49.5f, 38f, 48f, Color(0xFFFFFFFF)),
        Bar(62.5f, 54f, 32f, Color(0xFFC5D0FF)),
        Bar(75.5f, 68f, 18f, Color(0xFF8FA2FF)),
    )

    private const val BAR_WIDTH = 9f
    private const val BASELINE = 86f
    private val starCenter = Offset(30f, 29.5f)
    private val starPoints = listOf(
        30f to 22f, 31.94f to 27.33f, 37.61f to 27.53f, 33.14f to 31.02f, 34.7f to 36.47f,
        30f to 33.3f, 25.3f to 36.47f, 26.86f to 31.02f, 22.39f to 27.53f, 28.06f to 27.33f,
    )

    /** Convierte una coordenada del diseño (0..108) al lienzo adaptativo, en píxeles. */
    private fun DrawScope.map(x: Float, y: Float): Offset {
        val unit = size.minDimension / 108f
        return Offset((18f + x * 2f / 3f) * unit, (18f + y * 2f / 3f) * unit)
    }

    private fun DrawScope.scale(length: Float): Float = length * 2f / 3f * size.minDimension / 108f

    /** Barras con altura escalada desde la base según [scales] (1 = tamaño del ícono). */
    fun DrawScope.drawBars(scales: List<Float>) {
        bars.forEachIndexed { index, bar ->
            val height = bar.height * scales.getOrElse(index) { 1f }
            val topLeft = map(bar.x, BASELINE - height)
            drawRoundRect(
                color = bar.color,
                topLeft = topLeft,
                size = Size(scale(BAR_WIDTH), scale(height)),
                cornerRadius = CornerRadius(scale(BAR_WIDTH / 2f)),
            )
        }
    }

    fun DrawScope.drawStar(color: Color, starScale: Float, rotationDegrees: Float) {
        if (starScale <= 0f) return
        val path = Path().apply {
            starPoints.forEachIndexed { i, (x, y) ->
                val p = map(x, y)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        val pivot = map(starCenter.x, starCenter.y)
        withTransform({
            rotate(rotationDegrees, pivot)
            scale(starScale, starScale, pivot)
        }) {
            drawPath(path, color)
        }
    }

    /** Perfil de los cerros: une las puntas de las barras y baja a la base en los extremos. */
    fun DrawScope.ridgePath(): Path = Path().apply {
        val start = map(14f, BASELINE)
        moveTo(start.x, start.y)
        bars.forEach { bar ->
            val p = map(bar.x + BAR_WIDTH / 2f, bar.top)
            lineTo(p.x, p.y)
        }
        val end = map(94f, BASELINE)
        lineTo(end.x, end.y)
    }
}
