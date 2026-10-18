package cl.miradio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.miradio.app.R
import cl.miradio.app.ui.theme.CoralAccent
import cl.miradio.app.ui.theme.TextPrimary

/** Botón circular Play/Pause; mientras carga muestra un [CircularProgressIndicator]. */
@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    IconButton(onClick = onClick, modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .background(CoralAccent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = TextPrimary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(size * 0.5f),
                )
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(if (isPlaying) R.string.action_pause else R.string.action_play),
                    tint = TextPrimary,
                    modifier = Modifier.size(size * 0.6f),
                )
            }
        }
    }
}
