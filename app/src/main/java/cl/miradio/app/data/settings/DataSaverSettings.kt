package cl.miradio.app.data.settings

import android.content.Context
import androidx.core.content.edit
import cl.miradio.app.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Preferencia de ahorro de datos y si está activa ahora, según la red. */
@Singleton
class DataSaverSettings @Inject constructor(
    @ApplicationContext context: Context,
    private val networkMonitor: NetworkMonitor,
    @ApplicationScope scope: CoroutineScope,
) {
    private val prefs = context.getSharedPreferences("data_saver", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(
        prefs.getString(KEY_MODE, null)
            ?.let { saved -> DataSaverMode.entries.firstOrNull { it.name == saved } }
            ?: DataSaverMode.DEFAULT,
    )
    val mode: StateFlow<DataSaverMode> = _mode.asStateFlow()

    val isActive: StateFlow<Boolean> = combine(_mode, networkMonitor.isMetered) { mode, metered ->
        mode.isActive(metered)
    }.stateIn(scope, SharingStarted.Eagerly, isActiveNow())

    fun setMode(mode: DataSaverMode) {
        _mode.value = mode
        prefs.edit { putString(KEY_MODE, mode.name) }
    }

    /** Lectura inmediata, para código que no puede suscribirse (Coil, el servicio). */
    fun isActiveNow(): Boolean = _mode.value.isActive(networkMonitor.isMetered.value)

    private companion object {
        const val KEY_MODE = "mode"
    }
}
