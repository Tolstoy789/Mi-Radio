package cl.miradio.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import cl.miradio.app.data.local.entity.RecentPlayEntity

@Dao
abstract class RecentPlayDao {

    @Upsert
    abstract suspend fun upsert(play: RecentPlayEntity)

    /** Deja solo las [keep] estaciones escuchadas más recientemente. */
    @Query(
        """
        DELETE FROM recent_plays
        WHERE station_uuid NOT IN (
            SELECT station_uuid FROM recent_plays ORDER BY played_at DESC LIMIT :keep
        )
        """,
    )
    abstract suspend fun trim(keep: Int)

    @Query("DELETE FROM recent_plays")
    abstract suspend fun clear()

    @Transaction
    open suspend fun record(play: RecentPlayEntity, keep: Int) {
        upsert(play)
        trim(keep)
    }
}
