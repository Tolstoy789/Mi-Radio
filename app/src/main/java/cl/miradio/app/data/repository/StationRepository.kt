package cl.miradio.app.data.repository

import cl.miradio.app.domain.model.ChileRegion
import cl.miradio.app.domain.model.Station
import cl.miradio.app.domain.model.StationCategory
import kotlinx.coroutines.flow.Flow

/**
 * Fuente única de estaciones. Room es la verdad local: la UI siempre observa la
 * base de datos y [refresh] solo actualiza la caché desde Radio Browser.
 */
interface StationRepository {

    /**
     * Estaciones filtradas por texto (nombre o tag, sin distinguir tildes), categoría
     * y región. Con [region] en `null` no se filtra por región.
     */
    fun observeStations(
        query: String,
        category: StationCategory,
        region: ChileRegion? = null,
    ): Flow<List<Station>>

    /** Regiones que tienen al menos una estación en caché, de norte a sur. */
    fun observeRegions(): Flow<List<ChileRegion>>

    fun observeFavorites(): Flow<List<Station>>

    /** Últimas estaciones escuchadas, la más reciente primero. */
    fun observeRecent(): Flow<List<Station>>

    /** Guarda la estación en el historial de escucha local. */
    suspend fun recordPlay(stationId: String)

    suspend fun clearRecent()

    /** Marca que la radio no está transmitiendo; las listas la muestran como "Sin señal". */
    suspend fun markUnavailable(stationId: String)

    /** La radio volvió a sonar. */
    suspend fun markAvailable(stationId: String)

    /** La radio disponible más parecida a [stationId], o `null` si no hay ninguna. */
    suspend fun findSimilar(stationId: String): Station?

    /**
     * Descarga las estaciones si la caché está vacía, vencida o si [force] es `true`.
     * Devuelve [RefreshResult] en vez de lanzar, para que la UI decida cómo mostrarlo.
     */
    suspend fun refresh(force: Boolean = false): RefreshResult

    suspend fun toggleFavorite(stationId: String)

    /** Avisa a Radio Browser que se reprodujo la estación. No falla si no hay red. */
    suspend fun registerClick(stationId: String)
}

sealed interface RefreshResult {
    /** La caché se actualizó con [count] estaciones. */
    data class Updated(val count: Int) : RefreshResult

    /** La caché seguía vigente y no hizo falta llamar a la API. */
    data object Fresh : RefreshResult

    data class Failed(val reason: Reason, val hasCachedData: Boolean) : RefreshResult

    enum class Reason { NO_CONNECTION, SERVER, UNKNOWN }
}
