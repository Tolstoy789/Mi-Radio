package cl.miradio.app.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NowPlayingParserTest {

    private val station = "Radio Bío-Bío"

    @Test
    fun `separa artista y titulo`() {
        assertEquals(
            NowPlaying(title = "Gracias a la vida", artist = "Violeta Parra"),
            NowPlayingParser.parse("Violeta Parra - Gracias a la vida", station),
        )
    }

    @Test
    fun `respeta guiones dentro del titulo`() {
        assertEquals(
            NowPlaying(title = "Tren al sur - En vivo", artist = "Los Prisioneros"),
            NowPlayingParser.parse("Los Prisioneros – Tren al sur - En vivo", station),
        )
        assertEquals(NowPlaying(title = "Ojos-de-gato"), NowPlayingParser.parse("Ojos-de-gato", station))
    }

    @Test
    fun `sin separador queda como titulo`() {
        assertEquals(NowPlaying(title = "Expreso Bío-Bío"), NowPlayingParser.parse("  Expreso   Bío-Bío ", station))
    }

    @Test
    fun `descarta relleno, urls y el nombre de la radio`() {
        listOf(null, "", "  ", "-", " - ", "Unknown", "unknown - unknown", "https://biobiochile.cl",
            "RADIO BIO BIO", "radio bio-bio").forEach {
            assertNull("Debió descartar: $it", NowPlayingParser.parse(it, station))
        }
    }

    @Test
    fun `quita la radio cuando aparece como artista`() {
        assertEquals(NowPlaying(title = "Noticias"), NowPlayingParser.parse("Radio Bio Bio - Noticias", station))
    }

    @Test
    fun `combina titulo y artista de ID3`() {
        assertEquals(
            NowPlaying(title = "Amor violento", artist = "Los Tres"),
            NowPlayingParser.fromParts("Amor violento", "Los Tres", station),
        )
        assertEquals(NowPlaying(title = "Los Tres"), NowPlayingParser.fromParts(null, "Los Tres", station))
        assertNull(NowPlayingParser.fromParts("", station, station))
    }

    @Test
    fun `display une artista y titulo`() {
        assertEquals("Los Tres · Amor violento", NowPlaying("Amor violento", "Los Tres").display)
        assertEquals("Noticias", NowPlaying("Noticias").display)
    }
}
