package cl.miradio.app.domain.model

/**
 * Categorías del carrusel de chips. Cada una agrupa los tags de Radio Browser
 * que la representan; [ALL] no filtra.
 */
enum class StationCategory(val tags: Set<String>) {
    ALL(emptySet()),
    NEWS(setOf("noticias", "news", "informativa", "informativo", "talk", "actualidad", "deportes", "sports")),
    POP(setOf("pop", "top 40", "hits", "latin pop", "pop latino", "éxitos", "exitos")),
    ROCK(setOf("rock", "classic rock", "rock clásico", "rock clasico", "alternative", "alternativo", "metal", "indie")),
    ;

    fun matches(station: Station): Boolean =
        this == ALL || station.tags.any { tag -> tag in tags || tags.any { tag.contains(it) } }
}
