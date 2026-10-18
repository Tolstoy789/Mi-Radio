package cl.miradio.app.ui.home

import cl.miradio.app.data.repository.RefreshResult
import cl.miradio.app.domain.model.Station
import cl.miradio.app.domain.model.StationCategory

data class HomeUiState(
    /** `null` mientras Room aún no entrega la primera lectura. */
    val stations: List<Station>? = null,
    val query: String = "",
    val category: StationCategory = StationCategory.ALL,
    /** Hay una descarga en curso desde la API. */
    val isFetching: Boolean = false,
    /** Error de carga que impide mostrar la lista (no hay nada en caché). */
    val blockingError: RefreshResult.Reason? = null,
) {
    /** Primera carga sin datos: la grilla muestra placeholders con shimmer. */
    val showShimmer: Boolean
        get() = stations == null || (stations.isEmpty() && isFetching && query.isEmpty() && category == StationCategory.ALL)

    val isFiltering: Boolean get() = query.isNotBlank() || category != StationCategory.ALL
}

/** Mensajes de una sola vez que la UI muestra en un Snackbar. */
sealed interface HomeMessage {
    data class OfflineWithCache(val reason: RefreshResult.Reason) : HomeMessage
    data class Updated(val count: Int) : HomeMessage
}
