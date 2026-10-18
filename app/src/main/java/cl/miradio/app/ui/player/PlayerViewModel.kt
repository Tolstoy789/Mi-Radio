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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel compartido por el mini-player, Now Playing y las grillas. */
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
