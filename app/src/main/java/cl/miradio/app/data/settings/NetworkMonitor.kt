package cl.miradio.app.data.settings

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado de la red por defecto: si hay internet y si es una red medida (datos móviles). */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val connectivityManager = requireNotNull(context.getSystemService<ConnectivityManager>())

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isMetered = MutableStateFlow(connectivityManager.isActiveNetworkMetered)
    val isMetered: StateFlow<Boolean> = _isMetered.asStateFlow()

    init {
        connectivityManager.activeNetwork?.let(connectivityManager::getNetworkCapabilities)
            ?.let(::update)
            ?: run { _isOnline.value = false }

        connectivityManager.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
                    update(capabilities)

                override fun onLost(network: Network) {
                    _isOnline.value = false
                }
            },
        )
    }

    private fun update(capabilities: NetworkCapabilities) {
        _isOnline.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        _isMetered.value = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
