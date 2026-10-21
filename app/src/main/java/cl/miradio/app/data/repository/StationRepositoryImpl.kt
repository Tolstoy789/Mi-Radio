package cl.miradio.app.data.repository

import cl.miradio.app.BuildConfig
import cl.miradio.app.data.local.dao.FavoriteDao
import cl.miradio.app.data.local.dao.RecentPlayDao
import cl.miradio.app.data.local.dao.StationDao
import cl.miradio.app.data.local.dao.UnavailableStationDao
import cl.miradio.app.data.local.entity.FavoriteEntity
import cl.miradio.app.data.local.entity.StationWithFavorite
import cl.miradio.app.data.local.entity.RecentPlayEntity
import cl.miradio.app.data.local.entity.UnavailableStationEntity
import cl.miradio.app.data.mapper.toDomain
import cl.miradio.app.data.mapper.toEntities
import cl.miradio.app.data.remote.RadioBrowserApi
import cl.miradio.app.domain.model.ChileRegion
import cl.miradio.app.domain.model.Clock
import cl.miradio.app.domain.model.SimilarStations
import cl.miradio.app.domain.model.Station
import cl.miradio.app.domain.model.StationCategory
import cl.miradio.app.domain.model.region
import java.io.IOException
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

@Singleton
class StationRepositoryImpl @Inject constructor(
    private val api: RadioBrowserApi,
    private val stationDao: StationDao,
    private val favoriteDao: FavoriteDao,
    private val recentPlayDao: RecentPlayDao,
    private val unavailableDao: UnavailableStationDao,
    private val clock: Clock,
) : StationRepository {

    private val refreshMutex = Mutex()

    override fun observeStations(
        query: String,
        category: StationCategory,
        region: ChileRegion?,
    ): Flow<List<Station>> {
        val normalizedQuery = query.normalizeForSearch()
        return stationDao.observeStations()
            .withUnavailable()
            .map { stations ->
                stations.asSequence()
                    .filter { category.matches(it) }
                    .filter { region == null || it.region == region }
                    .filter { normalizedQuery.isEmpty() || it.matchesQuery(normalizedQuery) }
                    .toList()
            }
            .flowOn(Dispatchers.Default)
    }

    override fun observeRegions(): Flow<List<ChileRegion>> =
        stationDao.observeStations()
            .map { rows -> rows.mapNotNullTo(mutableSetOf()) { ChileRegion.fromState(it.station.state) }.sorted() }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    override fun observeFavorites(): Flow<List<Station>> = stationDao.observeFavorites().withUnavailable()

    override fun observeIsFavorite(stationId: String): Flow<Boolean> =
        favoriteDao.observeIsFavorite(stationId).distinctUntilChanged()

    override fun observeRecent(): Flow<List<Station>> = stationDao.observeRecent(RECENT_SHOWN).withUnavailable()

    override suspend fun recordPlay(stationId: String) {
        recentPlayDao.record(RecentPlayEntity(stationId, clock.nowMillis()), keep = RECENT_KEPT)
    }

    override suspend fun clearRecent() = recentPlayDao.clear()

    override suspend fun markUnavailable(stationId: String) {
        val now = clock.nowMillis()
        unavailableDao.upsert(UnavailableStationEntity(stationId, now))
        unavailableDao.deleteOlderThan(now - UNAVAILABLE_TTL_MS)
    }

    override suspend fun markAvailable(stationId: String) = unavailableDao.remove(stationId)

    override suspend fun findSimilar(stationId: String): Station? {
        val stations = stationDao.observeStations().withUnavailable().first()
        val target = stations.firstOrNull { it.id == stationId }
            ?: stationDao.getStation(stationId)?.toDomain()
            ?: return null
        return SimilarStations.pick(target, stations)
    }

    /** Une las filas con las radios que fallaron hace poco. */
    private fun Flow<List<StationWithFavorite>>.withUnavailable(): Flow<List<Station>> =
        combine(unavailableDao.observeSince(clock.nowMillis() - UNAVAILABLE_TTL_MS)) { rows, unavailable ->
            val ids = unavailable.toSet()
            rows.map { row -> row.toDomain().let { if (it.id in ids) it.copy(isUnavailable = true) else it } }
        }

    override suspend fun refresh(force: Boolean): RefreshResult = refreshMutex.withLock {
        val now = clock.nowMillis()
        val cachedCount = stationDao.count()
        val lastSync = stationDao.lastSyncedAt()
        if (!force && cachedCount > 0 && lastSync != null && now - lastSync < CACHE_TTL_MS) {
            return RefreshResult.Fresh
        }

        try {
            val entities = api.getStationsByCountry(BuildConfig.COUNTRY_CODE).toEntities(syncedAt = now)
            if (entities.isEmpty()) {
                return RefreshResult.Failed(RefreshResult.Reason.SERVER, hasCachedData = cachedCount > 0)
            }
            stationDao.replaceCache(entities)
            RefreshResult.Updated(entities.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RefreshResult.Failed(e.toReason(), hasCachedData = cachedCount > 0)
        }
    }

    override suspend fun toggleFavorite(stationId: String) {
        if (favoriteDao.isFavorite(stationId)) {
            favoriteDao.remove(stationId)
        } else {
            favoriteDao.add(FavoriteEntity(stationUuid = stationId, addedAt = clock.nowMillis()))
        }
    }

    override suspend fun registerClick(stationId: String) {
        try {
            api.registerClick(stationId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Es solo estadística para Radio Browser; nunca debe afectar la reproducción.
        }
    }

    private fun Station.matchesQuery(normalizedQuery: String): Boolean =
        name.normalizeForSearch().contains(normalizedQuery) ||
            tags.any { it.normalizeForSearch().contains(normalizedQuery) } ||
            frequency?.contains(normalizedQuery) == true ||
            state?.normalizeForSearch()?.contains(normalizedQuery) == true ||
            region?.displayName?.normalizeForSearch()?.contains(normalizedQuery) == true

    private fun Exception.toReason(): RefreshResult.Reason = when (this) {
        is IOException -> RefreshResult.Reason.NO_CONNECTION
        is HttpException, is SerializationException -> RefreshResult.Reason.SERVER
        else -> RefreshResult.Reason.UNKNOWN
    }

    companion object {
        const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L
        const val RECENT_SHOWN = 10
        const val RECENT_KEPT = 30
        const val UNAVAILABLE_TTL_MS = 6 * 60 * 60 * 1000L

        private val DIACRITICS = Regex("\\p{Mn}+")

        internal fun String.normalizeForSearch(): String =
            Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(DIACRITICS, "")
    }
}
