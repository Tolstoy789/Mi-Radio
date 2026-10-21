package cl.miradio.app.domain.model

/** Parte del día, para saludar en Inicio ("Buenos días", "Buenas tardes", "Buenas noches"). */
enum class DayPeriod {
    MORNING,
    AFTERNOON,
    NIGHT,
    ;

    companion object {
        /** [hour] va de 0 a 23, en la hora local del teléfono. */
        fun of(hour: Int): DayPeriod = when (hour) {
            in 5..11 -> MORNING
            in 12..19 -> AFTERNOON
            else -> NIGHT
        }
    }
}
