package cl.miradio.app.ui.home

import app.cash.turbine.test
import cl.miradio.app.data.repository.RefreshResult
import cl.miradio.app.data.repository.StationRepository
import cl.miradio.app.domain.model.Station
import cl.miradio.app.domain.model.StationCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun station(id: String, vararg tags: String) = Station(
        id = id, name = "Radio $id", streamUrl = "http://$id", faviconUrl = null, homepage = null,
        tags = tags.toList(), frequency = null, state = null, codec = null, bitrate = 0,
        isHls = false, votes = 0, isFavorite = false,
    )

    private class FakeRepository : StationRepository {
        val stations = MutableStateFlow<List<Station>>(emptyList())
        val refreshResult = CompletableDeferred<RefreshResult>()
        val favoritesToggled = mutableListOf<String>()

        override fun observeStations(query: String, category: StationCategory): Flow<List<Station>> =
            stations.map { list -> list.filter { category.matches(it) && it.name.contains(query, ignoreCase = true) } }

        override fun observeFavorites(): Flow<List<Station>> = emptyFlow()
        override suspend fun refresh(force: Boolean): RefreshResult = refreshResult.await()
        override suspend fun toggleFavorite(stationId: String) { favoritesToggled += stationId }
        override suspend fun registerClick(stationId: String) = Unit
    }

    @Test
    fun `muestra shimmer mientras la primera carga no termina`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = HomeViewModel(repository)

        viewModel.uiState.test {
            val loading = expectMostRecentItem()
            assertTrue(loading.isFetching)
            assertTrue(loading.showShimmer)

            repository.stations.value = listOf(station("1", "rock"), station("2", "pop"))
            repository.refreshResult.complete(RefreshResult.Updated(2))
            advanceUntilIdle()

            val loaded = expectMostRecentItem()
            assertFalse(loaded.showShimmer)
            assertFalse(loaded.isFetching)
            assertEquals(2, loaded.stations?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sin red y sin cache muestra un error bloqueante`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = HomeViewModel(repository)

        viewModel.uiState.test {
            repository.refreshResult.complete(
                RefreshResult.Failed(RefreshResult.Reason.NO_CONNECTION, hasCachedData = false),
            )
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(RefreshResult.Reason.NO_CONNECTION, state.blockingError)
            assertFalse(state.showShimmer)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filtra por categoria y delega favoritos al repositorio`() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            stations.value = listOf(station("1", "rock"), station("2", "pop"))
            refreshResult.complete(RefreshResult.Fresh)
        }
        val viewModel = HomeViewModel(repository)

        viewModel.uiState.test {
            viewModel.onCategorySelected(StationCategory.ROCK)
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(listOf("1"), state.stations?.map { it.id })
            assertNull(state.blockingError)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.onToggleFavorite("2")
        assertEquals(listOf("2"), repository.favoritesToggled)
    }
}
