package cl.miradio.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import cl.miradio.app.domain.model.Station
import cl.miradio.app.ui.theme.Motion
import kotlinx.coroutines.delay

/** Margen lateral de la grilla, para que las filas horizontales del encabezado lleguen al borde. */
val GridHorizontalPadding = 16.dp

private val GridContentPadding =
    PaddingValues(start = GridHorizontalPadding, end = GridHorizontalPadding, top = 8.dp, bottom = 16.dp)
private val GridSpacing = 12.dp
private const val PLACEHOLDER_COUNT = 8

/** Tarjetas de la primera pantalla que entran escalonadas; las demás entran sin espera. */
private const val STAGGERED_ITEMS = 8
private const val STAGGER_DELAY_MS = 45L

/** Cuadrícula de 2 columnas con las tarjetas de estación. */
@Composable
fun StationGrid(
    stations: List<Station>,
    currentStationId: String?,
    onStationClick: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    state: LazyGridState = rememberLazyGridState(),
    /** Contenido a todo el ancho sobre las tarjetas (p. ej. destacadas y recientes). */
    header: (@Composable () -> Unit)? = null,
) {
    // Tarjetas que ya hicieron su entrada, para no repetirla al volver a desplazarse.
    val appeared = remember { mutableSetOf<String>() }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = state,
        modifier = modifier.fadingTopEdge(),
        contentPadding = GridContentPadding,
        horizontalArrangement = Arrangement.spacedBy(GridSpacing),
        verticalArrangement = Arrangement.spacedBy(GridSpacing),
    ) {
        if (header != null) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
        }
        itemsIndexed(stations, key = { _, station -> station.id }) { index, station ->
            val isCurrent = station.id == currentStationId
            StationCard(
                station = station,
                isCurrent = isCurrent,
                isPlaying = isCurrent && isPlaying,
                onClick = { onStationClick(station) },
                onToggleFavorite = { onToggleFavorite(station) },
                modifier = Modifier
                    .animateItem()
                    .entrance(key = station.id, index = index, appeared = appeared),
            )
        }
    }
}

/** La tarjeta aparece subiendo y creciendo un poco, con un leve desfase según su posición. */
private fun Modifier.entrance(key: String, index: Int, appeared: MutableSet<String>): Modifier = composed {
    val progress = remember(key) { Animatable(if (key in appeared) 1f else 0f) }
    LaunchedEffect(key) {
        if (!appeared.add(key) && progress.value == 1f) return@LaunchedEffect
        if (index < STAGGERED_ITEMS) delay(index * STAGGER_DELAY_MS)
        progress.animateTo(1f, Motion.gentle)
    }
    graphicsLayer {
        val value = progress.value
        alpha = value.coerceIn(0f, 1f)
        translationY = (1f - value) * 28.dp.toPx()
        val scale = 0.92f + 0.08f * value
        scaleX = scale
        scaleY = scale
    }
}

/** Misma cuadrícula, con placeholders animados mientras llega la primera carga. */
@Composable
fun StationGridPlaceholder(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = GridContentPadding,
        horizontalArrangement = Arrangement.spacedBy(GridSpacing),
        verticalArrangement = Arrangement.spacedBy(GridSpacing),
        userScrollEnabled = false,
    ) {
        items(PLACEHOLDER_COUNT) { StationCardPlaceholder() }
    }
}
