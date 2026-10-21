package cl.miradio.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import cl.miradio.app.ui.theme.AndesBlue
import cl.miradio.app.ui.theme.ArtworkPalette
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Lado en píxeles de la miniatura que se analiza: basta para saber el color. */
private const val SAMPLE_SIZE = 32

/** ARGB 0 (transparente) marca los logos sin un color claro, para no analizarlos otra vez. */
private const val NO_COLOR = 0

private val colorCache = LruCache<String, Int>(64)

/**
 * Color principal del logo en [url], ajustado al tema oscuro y animado al cambiar
 * de radio. Mientras no se conoce, o si el logo es gris, devuelve [fallback].
 * Usa el mismo [coil.ImageLoader] de la app, así que respeta el ahorro de datos.
 */
@Composable
fun rememberArtworkColor(url: String?, fallback: Color = AndesBlue): State<Color> {
    val context = LocalContext.current
    var extracted by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(url) {
        extracted = if (url.isNullOrBlank()) null else artworkColor(context, url)
    }
    return animateColorAsState(extracted ?: fallback, animationSpec = tween(durationMillis = 600), label = "artworkColor")
}

private suspend fun artworkColor(context: Context, url: String): Color? {
    val argb = colorCache.get(url) ?: extractColor(context, url)?.also { colorCache.put(url, it) } ?: return null
    return if (argb == NO_COLOR) null else Color(argb)
}

/** Descarga (o lee de la caché) una miniatura del logo y calcula su color. `null` si no cargó. */
private suspend fun extractColor(context: Context, url: String): Int? {
    val request = ImageRequest.Builder(context)
        .data(url)
        .size(SAMPLE_SIZE)
        // Los bitmaps de hardware no permiten leer sus píxeles.
        .allowHardware(false)
        // El color ya queda guardado aparte; así esta miniatura no desplaza a los logos de la grilla.
        .memoryCachePolicy(CachePolicy.DISABLED)
        .build()
    val drawable = (context.imageLoader.execute(request) as? SuccessResult)?.drawable ?: return null
    return withContext(Dispatchers.Default) {
        val bitmap = drawable.toBitmap(SAMPLE_SIZE, SAMPLE_SIZE, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        ArtworkPalette.dominantColor(pixels)?.let(ArtworkPalette::forDarkTheme) ?: NO_COLOR
    }
}
