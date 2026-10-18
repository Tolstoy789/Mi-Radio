package cl.miradio.app.playback

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import cl.miradio.app.domain.model.Station

private const val EXTRA_FREQUENCY = "frequency"

/**
 * Construye el [MediaItem] de una estación. El URI va también en `requestMetadata`
 * porque Media3 no envía `localConfiguration` del controlador a la sesión; así
 * [PlaybackService] puede reconstruirlo aunque la estación no esté en caché.
 */
fun Station.toMediaItem(): MediaItem {
    val uri = streamUrl.toUri()
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setDisplayTitle(name)
        .setStation(name)
        .setArtist(frequency ?: tags.firstOrNull()?.replaceFirstChar(Char::titlecase))
        .setArtworkUri(faviconUrl?.toUri())
        .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .setExtras(Bundle().apply { putString(EXTRA_FREQUENCY, frequency) })
        .build()

    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .apply { if (isHls) setMimeType(MimeTypes.APPLICATION_M3U8) }
        .setMediaMetadata(metadata)
        .setRequestMetadata(
            MediaItem.RequestMetadata.Builder().setMediaUri(uri).build(),
        )
        .build()
}

/** Dial guardado en los extras del [MediaItem], para mostrarlo en la UI. */
val MediaMetadata.frequency: String?
    get() = extras?.getString(EXTRA_FREQUENCY)
