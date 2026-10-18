package cl.miradio.app.playback

/** Canción o programa que está sonando, según los metadatos del stream. */
data class NowPlaying(
    val title: String,
    val artist: String? = null,
) {
    /** "Artista · Título", o solo el título si la radio no separa el artista. */
    val display: String get() = listOfNotNull(artist, title).joinToString(" · ")
}

/**
 * Interpreta el texto que emiten las radios en los metadatos ICY (`StreamTitle`)
 * o ID3 (HLS). Cada radio lo escribe a su manera, así que se descarta lo que no
 * aporta: vacío, el nombre de la propia radio, URLs o relleno como "-" o "Unknown".
 */
object NowPlayingParser {

    private const val MAX_LENGTH = 160

    private val placeholders = setOf(
        "-", "unknown", "unknown - unknown", "desconocido", "sin título", "sin titulo",
        "n/a", "null", "untitled", "live", "en vivo", "advert", "publicidad",
    )
    private val urlRegex = Regex("""^(https?://|www\.)\S+$""", RegexOption.IGNORE_CASE)
    private val separatorRegex = Regex("""\s+[-–—]\s+""")
    private val whitespaceRegex = Regex("""\s+""")

    fun parse(raw: String?, stationName: String): NowPlaying? {
        val text = raw
            ?.replace(whitespaceRegex, " ")
            ?.trim()
            ?.trim('-', '–', '—', ' ')
            ?.take(MAX_LENGTH)
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        if (text.lowercase() in placeholders) return null
        if (urlRegex.matches(text)) return null
        if (text.sameAs(stationName)) return null

        val parts = text.split(separatorRegex, limit = 2).map { it.trim() }
        if (parts.size == 2 && parts.all { it.isNotEmpty() }) {
            val (artist, title) = parts
            return when {
                // "Radio X - Noticias": el "artista" es la misma radio.
                artist.sameAs(stationName) -> NowPlaying(title = title)
                title.lowercase() in placeholders -> NowPlaying(title = artist)
                else -> NowPlaying(title = title, artist = artist)
            }
        }
        return NowPlaying(title = text)
    }

    /** Combina título y artista de ID3 (streams HLS), que llegan en campos separados. */
    fun fromParts(title: String?, artist: String?, stationName: String): NowPlaying? {
        val cleanTitle = parse(title, stationName) ?: return parse(artist, stationName)
        val cleanArtist = artist?.trim()?.takeIf {
            it.isNotEmpty() && it.lowercase() !in placeholders && !it.sameAs(stationName)
        }
        return if (cleanArtist != null && cleanTitle.artist == null) {
            cleanTitle.copy(artist = cleanArtist)
        } else {
            cleanTitle
        }
    }

    private fun String.sameAs(other: String): Boolean {
        val a = normalize()
        return a.isNotEmpty() && a == other.normalize()
    }

    private fun String.normalize(): String =
        java.text.Normalizer.normalize(lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
            .replace(Regex("""[^a-z0-9]"""), "")
}
