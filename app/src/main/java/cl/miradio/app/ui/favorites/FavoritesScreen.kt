package cl.miradio.app.ui.favorites

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.miradio.app.R
import cl.miradio.app.domain.model.Station
import cl.miradio.app.ui.components.StateMessage
import cl.miradio.app.ui.components.StationGrid
import cl.miradio.app.ui.components.StationGridPlaceholder
import cl.miradio.app.ui.theme.TextPrimary
import cl.miradio.app.ui.theme.TextSecondary

/** Qué muestra la pantalla; cambiar de uno a otro se hace con un fundido. */
private enum class FavoritesContent { LOADING, EMPTY, LIST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesRoute(
    currentStationId: String?,
    isPlaying: Boolean,
    onPlay: (Station) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val list = favorites
    val content = when {
        list == null -> FavoritesContent.LOADING
        list.isEmpty() -> FavoritesContent.EMPTY
        else -> FavoritesContent.LIST
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    if (!list.isNullOrEmpty()) {
                        Text(
                            text = pluralStringResource(R.plurals.favorites_count, list.size, list.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                    Text(stringResource(R.string.favorites_title), color = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
        AnimatedContent(
            targetState = content,
            transitionSpec = { fadeIn(tween(durationMillis = 250)) togetherWith fadeOut(tween(durationMillis = 150)) },
            label = "favoritesContent",
        ) { target ->
            when (target) {
                FavoritesContent.LOADING -> StationGridPlaceholder(Modifier.fillMaxSize())
                FavoritesContent.EMPTY -> StateMessage(
                    icon = Icons.Rounded.FavoriteBorder,
                    title = stringResource(R.string.favorites_empty_title),
                    body = stringResource(R.string.favorites_empty_body),
                )
                FavoritesContent.LIST -> StationGrid(
                    stations = list.orEmpty(),
                    currentStationId = currentStationId,
                    isPlaying = isPlaying,
                    onStationClick = onPlay,
                    onToggleFavorite = { viewModel.onToggleFavorite(it.id) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
