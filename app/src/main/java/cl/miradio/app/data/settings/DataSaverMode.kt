package cl.miradio.app.data.settings

/** Cuándo se activa el ahorro de datos. */
enum class DataSaverMode {
    OFF,

    /** Solo en redes medidas (datos móviles o un hotspot), que es lo que cuesta plata. */
    MOBILE_ONLY,
    ALWAYS,
    ;

    fun isActive(isMetered: Boolean): Boolean = when (this) {
        OFF -> false
        MOBILE_ONLY -> isMetered
        ALWAYS -> true
    }

    companion object {
        val DEFAULT = MOBILE_ONLY
    }
}

/** Datos consumidos por los streams en el mes [month] (formato "2026-10"). */
data class DataUsage(
    val month: String,
    val mobileBytes: Long = 0,
    val wifiBytes: Long = 0,
) {
    /** Suma [bytes] al mes indicado; si cambió el mes, el contador parte de cero. */
    fun add(currentMonth: String, bytes: Long, isMetered: Boolean): DataUsage {
        val base = if (currentMonth == month) this else DataUsage(currentMonth)
        return if (isMetered) {
            base.copy(mobileBytes = base.mobileBytes + bytes)
        } else {
            base.copy(wifiBytes = base.wifiBytes + bytes)
        }
    }

    /** El mismo registro visto desde [currentMonth]: vacío si es de un mes anterior. */
    fun forMonth(currentMonth: String): DataUsage = if (currentMonth == month) this else DataUsage(currentMonth)
}

/** Megabytes por hora que consume un stream de [bitrateKbps], redondeado. */
fun megabytesPerHour(bitrateKbps: Int): Int = ((bitrateKbps * 3_600L) / 8 / 1_000).toInt()
