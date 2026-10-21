package cl.miradio.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.miradio.app.R
import cl.miradio.app.ui.theme.Accent
import cl.miradio.app.ui.theme.AccentDeep
import cl.miradio.app.ui.theme.TextPrimary

private enum class PlayButtonContent { PLAY, PAUSE, LOADING }

/**
 * Botón circular Play/Pause con degradado. El ícono se transforma al cambiar de
 * estado, el botón se hunde al presionarlo y, mientras carga, muestra un spinner.
 * Con [glowColor] dibuja un halo que late detrás mientras suena (Now Playing).
 */
@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    glowColor: Color? = null,
) {
    val haptics = rememberHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val content = when {
        isLoading -> PlayButtonContent.LOADING
        isPlaying -> PlayButtonContent.PAUSE
        else -> PlayButtonContent.PLAY
    }
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .then(if (glowColor != null) Modifier.playingGlow(isPlaying, glowColor) else Modifier)
            .pressScale(interactionSource, pressedScale = 0.88f)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Accent, AccentDeep)))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = TextPrimary),
                onClickLabel = null,
                role = Role.Button,
                onClick = {
                    haptics.tap()
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = content,
            transitionSpec = {
                (scaleIn(initialScale = 0.5f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.5f) + fadeOut())
            },
            label = "playPauseIcon",
        ) { target ->
            when (target) {
                PlayButtonContent.LOADING -> CircularProgressIndicator(
                    color = TextPrimary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(size * 0.5f),
                )
                PlayButtonContent.PAUSE, PlayButtonContent.PLAY -> Icon(
                    imageVector = if (target == PlayButtonContent.PAUSE) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(
                        if (target == PlayButtonContent.PAUSE) R.string.action_pause else R.string.action_play,
                    ),
                    tint = TextPrimary,
                    modifier = Modifier.size(size * 0.6f),
                )
            }
        }
    }
}

/** Halo de [color] que late detrás del botón mientras suena y se apaga en pausa. */
private fun Modifier.playingGlow(isPlaying: Boolean, color: Color): Modifier = composed {
    val strength by animateFloatAsState(if (isPlaying) 1f else 0f, tween(durationMillis = 500), label = "glowStrength")
    // El latido solo corre mientras suena, para no redibujar en pausa.
    val pulse = if (isPlaying) {
        rememberInfiniteTransition(label = "glow").animateFloat(
            initialValue = 0.9f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(1_400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "glowPulse",
        )
    } else {
        null
    }
    drawBehind {
        if (strength <= 0f) return@drawBehind
        val radius = size.minDimension * (pulse?.value ?: 1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.55f * strength), Color.Transparent),
                center = center,
                radius = radius,
            ),
            radius = radius,
        )
    }
}
