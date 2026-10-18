package cl.miradio.app.data.repository

import cl.miradio.app.data.local.dao.FavoriteDao
import cl.miradio.app.data.local.dao.RecentPlayDao
import cl.miradio.app.data.local.dao.StationDao
import cl.miradio.app.data.local.dao.UnavailableStationDao
import cl.miradio.app.data.local.entity.FavoriteEntity
import cl.miradio.app.data.local.entity.RecentPlayEntity
import cl.miradio.app.data.local.entity.StationWithFavorite
import cl.miradio.app.data.local.entity.UnavailableStationEntity
import cl.miradio.app.data.mapper.toEntities
import cl.miradio.app.data.remote.RadioBrowserApi
import cl.miradio.app.data.remote.model.StationDto
import cl.miradio.app.domain.model.ChileRegion
import cl.miradio.app.domain.model.StationCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StationRepositoryImplTest {

    private val api = mockk<RadioBrowserApi>()
    private val stationDao = mockk<StationDao>(relaxUnitFun = true)
    private val favoriteDao = mockk<FavoriteDao>(relaxUnitFun = true)
    private val recentPlayDao = mockk<RecentPlayDao>(relaxUnitFun = true)
    private val unavailableDao = mockk<UnavailableStationDao>(relaxUnitFun = true) {
        every { observeSince(any()) } returns flowOf(emptyList())
    }
    private var now = 1_000_000_000L
    private val repository =
        StationRepositoryImpl(api, stationDao, favoriteDao, recentPlayDao, unavailableDao) { now }

    private val dtos = listOf(
        StationDto(stationUuid = "1", name = "Radio Bío-Bío", url = "http://a", tags = "noticias,chile", state = "Concepción"),
        StationDto(stationUuid = "2", name = "Rock & Pop", url = "http://b", tags = "rock,pop", state = "Santiago"),
        StationDto(stationUuid = "3", name = "Radio Futuro 88.9", url = "http://c", tags = "rock,classic rock", state = "Región Metropolitana"),
    )

    @Test
    fun `no llama a la API si la cache esta vigente`() = runTest {
        coEvery { stationDao.count() } returns 3
        coEvery { stationDao.lastSyncedAt() } returns now - 60_000

        val result = repository.refresh()

        assertEquals(RefreshResult.Fresh, result)
        coVerify(exactly = 0) { api.getStationsByCountry(any(), any(), any(), any()) }
    }

    @Test
    fun `descarga si la cache esta vacia, vencida o se fuerza`() = runTest {
        coEvery { api.getStationsByCountry(any(), any(), any(), any()) } returns dtos

        coEvery { stationDao.count() } returns 0
        coEvery { stationDao.lastSyncedAt() } returns null
        assertEquals(RefreshResult.Updated(3), repository.refresh())

        coEvery { stationDao.count() } returns 3
        coEvery { stationDao.lastSyncedAt() } returns now - StationRepositoryImpl.CACHE_TTL_MS - 1
        assertEquals(RefreshResult.Updated(3), repository.refresh())

        coEvery { stationDao.lastSyncedAt() } returns now
        assertEquals(RefreshResult.Updated(3), repository.refresh(force = true))

        coVerify(exactly = 3) { stationDao.replaceCache(match { rows -> rows.all { it.syncedAt == now } }) }
    }

    @Test
    fun `un error de red informa si hay datos guardados`() = runTest {
        coEvery { api.getStationsByCountry(any(), any(), any(), any()) } throws IOException("sin red")
        coEvery { stationDao.lastSyncedAt() } returns null

        coEvery { stationDao.count() } returns 0
        assertEquals(
            RefreshResult.Failed(RefreshResult.Reason.NO_CONNECTION, hasCachedData = false),
            repository.refresh(),
        )

        coEvery { stationDao.count() } returns 5
        assertEquals(
            RefreshResult.Failed(RefreshResult.Reason.NO_CONNECTION, hasCachedData = true),
            repository.refresh(force = true),
        )
    }

    @Test
    fun `filtra por categoria y por texto sin distinguir tildes`() = runTest {
        val rows = dtos.toEntities(syncedAt = now).map { StationWithFavorite(it, isFavorite = it.stationUuid == "2") }
        every { stationDao.observeStations() } returns flowOf(rows)

        assertEquals(listOf("2", "3"), repository.observeStations("", StationCategory.ROCK).first().map { it.id })
        assertEquals(listOf("1"), repository.observeStations("bio-bio", StationCategory.ALL).first().map { it.id })
        assertEquals(listOf("1"), repository.observeStations("BÍO", StationCategory.NEWS).first().map { it.id })
        assertEquals(listOf("3"), repository.observeStations("88.9", StationCategory.ALL).first().map { it.id })
        assertEquals(true, repository.observeStations("rock", StationCategory.ALL).first().first().isFavorite)
    }

    @Test
    fun `toggleFavorite agrega o quita segun el estado actual`() = runTest {
        coEvery { favoriteDao.isFavorite("1") } returns false
        repository.toggleFavorite("1")
        coVerify { favoriteDao.add(FavoriteEntity("1", now)) }

        coEvery { favoriteDao.isFavorite("1") } returns true
        repository.toggleFavorite("1")
        coVerify { favoriteDao.remove("1") }
    }

    @Test
    fun `registerClick nunca propaga errores`() = runTest {
        coEvery { api.registerClick(any()) } throws IOException("sin red")
        repository.registerClick("1")
    }

    @Test
    fun `filtra por region y lista las regiones de norte a sur`() = runTest {
        val rows = dtos.toEntities(syncedAt = now).map { StationWithFavorite(it, isFavorite = false) }
        every { stationDao.observeStations() } returns flowOf(rows)

        assertEquals(
            listOf("2", "3"),
            repository.observeStations("", StationCategory.ALL, ChileRegion.METROPOLITANA).first().map { it.id },
        )
        assertEquals(
            listOf("1"),
            repository.observeStations("", StationCategory.ALL, ChileRegion.BIOBIO).first().map { it.id },
        )
        assertEquals(listOf("1"), repository.observeStations("biobio", StationCategory.ALL).first().map { it.id })
        assertEquals(listOf(ChileRegion.METROPOLITANA, ChileRegion.BIOBIO), repository.observeRegions().first())
    }

    @Test
    fun `recordPlay guarda la escucha con la hora actual`() = runTest {
        repository.recordPlay("2")
        coVerify { recentPlayDao.record(RecentPlayEntity("2", now), StationRepositoryImpl.RECENT_KEPT) }
    }

    @Test
    fun `marca radios sin senal y sugiere una parecida disponible`() = runTest {
        val rows = dtos.toEntities(syncedAt = now).map { StationWithFavorite(it, isFavorite = false) }
        every { stationDao.observeStations() } returns flowOf(rows)
        every { unavailableDao.observeSince(now - StationRepositoryImpl.UNAVAILABLE_TTL_MS) } returns flowOf(listOf("3"))

        val stations = repository.observeStations("", StationCategory.ALL).first()
        assertEquals(listOf(false, false, true), stations.map { it.isUnavailable })

        // "3" es la única parecida a "2", pero está caída; "2" sí sirve como reemplazo de "3".
        assertEquals(null, repository.findSimilar("2"))
        assertEquals("2", repository.findSimilar("3")?.id)

        repository.markUnavailable("1")
        coVerify { unavailableDao.upsert(UnavailableStationEntity("1", now)) }
        coVerify { unavailableDao.deleteOlderThan(now - StationRepositoryImpl.UNAVAILABLE_TTL_MS) }
    }
}
