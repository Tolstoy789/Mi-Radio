package cl.miradio.app.data.mapper

import cl.miradio.app.data.local.entity.StationEntity
import cl.miradio.app.data.local.entity.StationWithFavorite
import cl.miradio.app.data.remote.model.StationDto
import cl.miradio.app.domain.model.Station

/**
 * Convierte la respuesta de la API en filas de caché. Descarta estaciones sin
 * URL de stream o caídas en el último chequeo, y elimina UUID duplicados.
 */
fun List<StationDto>.toEntities(syncedAt: Long): List<StationEntity> =
    asSequence()
        .filter { it.lastCheckOk == 1 }
        .mapNotNull { dto -> dto.streamUrlOrNull()?.let { dto to it } }
        .distinctBy { (dto, _) -> dto.stationUuid }
        .mapIndexed { rank, (dto, streamUrl) -> dto.toEntity(streamUrl, rank, syncedAt) }
        .toList()

private fun StationDto.streamUrlOrNull(): String? =
    urlResolved.trim().ifBlank { url.trim() }.takeIf { it.startsWith("http", ignoreCase = true) }

private fun StationDto.toEntity(streamUrl: String, rank: Int, syncedAt: Long) = StationEntity(
    stationUuid = stationUuid,
    name = name.trim().replace(Regex("\\s+"), " ").ifBlank { "Radio sin nombre" },
    streamUrl = streamUrl,
    homepage = homepage.trim().ifBlank { null },
    faviconUrl = favicon.trim().takeIf { it.startsWith("http", ignoreCase = true) },
    tags = tags.lowercase().split(',').map(String::trim).filter(String::isNotEmpty).joinToString(","),
    countryCode = countryCode.uppercase(),
    state = state.trim().ifBlank { null },
    language = language.trim().ifBlank { null },
    codec = codec.trim().ifBlank { null },
    bitrate = bitrate,
    isHls = hls == 1,
    votes = votes,
    clickCount = clickCount,
    popularityRank = rank,
    syncedAt = syncedAt,
)

fun StationWithFavorite.toDomain(): Station = station.toDomain(isFavorite)

fun StationEntity.toDomain(isFavorite: Boolean = false): Station {
    val tagList = tags.split(',').filter(String::isNotEmpty)
    return Station(
        id = stationUuid,
        name = name,
        streamUrl = streamUrl,
        faviconUrl = faviconUrl,
        homepage = homepage,
        tags = tagList,
        frequency = extractFrequency(name) ?: tagList.firstNotNullOfOrNull(::extractFrequency),
        state = state,
        codec = codec,
        bitrate = bitrate,
        isHls = isHls,
        votes = votes,
        isFavorite = isFavorite,
    )
}

private val FM_REGEX = Regex("""(?<![\d.])(8[7-9]|9\d|10[0-8])[.,](\d)(?!\d)""")
private val AM_REGEX = Regex("""(?<![\d.])(5[3-9]\d|[6-9]\d\d|1[0-6]\d\d|1700)\s*(?:am|khz)""", RegexOption.IGNORE_CASE)

/** Busca un dial FM (87.5–108.9) o AM (530–1700 kHz) dentro de un texto. */
internal fun extractFrequency(text: String): String? {
    FM_REGEX.find(text)?.let { return "${it.groupValues[1]}.${it.groupValues[2]} FM" }
    AM_REGEX.find(text)?.let { return "${it.groupValues[1]} AM" }
    return null
}
