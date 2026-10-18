package cl.miradio.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.miradio.app.data.settings.DataSaverMode
import cl.miradio.app.data.settings.DataSaverSettings
import cl.miradio.app.data.settings.DataUsage
import cl.miradio.app.data.settings.DataUsageTracker
import cl.miradio.app.data.settings.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DataSaverUiState(
    val mode: DataSaverMode = DataSaverMode.DEFAULT,
    val isActive: Boolean = false,
    val isMetered: Boolean = false,
    val usage: DataUsage = DataUsage(month = ""),
)

@HiltViewModel
class DataSaverViewModel @Inject constructor(
    private val settings: DataSaverSettings,
    networkMonitor: NetworkMonitor,
    private val usageTracker: DataUsageTracker,
) : ViewModel() {

    val uiState: StateFlow<DataSaverUiState> = combine(
        settings.mode,
        settings.isActive,
        networkMonitor.isMetered,
        usageTracker.usage,
    ) { mode, active, metered, _ ->
        DataSaverUiState(mode, active, metered, usageTracker.currentMonthUsage())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DataSaverUiState(usage = usageTracker.currentMonthUsage()),
    )

    fun onModeSelected(mode: DataSaverMode) = settings.setMode(mode)
}
