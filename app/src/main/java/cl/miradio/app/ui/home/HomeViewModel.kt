package cl.miradio.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.miradio.app.data.repository.RefreshResult
import cl.miradio.app.data.repository.StationRepository
import cl.miradio.app.domain.model.Station
import cl.miradio.app.domain.model.StationCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: StationRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow(StationCategory.ALL)
    private val fetchState = MutableStateFlow(FetchState())

    private val _messages = Channel<HomeMessage>(Channel.BUFFERED)
    val messages: Flow<HomeMessage> = _messages.receiveAsFlow()

    private var refreshJob: Job? = null

    private val stations = combine(
        query.debounce { if (it.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }.distinctUntilChanged(),
        category,
    ) { q, c -> q to c }
        .flatMapLatest { (q, c) -> repository.observeStations(q, c) }

    val uiState: StateFlow<HomeUiState> = combine(
        stations.onStart<List<Station>?> { emit(null) },
        query,
        category,
        fetchState,
    ) { stations, query, category, fetch ->
        HomeUiState(
            stations = stations,
            query = query,
            category = category,
            isFetching = fetch.isFetching,
            blockingError = fetch.blockingError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    init {
        refresh(force = false)
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onCategorySelected(value: StationCategory) {
        category.value = value
    }

    fun onRefresh() = refresh(force = true)

    fun onRetry() = refresh(force = true)

    fun onToggleFavorite(stationId: String) {
        viewModelScope.launch { repository.toggleFavorite(stationId) }
    }

    private fun refresh(force: Boolean) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            fetchState.update { it.copy(isFetching = true) }
            val result = repository.refresh(force)
            fetchState.value = FetchState(
                isFetching = false,
                blockingError = (result as? RefreshResult.Failed)?.takeIf { !it.hasCachedData }?.reason,
            )
            when (result) {
                is RefreshResult.Failed ->
                    if (result.hasCachedData) _messages.send(HomeMessage.OfflineWithCache(result.reason))
                is RefreshResult.Updated -> if (force) _messages.send(HomeMessage.Updated(result.count))
                RefreshResult.Fresh -> Unit
            }
        }
    }

    private data class FetchState(
        val isFetching: Boolean = false,
        val blockingError: RefreshResult.Reason? = null,
    )

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
