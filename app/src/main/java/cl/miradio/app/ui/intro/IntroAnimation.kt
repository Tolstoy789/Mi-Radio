package cl.miradio.app.ui.intro

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import cl.miradio.app.R
import cl.miradio.app.ui.intro.CordilleraMark.drawBars
import cl.miradio.app.ui.intro.CordilleraMark.drawStar
import cl.miradio.app.ui.intro.CordilleraMark.ridgePath
import cl.miradio.app.ui.theme.Accent
import cl.miradio.app.ui.theme.Background
import cl.miradio.app.ui.theme.TextPrimary
import cl.miradio.app.ui.theme.TextSecondary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Mismo tamaño que el ícono del splash del sistema (lienzo de 288dp en API 31+). */
private val MarkSize = 288.dp

/**
 * Animación de inicio "Cordillera", continuación del splash del sistema:
 * las barras laten como un ecualizador, se dibuja el perfil de los cerros en rojo,
 * la estrella gira hasta su lugar y aparece el nombre. Luego se desvanece sobre la app.
 * Si el usuario desactivó las animaciones del sistema, termina de inmediato.
 */
@Composable
fun IntroAnimation(onFinished: () -> Unit) {
    val context = LocalContext.current
    val currentOnFinished by rememberUpdatedState(onFinished)

    val barScales = remember { CordilleraMark.bars.map { Animatable(1f) } }
    val ridgeProgress = remember { Animatable(0f) }
    val starScale = remember { Animatable(1f) }
    val starRotation = remember { Animatable(0f) }
    val wordmark = remember { Animatable(0f) }
    val overlayAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        val animationsOff = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
        if (animationsOff) {
            currentOnFinished()
            return@LaunchedEffect
        }

        // 1. Ecualizador: cada barra late con un pequeño desfase.
        barScales.mapIndexed { index, bar ->
            async {
                delay(index * 60L)
                bar.animateTo(
                    targetValue = 1f,
                    animationSpec = keyframes {
                        durationMillis = 720
                        1f at 0
                        0.55f at 140
                        1.18f at 300
                        0.82f at 460
                        1.05f at 600
                        1f at 720
                    },
                )
            }
        }.awaitAll()

        // 2. Perfil de los cerros + la estrella gira hasta su lugar.
        launch {
            starScale.snapTo(0.6f)
            starRotation.snapTo(-120f)
            launch { starRotation.animateTo(0f, tween(520, easing = FastOutSlowInEasing)) }
            starScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        ridgeProgress.animateTo(1f, tween(560, easing = FastOutSlowInEasing))

        // 3. Nombre de la app.
        wordmark.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        delay(520)

        // 4. Se desvanece sobre la pantalla de inicio.
        overlayAlpha.animateTo(0f, tween(320))
        currentOnFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha.value }
            .background(Background)
            // Bloquea los toques hacia la app mientras la animación la cubre.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            }
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(MarkSize)) {
            drawBars(barScales.map { it.value })
            if (ridgeProgress.value > 0f) {
                val full = ridgePath()
                val measure = PathMeasure().apply { setPath(full, false) }
                val segment = Path()
                measure.getSegment(0f, measure.length * ridgeProgress.value, segment, true)
                drawPath(
                    path = segment,
                    color = Accent,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
            drawStar(TextPrimary, starScale.value, starRotation.value)
        }
        Column(
            modifier = Modifier
                .padding(top = MarkSize * 0.8f)
                .graphicsLayer {
                    alpha = wordmark.value
                    translationY = (1f - wordmark.value) * 16.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.intro_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}
