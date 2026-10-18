package cl.miradio.app.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.extractor.metadata.id3.TextInformationFrame

/**
 * Lee "qué suena ahora" de los metadatos del stream (ICY en MP3/AAC, ID3 en HLS)
 * y lo escribe en el [MediaItem] actual. Así lo ven la UI, la notificación, la
 * pantalla de bloqueo y los dispositivos Bluetooth.
 *
 * ExoPlayer da prioridad a la metadata del [MediaItem] sobre la del stream, por eso
 * no basta con leer `player.mediaMetadata`: hay que reemplazar el ítem. Media3
 * actualiza el ítem sin volver a cargar el stream porque el URI no cambia.
 */
@OptIn(UnstableApi::class)
class NowPlayingTracker(private val player: Player) : Player.Listener {

    override fun onMetadata(metadata: Metadata) {
        val item = player.currentMediaItem ?: return
        // Metadata sin título (p. ej. solo cabeceras ICY) no cambia lo que suena.
        if (!metadata.hasTitleEntry()) return
        val nowPlaying = metadata.toNowPlaying(item.mediaMetadata.stationName)
        if (nowPlaying == item.mediaMetadata.nowPlaying) return
        val index = player.currentMediaItemIndex
        if (index == C.INDEX_UNSET) return
        player.replaceMediaItem(index, item.withNowPlaying(nowPlaying))
    }

    private fun Metadata.hasTitleEntry(): Boolean = (0 until length()).any { i ->
        when (val entry = get(i)) {
            is IcyInfo -> true
            is TextInformationFrame -> entry.id == "TIT2" || entry.id == "TPE1"
            else -> false
        }
    }

    private fun Metadata.toNowPlaying(stationName: String): NowPlaying? {
        var icyTitle: String? = null
        var id3Title: String? = null
        var id3Artist: String? = null
        for (i in 0 until length()) {
            when (val entry = get(i)) {
                is IcyInfo -> icyTitle = entry.title
                is TextInformationFrame -> when (entry.id) {
                    "TIT2" -> id3Title = entry.values.firstOrNull()
                    "TPE1" -> id3Artist = entry.values.firstOrNull()
                }
            }
        }
        return if (id3Title != null || id3Artist != null) {
            NowPlayingParser.fromParts(id3Title, id3Artist, stationName)
        } else {
            NowPlayingParser.parse(icyTitle, stationName)
        }
    }
}
