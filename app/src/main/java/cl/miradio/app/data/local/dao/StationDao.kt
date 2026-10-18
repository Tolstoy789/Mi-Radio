package cl.miradio.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import cl.miradio.app.data.local.entity.StationEntity
import cl.miradio.app.data.local.entity.StationWithFavorite
import kotlinx.coroutines.flow.Flow

@Dao
abstract class StationDao {

    @Query(
        """
        SELECT s.*, (f.station_uuid IS NOT NULL) AS is_favorite
        FROM stations s
        LEFT JOIN favorites f ON f.station_uuid = s.station_uuid
        ORDER BY s.popularity_rank ASC
        """,
    )
    abstract fun observeStations(): Flow<List<StationWithFavorite>>

    @Query(
        """
        SELECT s.*, 1 AS is_favorite
        FROM stations s
        INNER JOIN favorites f ON f.station_uuid = s.station_uuid
        ORDER BY f.added_at DESC
        """,
    )
    abstract fun observeFavorites(): Flow<List<StationWithFavorite>>

    /** Estaciones escuchadas, de la más reciente a la más antigua. */
    @Query(
        """
        SELECT s.*, (f.station_uuid IS NOT NULL) AS is_favorite
        FROM recent_plays r
        INNER JOIN stations s ON s.station_uuid = r.station_uuid
        LEFT JOIN favorites f ON f.station_uuid = s.station_uuid
        ORDER BY r.played_at DESC
        LIMIT :limit
        """,
    )
    abstract fun observeRecent(limit: Int): Flow<List<StationWithFavorite>>

    @Query("SELECT * FROM stations WHERE station_uuid = :stationUuid LIMIT 1")
    abstract suspend fun getStation(stationUuid: String): StationEntity?

    @Query("SELECT COUNT(*) FROM stations")
    abstract suspend fun count(): Int

    @Query("SELECT MAX(synced_at) FROM stations")
    abstract suspend fun lastSyncedAt(): Long?

    @Upsert
    abstract suspend fun upsertAll(stations: List<StationEntity>)

    /** Borra estaciones de sincronizaciones anteriores, salvo las marcadas como favoritas. */
    @Query(
        """
        DELETE FROM stations
        WHERE synced_at < :syncedAt
          AND station_uuid NOT IN (SELECT station_uuid FROM favorites)
        """,
    )
    abstract suspend fun deleteStale(syncedAt: Long)

    /**
     * Reemplaza la caché de forma atómica. Todas las filas de [stations] deben
     * traer el mismo `syncedAt`, que marca qué filas son de esta sincronización.
     */
    @Transaction
    open suspend fun replaceCache(stations: List<StationEntity>) {
        if (stations.isEmpty()) return
        upsertAll(stations)
        deleteStale(stations.first().syncedAt)
    }
}
