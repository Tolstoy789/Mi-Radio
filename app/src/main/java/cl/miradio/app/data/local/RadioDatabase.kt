package cl.miradio.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import cl.miradio.app.data.local.dao.FavoriteDao
import cl.miradio.app.data.local.dao.StationDao
import cl.miradio.app.data.local.entity.FavoriteEntity
import cl.miradio.app.data.local.entity.StationEntity

@Database(
    entities = [StationEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class RadioDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val NAME = "radio_chile.db"
    }
}
