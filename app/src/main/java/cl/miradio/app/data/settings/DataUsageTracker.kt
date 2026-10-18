package cl.miradio.app.data.settings

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Cuenta los bytes reales que descargan los streams (no una estimación), separando
 * datos móviles de Wi-Fi, y los acumula por mes. ExoPlayer lo llama desde su hilo
 * de carga; se guarda en disco cada [FLUSH_EVERY_BYTES] y al cerrar cada conexión.
 */
@OptIn(UnstableApi::class)
@Singleton
class DataUsageTracker @Inject constructor(
    @ApplicationContext context: Context,
    private val networkMonitor: NetworkMonitor,
) : TransferListener {

    private val prefs = context.getSharedPreferences("data_usage", Context.MODE_PRIVATE)

    private val _usage = MutableStateFlow(load())
    /** Consumo guardado; puede ir hasta [FLUSH_EVERY_BYTES] detrás del real. */
    val usage: StateFlow<DataUsage> = _usage.asStateFlow()

    private var pendingBytes = 0L
    private var pendingMetered = false

    /** Consumo del mes en curso (vacío si lo guardado es de un mes anterior). */
    fun currentMonthUsage(): DataUsage = _usage.value.forMonth(currentMonth())

    override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int) {
        if (!isNetwork) return
        synchronized(this) {
            val metered = networkMonitor.isMetered.value
            if (metered != pendingMetered) flushLocked()
            pendingMetered = metered
            pendingBytes += bytesTransferred
            if (pendingBytes >= FLUSH_EVERY_BYTES) flushLocked()
        }
    }

    override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
        if (isNetwork) synchronized(this) { flushLocked() }
    }

    override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit
    override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit

    private fun flushLocked() {
        if (pendingBytes == 0L) return
        val bytes = pendingBytes
        pendingBytes = 0
        _usage.update { it.add(currentMonth(), bytes, pendingMetered) }
        val saved = _usage.value
        prefs.edit {
            putString(KEY_MONTH, saved.month)
            putLong(KEY_MOBILE, saved.mobileBytes)
            putLong(KEY_WIFI, saved.wifiBytes)
        }
    }

    private fun load(): DataUsage = DataUsage(
        month = prefs.getString(KEY_MONTH, null) ?: currentMonth(),
        mobileBytes = prefs.getLong(KEY_MOBILE, 0),
        wifiBytes = prefs.getLong(KEY_WIFI, 0),
    )

    private fun currentMonth(): String {
        val now = Calendar.getInstance()
        return String.format(Locale.ROOT, "%04d-%02d", now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
    }

    private companion object {
        const val FLUSH_EVERY_BYTES = 512 * 1024L
        const val KEY_MONTH = "month"
        const val KEY_MOBILE = "mobile_bytes"
        const val KEY_WIFI = "wifi_bytes"
    }
}
