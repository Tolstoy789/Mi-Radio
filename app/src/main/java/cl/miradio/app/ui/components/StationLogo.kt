package cl.miradio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.miradio.app.ui.theme.CardBorder
import cl.miradio.app.ui.theme.CoralAccent
import cl.miradio.app.ui.theme.MiniPlayerSurface
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Logo de una estación con Coil. Mientras carga muestra shimmer y, si no hay URL
 * o la imagen falla (muchos favicons de Radio Browser están rotos), un ícono de radio.
 */
@Composable
fun StationLogo(
    url: String?,
    contentDescription: String?,
    shape: Shape,
    modifier: Modifier = Modifier,
    fallbackIconSize: Dp = 40.dp,
) {
    val context = LocalContext.current
    Box(modifier = modifier.clip(shape).background(MiniPlayerSurface)) {
        if (url.isNullOrBlank()) {
            LogoFallback(fallbackIconSize)
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                loading = { Box(Modifier.fillMaxSize().shimmerEffect()) },
                error = { LogoFallback(fallbackIconSize) },
            )
        }
    }
}

@Composable
private fun LogoFallback(iconSize: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(MiniPlayerSurface, CardBorder))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Radio,
            contentDescription = null,
            tint = CoralAccent,
            modifier = Modifier.size(iconSize),
        )
    }
}
