package cl.miradio.app.domain.model

/** Radios destacadas en Inicio: las más votadas en Radio Browser que están transmitiendo. */
object FeaturedStations {

    const val COUNT = 8

    /** Con menos candidatas que esto el carrusel se ve vacío y no se muestra. */
    private const val MIN_COUNT = 3

    fun pick(stations: List<Station>, count: Int = COUNT): List<Station> {
        val featured = stations.asSequence()
            .filter { !it.isUnavailable }
            .sortedByDescending { it.votes }
            .take(count)
            .toList()
        return if (featured.size < MIN_COUNT) emptyList() else featured
    }
}
