package cl.miradio.app.ui.favorites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.miradio.app.R
import cl.miradio.app.domain.model.Station
import cl.miradio.app.ui.components.StateMessage
import cl.miradio.app.ui.components.StationGrid
import cl.miradio.app.ui.components.StationGridPlaceholder
import cl.miradio.app.ui.theme.Background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesRoute(
    currentStationId: String?,
    onPlay: (Station) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.favorites_title)) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
        )
        val list = favorites
        when {
            list == null -> StationGridPlaceholder(Modifier.fillMaxSize())
            list.isEmpty() -> StateMessage(
                icon = Icons.Rounded.FavoriteBorder,
                title = stringResource(R.string.favorites_empty_title),
                body = stringResource(R.string.favorites_empty_body),
            )
            else -> StationGrid(
                stations = list,
                currentStationId = currentStationId,
                onStationClick = onPlay,
                onToggleFavorite = { viewModel.onToggleFavorite(it.id) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
