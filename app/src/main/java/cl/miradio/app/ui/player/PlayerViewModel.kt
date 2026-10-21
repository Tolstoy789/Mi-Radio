package cl.miradio.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.miradio.app.data.repository.StationRepository
import cl.miradio.app.data.settings.DataSaverSettings
import cl.miradio.app.data.settings.NetworkMonitor
import cl.miradio.app.domain.model.Station
import cl.miradio.app.playback.PlayerController
import cl.miradio.app.playback.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel compartido por el mini-player, Now Playing y las grillas. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val repository: StationRepository,
    networkMonitor: NetworkMonitor,
    dataSaverSettings: DataSaverSettings,
) : ViewModel() {

    val state: StateFlow<PlayerUiState> = playerController.state

    val isDataSaverActive: StateFlow<Boolean> = dataSaverSettings.isActive

    /** La radio actual no está transmitiendo: Now Playing ofrece una parecida. */
    val isStationDown: StateFlow<Boolean> = combine(state, networkMonitor.isOnline) { player, online ->
        player.isStationFault(online)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** La radio que suena está en favoritas (corazón de Now Playing). */
    val isFavorite: StateFlow<Boolean> = state
        .map { it.stationId }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(false) else repository.observeIsFavorite(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _messages = Channel<PlayerMessage>(Channel.BUFFERED)
    val messages: Flow<PlayerMessage> = _messages.receiveAsFlow()

    init {
        // Recuerda qué radios fallan (tarjeta "Sin señal") y las limpia cuando vuelven a sonar.
        viewModelScope.launch {
            combine(state, isStationDown) { player, down ->
                StationHealth(player.stationId, isPlaying = player.isPlaying, isDown = down)
            }
                .distinctUntilChanged()
                .collect { health ->
                    val id = health.stationId ?: return@collect
                    when {
                        health.isDown -> repository.markUnavailable(id)
                        health.isPlaying -> repository.markAvailable(id)
                    }
                }
        }
    }

    fun play(station: Station) {
        viewModelScope.launch {
            if (state.value.stationId == station.id && state.value.error == null) {
                playerController.togglePlayPause()
                return@launch
            }
            playerController.play(station)
            repository.recordPlay(station.id)
            repository.registerClick(station.id)
        }
    }

    /** Cambia a la radio más parecida a la que no está transmitiendo. */
    fun playSimilar() {
        val currentId = state.value.stationId ?: return
        viewModelScope.launch {
            val similar = repository.findSimilar(currentId)
            if (similar == null) {
                _messages.send(PlayerMessage.NoSimilarStation)
                return@launch
            }
            play(similar)
            _messages.send(PlayerMessage.SwitchedTo(similar.name))
        }
    }

    fun toggleFavorite() {
        val id = state.value.stationId ?: return
        viewModelScope.launch { repository.toggleFavorite(id) }
    }

    fun togglePlayPause() {
        viewModelScope.launch { playerController.togglePlayPause() }
    }

    fun setVolume(volume: Float) {
        viewModelScope.launch { playerController.setVolume(volume) }
    }

    fun setSleepTimer(minutes: Int) {
        viewModelScope.launch { playerController.setSleepTimer(minutes) }
    }

    private data class StationHealth(val stationId: String?, val isPlaying: Boolean, val isDown: Boolean)
}

/** Avisos de una sola vez que la UI muestra en un Snackbar. */
sealed interface PlayerMessage {
    data object NoSimilarStation : PlayerMessage
    data class SwitchedTo(val stationName: String) : PlayerMessage
}
