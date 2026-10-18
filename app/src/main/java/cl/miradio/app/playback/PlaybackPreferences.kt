package cl.miradio.app.playback

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Recuerda la última estación para retomarla desde los botones multimedia del sistema. */
@Singleton
class PlaybackPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("playback", Context.MODE_PRIVATE)

    var lastStationId: String?
        get() = prefs.getString(KEY_LAST_STATION_ID, null)
        set(value) = prefs.edit { putString(KEY_LAST_STATION_ID, value) }

    private companion object {
        const val KEY_LAST_STATION_ID = "last_station_id"
    }
}
