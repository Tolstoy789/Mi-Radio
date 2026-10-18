package cl.miradio.app.data.repository

import cl.miradio.app.BuildConfig
import cl.miradio.app.data.local.dao.FavoriteDao
import cl.miradio.app.data.local.dao.StationDao
import cl.miradio.app.data.local.entity.FavoriteEntity
import cl.miradio.app.data.mapper.toDomain
import cl.miradio.app.data.mapper.toEntities
import cl.miradio.app.data.remote.RadioBrowserApi
import cl.miradio.app.domain.model.Clock
import cl.miradio.app.domain.model.Station
import cl.miradio.app.domain.model.StationCategory
import java.io.IOException
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
    private val clock: Clock,
) : StationRepository {

    private val refreshMutex = Mutex()

    override fun observeStations(query: String, category: StationCategory): Flow<List<Station>> {
        val normalizedQuery = query.normalizeForSearch()
        return stationDao.observeStations()
            .map { rows ->
                rows.asSequence()
                    .map { it.toDomain() }
                    .filter { category.matches(it) }
                    .filter { normalizedQuery.isEmpty() || it.matchesQuery(normalizedQuery) }
                    .toList()
            }
            .flowOn(Dispatchers.Default)
    }

    override fun observeFavorites(): Flow<List<Station>> =
        stationDao.observeFavorites().map { rows -> rows.map { it.toDomain() } }

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
            frequency?.contains(normalizedQuery) == true

    private fun Exception.toReason(): RefreshResult.Reason = when (this) {
        is IOException -> RefreshResult.Reason.NO_CONNECTION
        is HttpException, is SerializationException -> RefreshResult.Reason.SERVER
        else -> RefreshResult.Reason.UNKNOWN
    }

    companion object {
        const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L

        private val DIACRITICS = Regex("\\p{Mn}+")

        internal fun String.normalizeForSearch(): String =
            Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(DIACRITICS, "")
    }
}
