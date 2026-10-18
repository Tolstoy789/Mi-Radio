package cl.miradio.app.domain.model

/** Estación lista para mostrar en la UI y reproducir. */
data class Station(
    val id: String,
    val name: String,
    val streamUrl: String,
    val faviconUrl: String?,
    val homepage: String?,
    val tags: List<String>,
    /** Dial FM/AM extraído del nombre o de los tags (p. ej. "89.7 FM"), si existe. */
    val frequency: String?,
    val state: String?,
    val codec: String?,
    val bitrate: Int,
    val isHls: Boolean,
    val votes: Int,
    val isFavorite: Boolean,
    /** Falló hace poco al intentar reproducirla (la radio no está transmitiendo). */
    val isUnavailable: Boolean = false,
)

/** Línea secundaria de la tarjeta: "93.3 FM · Santiago", solo lo que se conozca. */
val Station.dialAndPlace: String?
    get() = listOfNotNull(frequency, state?.takeIf { it.isNotBlank() })
        .joinToString(" · ")
        .ifEmpty { null }
