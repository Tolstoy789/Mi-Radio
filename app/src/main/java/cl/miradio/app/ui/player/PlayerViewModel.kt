package cl.miradio.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.miradio.app.data.repository.StationRepository
import cl.miradio.app.domain.model.Station
import cl.miradio.app.playback.PlayerController
import cl.miradio.app.playback.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** ViewModel compartido por el mini-player, Now Playing y las grillas. */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val repository: StationRepository,
) : ViewModel() {

    val state: StateFlow<PlayerUiState> = playerController.state

    fun play(station: Station) {
        viewModelScope.launch {
            if (state.value.stationId == station.id && state.value.error == null) {
                playerController.togglePlayPause()
                return@launch
            }
            playerController.play(station)
            repository.registerClick(station.id)
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
}
