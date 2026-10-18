package cl.miradio.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.miradio.app.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE station_uuid = :stationUuid")
    suspend fun remove(stationUuid: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE station_uuid = :stationUuid)")
    suspend fun isFavorite(stationUuid: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE station_uuid = :stationUuid)")
    fun observeIsFavorite(stationUuid: String): Flow<Boolean>
}
