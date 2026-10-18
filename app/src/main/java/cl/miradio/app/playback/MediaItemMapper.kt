package cl.miradio.app.playback

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import cl.miradio.app.domain.model.Station

private const val EXTRA_FREQUENCY = "frequency"
private const val EXTRA_SUBTITLE = "subtitle"
private const val EXTRA_NOW_PLAYING_TITLE = "now_playing_title"
private const val EXTRA_NOW_PLAYING_ARTIST = "now_playing_artist"

/**
 * Construye el [MediaItem] de una estación. El URI va también en `requestMetadata`
 * porque Media3 no envía `localConfiguration` del controlador a la sesión; así
 * [PlaybackService] puede reconstruirlo aunque la estación no esté en caché.
 */
fun Station.toMediaItem(): MediaItem {
    val uri = streamUrl.toUri()
    val subtitle = frequency ?: tags.firstOrNull()?.replaceFirstChar(Char::titlecase)
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setDisplayTitle(name)
        .setStation(name)
        .setArtist(subtitle)
        .setArtworkUri(faviconUrl?.toUri())
        .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .setExtras(
            Bundle().apply {
                putString(EXTRA_FREQUENCY, frequency)
                putString(EXTRA_SUBTITLE, subtitle)
            },
        )
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

/** Nombre de la radio, aunque el título ya muestre la canción en curso. */
val MediaMetadata.stationName: String
    get() = (station ?: title)?.toString().orEmpty()

/** Lo que suena ahora según el stream, o `null` si la radio no lo informa. */
val MediaMetadata.nowPlaying: NowPlaying?
    get() {
        val title = extras?.getString(EXTRA_NOW_PLAYING_TITLE) ?: return null
        return NowPlaying(title = title, artist = extras?.getString(EXTRA_NOW_PLAYING_ARTIST))
    }

/**
 * Copia del ítem con la canción en curso: la notificación muestra la canción como
 * título y al artista (o la radio) debajo. Con `null` vuelve a mostrar solo la radio.
 */
fun MediaItem.withNowPlaying(nowPlaying: NowPlaying?): MediaItem {
    val current = mediaMetadata
    val station = current.stationName
    val extras = Bundle(current.extras ?: Bundle.EMPTY).apply {
        if (nowPlaying == null) {
            remove(EXTRA_NOW_PLAYING_TITLE)
            remove(EXTRA_NOW_PLAYING_ARTIST)
        } else {
            putString(EXTRA_NOW_PLAYING_TITLE, nowPlaying.title)
            putString(EXTRA_NOW_PLAYING_ARTIST, nowPlaying.artist)
        }
    }
    val title = nowPlaying?.title ?: station
    val artist = when {
        nowPlaying == null -> extras.getString(EXTRA_SUBTITLE)
        nowPlaying.artist != null -> "${nowPlaying.artist} · $station"
        else -> station
    }
    val metadata = current.buildUpon()
        .setTitle(title)
        .setDisplayTitle(title)
        .setArtist(artist)
        .setExtras(extras)
        .build()
    return buildUpon().setMediaMetadata(metadata).build()
}
