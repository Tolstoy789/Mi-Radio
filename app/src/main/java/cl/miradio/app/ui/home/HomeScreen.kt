package cl.miradio.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.miradio.app.R
import cl.miradio.app.data.repository.RefreshResult
import cl.miradio.app.domain.model.ChileRegion
import cl.miradio.app.domain.model.Station
import cl.miradio.app.domain.model.StationCategory
import cl.miradio.app.ui.components.CategoryChips
import cl.miradio.app.ui.components.RadioTopBar
import cl.miradio.app.ui.components.RecentStationsRow
import cl.miradio.app.ui.components.RegionChip
import cl.miradio.app.ui.components.StateMessage
import cl.miradio.app.ui.components.StationGrid
import cl.miradio.app.ui.components.StationGridPlaceholder

@Composable
fun HomeRoute(
    currentStationId: String?,
    onPlay: (Station) -> Unit,
    snackbarHostState: SnackbarHostState,
    onOpenDataSaver: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                is HomeMessage.OfflineWithCache -> context.getString(R.string.message_offline_cache)
                is HomeMessage.Updated -> context.getString(R.string.message_updated, message.count)
            }
            snackbarHostState.showSnackbar(text)
        }
    }

    HomeScreen(
        state = state,
        currentStationId = currentStationId,
        onQueryChange = viewModel::onQueryChange,
        onCategorySelected = viewModel::onCategorySelected,
        onRegionSelected = viewModel::onRegionSelected,
        onClearRecent = viewModel::onClearRecent,
        onRefresh = viewModel::onRefresh,
        onRetry = viewModel::onRetry,
        onPlay = onPlay,
        onToggleFavorite = { viewModel.onToggleFavorite(it.id) },
        onOpenDataSaver = onOpenDataSaver,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    currentStationId: String?,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (StationCategory) -> Unit,
    onRegionSelected: (ChileRegion?) -> Unit,
    onClearRecent: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    modifier: Modifier = Modifier,
    onOpenDataSaver: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxSize()) {
        RadioTopBar(
            title = stringResource(R.string.home_title),
            query = state.query,
            onQueryChange = onQueryChange,
            onRefresh = onRefresh,
            onOpenDataSaver = onOpenDataSaver,
        )
        CategoryChips(
            selected = state.category,
            onSelected = onCategorySelected,
            leadingContent = {
                RegionChip(selected = state.region, regions = state.regions, onSelected = onRegionSelected)
            },
        )
        Spacer(Modifier.height(4.dp))

        PullToRefreshBox(
            isRefreshing = state.isFetching && !state.showShimmer,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            val stations = state.stations
            when {
                state.showShimmer -> StationGridPlaceholder(Modifier.fillMaxSize())

                stations.isNullOrEmpty() && state.blockingError != null -> StateMessage(
                    icon = Icons.Rounded.CloudOff,
                    title = stringResource(R.string.error_title),
                    body = stringResource(state.blockingError.messageRes),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = onRetry,
                )

                stations.isNullOrEmpty() -> StateMessage(
                    icon = Icons.Rounded.SearchOff,
                    title = stringResource(R.string.empty_search_title),
                    body = stringResource(
                        if (state.isFiltering) R.string.empty_search_body else R.string.empty_list_body,
                    ),
                )

                else -> StationGrid(
                    stations = stations,
                    currentStationId = currentStationId,
                    onStationClick = onPlay,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier.fillMaxSize(),
                    header = if (state.showRecent) {
                        {
                            RecentStationsRow(
                                stations = state.recent,
                                currentStationId = currentStationId,
                                onStationClick = onPlay,
                                onClear = onClearRecent,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

private val RefreshResult.Reason.messageRes: Int
    get() = when (this) {
        RefreshResult.Reason.NO_CONNECTION -> R.string.error_no_connection
        RefreshResult.Reason.SERVER -> R.string.error_server
        RefreshResult.Reason.UNKNOWN -> R.string.error_unknown
    }
