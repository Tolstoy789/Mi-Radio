package cl.miradio.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.miradio.app.R
import cl.miradio.app.ui.theme.Accent
import cl.miradio.app.ui.theme.Motion
import cl.miradio.app.ui.theme.TextPrimary
import kotlinx.coroutines.launch

/**
 * Corazón de favoritas. Al marcarlo "salta" con un anillo que se expande y una
 * vibración de confirmación; al quitarlo se encoge un instante.
 */
@Composable
fun FavoriteToggle(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    inactiveTint: Color = TextPrimary,
) {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val ring = remember { Animatable(1f) }
    val tint by animateColorAsState(if (isFavorite) Accent else inactiveTint, label = "favoriteTint")

    IconButton(
        onClick = {
            val adding = !isFavorite
            if (adding) haptics.confirm() else haptics.tap()
            onToggle()
            scope.launch {
                scale.animateTo(if (adding) 1.3f else 0.8f, tween(durationMillis = 110))
                scale.animateTo(1f, Motion.bouncy)
            }
            if (adding) {
                scope.launch {
                    ring.snapTo(0f)
                    ring.animateTo(1f, tween(durationMillis = 450, easing = FastOutSlowInEasing))
                }
            }
        },
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = stringResource(
                if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite,
            ),
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .drawBehind {
                    val progress = ring.value
                    if (progress < 1f) {
                        drawCircle(
                            color = Accent.copy(alpha = 0.6f * (1f - progress)),
                            radius = size.minDimension * (0.35f + 0.45f * progress),
                            style = Stroke(width = 2.dp.toPx() * (1f - progress)),
                        )
                    }
                }
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
    }
}
