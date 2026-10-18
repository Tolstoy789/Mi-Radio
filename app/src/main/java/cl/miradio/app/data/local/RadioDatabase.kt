package cl.miradio.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.miradio.app.data.local.dao.FavoriteDao
import cl.miradio.app.data.local.dao.RecentPlayDao
import cl.miradio.app.data.local.dao.StationDao
import cl.miradio.app.data.local.dao.UnavailableStationDao
import cl.miradio.app.data.local.entity.FavoriteEntity
import cl.miradio.app.data.local.entity.RecentPlayEntity
import cl.miradio.app.data.local.entity.StationEntity
import cl.miradio.app.data.local.entity.UnavailableStationEntity

@Database(
    entities = [
        StationEntity::class,
        FavoriteEntity::class,
        RecentPlayEntity::class,
        UnavailableStationEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class RadioDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentPlayDao(): RecentPlayDao
    abstract fun unavailableStationDao(): UnavailableStationDao

    companion object {
        const val NAME = "radio_chile.db"

        /** v2: historial de escucha. Conserva caché y favoritos. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recent_plays` (" +
                        "`station_uuid` TEXT NOT NULL, `played_at` INTEGER NOT NULL, PRIMARY KEY(`station_uuid`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recent_plays_played_at` ON `recent_plays` (`played_at`)")
            }
        }

        /** v3: radios que no están transmitiendo. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `unavailable_stations` (" +
                        "`station_uuid` TEXT NOT NULL, `failed_at` INTEGER NOT NULL, PRIMARY KEY(`station_uuid`))",
                )
            }
        }
    }
}
