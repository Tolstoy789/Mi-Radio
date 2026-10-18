package cl.miradio.app.data.remote

import cl.miradio.app.data.remote.model.ClickResponseDto
import cl.miradio.app.data.remote.model.StationDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API pública de Radio Browser. Documentación: https://api.radio-browser.info
 * El header `User-Agent` lo agrega [UserAgentInterceptor] en todas las llamadas.
 */
interface RadioBrowserApi {

    /** Estaciones de un país, ordenadas por votos (las más populares primero). */
    @GET("json/stations/bycountrycodeexact/{countryCode}")
    suspend fun getStationsByCountry(
        @Path("countryCode") countryCode: String,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hideBroken: Boolean = true,
    ): List<StationDto>

    /** Búsqueda en el servidor por nombre y/o tag dentro de un país. */
    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("countrycode") countryCode: String,
        @Query("name") name: String? = null,
        @Query("tag") tag: String? = null,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("limit") limit: Int = 100,
    ): List<StationDto>

    /** Registra una reproducción; Radio Browser lo usa para su ranking de popularidad. */
    @GET("json/url/{stationUuid}")
    suspend fun registerClick(@Path("stationUuid") stationUuid: String): ClickResponseDto
}
