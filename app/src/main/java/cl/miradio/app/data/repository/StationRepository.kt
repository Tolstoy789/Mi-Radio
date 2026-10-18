package cl.miradio.app.data.repository

import cl.miradio.app.domain.model.Station
import cl.miradio.app.domain.model.StationCategory
import kotlinx.coroutines.flow.Flow

/**
 * Fuente única de estaciones. Room es la verdad local: la UI siempre observa la
 * base de datos y [refresh] solo actualiza la caché desde Radio Browser.
 */
interface StationRepository {

    /** Estaciones filtradas por texto (nombre o tag, sin distinguir tildes) y categoría. */
    fun observeStations(query: String, category: StationCategory): Flow<List<Station>>

    fun observeFavorites(): Flow<List<Station>>

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
