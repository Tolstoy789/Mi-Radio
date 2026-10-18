package cl.miradio.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cl.miradio.app.data.local.entity.UnavailableStationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnavailableStationDao {

    @Upsert
    suspend fun upsert(entity: UnavailableStationEntity)

    @Query("DELETE FROM unavailable_stations WHERE station_uuid = :stationUuid")
    suspend fun remove(stationUuid: String)

    /** IDs de radios que fallaron después de [since]. */
    @Query("SELECT station_uuid FROM unavailable_stations WHERE failed_at >= :since")
    fun observeSince(since: Long): Flow<List<String>>

    @Query("DELETE FROM unavailable_stations WHERE failed_at < :before")
    suspend fun deleteOlderThan(before: Long)
}
