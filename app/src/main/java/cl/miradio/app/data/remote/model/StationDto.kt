package cl.miradio.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Estación tal como la entrega Radio Browser (`/json/stations/...`).
 * Solo se declaran los campos que usa la app; el resto se ignora al deserializar.
 */
@Serializable
data class StationDto(
    @SerialName("stationuuid") val stationUuid: String,
    @SerialName("name") val name: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("url_resolved") val urlResolved: String = "",
    @SerialName("homepage") val homepage: String = "",
    @SerialName("favicon") val favicon: String = "",
    @SerialName("tags") val tags: String = "",
    @SerialName("countrycode") val countryCode: String = "",
    @SerialName("state") val state: String = "",
    @SerialName("language") val language: String = "",
    @SerialName("codec") val codec: String = "",
    @SerialName("bitrate") val bitrate: Int = 0,
    @SerialName("hls") val hls: Int = 0,
    @SerialName("votes") val votes: Int = 0,
    @SerialName("clickcount") val clickCount: Int = 0,
    @SerialName("lastcheckok") val lastCheckOk: Int = 1,
)

/** Respuesta de `/json/url/{uuid}`, que registra un "click" en la estación. */
@Serializable
data class ClickResponseDto(
    @SerialName("ok") val ok: Boolean = false,
    @SerialName("message") val message: String = "",
    @SerialName("url") val url: String = "",
)
