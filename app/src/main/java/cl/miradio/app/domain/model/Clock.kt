package cl.miradio.app.domain.model

/** Fuente de la hora actual, inyectable para poder testear políticas de caché. */
fun interface Clock {
    fun nowMillis(): Long
}
