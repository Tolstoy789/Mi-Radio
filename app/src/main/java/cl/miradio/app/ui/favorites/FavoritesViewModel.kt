package cl.miradio.app.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.miradio.app.data.repository.StationRepository
import cl.miradio.app.domain.model.Station
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: StationRepository,
) : ViewModel() {

    /** `null` hasta la primera lectura de Room. */
    val favorites: StateFlow<List<Station>?> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onToggleFavorite(stationId: String) {
        viewModelScope.launch { repository.toggleFavorite(stationId) }
    }
}
