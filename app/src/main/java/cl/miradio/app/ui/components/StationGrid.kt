package cl.miradio.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.miradio.app.domain.model.Station

private val GridContentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
private val GridSpacing = 12.dp
private const val PLACEHOLDER_COUNT = 8

/** Cuadrícula de 2 columnas con las tarjetas de estación. */
@Composable
fun StationGrid(
    stations: List<Station>,
    currentStationId: String?,
    onStationClick: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = state,
        modifier = modifier,
        contentPadding = GridContentPadding,
        horizontalArrangement = Arrangement.spacedBy(GridSpacing),
        verticalArrangement = Arrangement.spacedBy(GridSpacing),
    ) {
        items(stations, key = { it.id }) { station ->
            StationCard(
                station = station,
                isCurrent = station.id == currentStationId,
                onClick = { onStationClick(station) },
                onToggleFavorite = { onToggleFavorite(station) },
                modifier = Modifier.animateItem(),
            )
        }
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
