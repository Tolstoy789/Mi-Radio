package cl.miradio.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.miradio.app.R
import cl.miradio.app.data.repository.RefreshResult
import cl.miradio.app.domain.model.ChileRegion
import cl.miradio.app.domain.model.DayPeriod
import cl.miradio.app.domain.model.Station
import cl.miradio.app.domain.model.StationCategory
import cl.miradio.app.ui.components.CategoryChips
import cl.miradio.app.ui.components.FeaturedStationsRow
import cl.miradio.app.ui.components.RadioTopBar
import cl.miradio.app.ui.components.RecentStationsRow
import cl.miradio.app.ui.components.RegionChip
import cl.miradio.app.ui.components.SectionTitle
import cl.miradio.app.ui.components.StateMessage
import cl.miradio.app.ui.components.StationGrid
import cl.miradio.app.ui.components.StationGridPlaceholder
import cl.miradio.app.ui.theme.Accent
import cl.miradio.app.ui.theme.MiniPlayerSurface
import java.util.Calendar

@Composable
fun HomeRoute(
    currentStationId: String?,
    isPlaying: Boolean,
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
        isPlaying = isPlaying,
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

/** Qué ocupa el área bajo los filtros; cambiar de uno a otro se hace con un fundido. */
private enum class HomeContent { LOADING, ERROR, EMPTY, LIST }

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
    isPlaying: Boolean = false,
    onOpenDataSaver: (() -> Unit)? = null,
) {
    val greeting = remember { DayPeriod.of(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    val pullState = rememberPullToRefreshState()
    val isRefreshing = state.isFetching && !state.showShimmer
    val stations = state.stations
    val content = when {
        state.showShimmer -> HomeContent.LOADING
        stations.isNullOrEmpty() && state.blockingError != null -> HomeContent.ERROR
        stations.isNullOrEmpty() -> HomeContent.EMPTY
        else -> HomeContent.LIST
    }

    Column(modifier.fillMaxSize()) {
        RadioTopBar(
            title = stringResource(R.string.home_title),
            subtitle = stringResource(greeting.greetingRes),
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
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            state = pullState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = MiniPlayerSurface,
                    color = Accent,
                )
            },
        ) {
            AnimatedContent(
                targetState = content,
                transitionSpec = { fadeIn(tween(durationMillis = 250)) togetherWith fadeOut(tween(durationMillis = 150)) },
                label = "homeContent",
            ) { target ->
                when (target) {
                    HomeContent.LOADING -> StationGridPlaceholder(Modifier.fillMaxSize())

                    HomeContent.ERROR -> StateMessage(
                        icon = Icons.Rounded.CloudOff,
                        title = stringResource(R.string.error_title),
                        body = stringResource(state.blockingError?.messageRes ?: R.string.error_unknown),
                        actionLabel = stringResource(R.string.action_retry),
                        onAction = onRetry,
                    )

                    HomeContent.EMPTY -> StateMessage(
                        icon = Icons.Rounded.SearchOff,
                        title = stringResource(R.string.empty_search_title),
                        body = stringResource(
                            if (state.isFiltering) R.string.empty_search_body else R.string.empty_list_body,
                        ),
                    )

                    HomeContent.LIST -> StationGrid(
                        stations = stations.orEmpty(),
                        currentStationId = currentStationId,
                        isPlaying = isPlaying,
                        onStationClick = onPlay,
                        onToggleFavorite = onToggleFavorite,
                        modifier = Modifier.fillMaxSize(),
                        header = if (state.showFeatured || state.showRecent) {
                            {
                                HomeHeader(
                                    state = state,
                                    currentStationId = currentStationId,
                                    isPlaying = isPlaying,
                                    onPlay = onPlay,
                                    onClearRecent = onClearRecent,
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
}

/** Destacadas y recientes sobre la grilla, en la vista general sin filtros. */
@Composable
private fun HomeHeader(
    state: HomeUiState,
    currentStationId: String?,
    isPlaying: Boolean,
    onPlay: (Station) -> Unit,
    onClearRecent: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.showFeatured) {
            FeaturedStationsRow(
                stations = state.featured,
                currentStationId = currentStationId,
                isPlaying = isPlaying,
                onStationClick = onPlay,
            )
        }
        if (state.showRecent) {
            RecentStationsRow(
                stations = state.recent,
                currentStationId = currentStationId,
                onStationClick = onPlay,
                onClear = onClearRecent,
            )
        }
        SectionTitle(stringResource(R.string.all_stations_title), modifier = Modifier.padding(top = 4.dp))
    }
}

private val DayPeriod.greetingRes: Int
    get() = when (this) {
        DayPeriod.MORNING -> R.string.greeting_morning
        DayPeriod.AFTERNOON -> R.string.greeting_afternoon
        DayPeriod.NIGHT -> R.string.greeting_night
    }

private val RefreshResult.Reason.messageRes: Int
    get() = when (this) {
        RefreshResult.Reason.NO_CONNECTION -> R.string.error_no_connection
        RefreshResult.Reason.SERVER -> R.string.error_server
        RefreshResult.Reason.UNKNOWN -> R.string.error_unknown
    }
