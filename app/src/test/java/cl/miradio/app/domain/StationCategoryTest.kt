package cl.miradio.app.domain

import cl.miradio.app.domain.model.Station
import cl.miradio.app.domain.model.StationCategory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationCategoryTest {

    private fun station(vararg tags: String) = Station(
        id = "id", name = "Radio", streamUrl = "http://x", faviconUrl = null, homepage = null,
        tags = tags.toList(), frequency = null, state = null, codec = null, bitrate = 0,
        isHls = false, votes = 0, isFavorite = false,
    )

    @Test
    fun `Todas incluye cualquier estacion, incluso sin tags`() {
        assertTrue(StationCategory.ALL.matches(station()))
    }

    @Test
    fun `cada categoria reconoce sus tags y variantes compuestas`() {
        assertTrue(StationCategory.NEWS.matches(station("noticias")))
        assertTrue(StationCategory.NEWS.matches(station("news talk")))
        assertTrue(StationCategory.POP.matches(station("latin pop")))
        assertTrue(StationCategory.ROCK.matches(station("classic rock", "80s")))
    }

    @Test
    fun `no mezcla categorias`() {
        assertFalse(StationCategory.ROCK.matches(station("noticias", "deportes")))
        assertFalse(StationCategory.NEWS.matches(station("pop", "hits")))
        assertFalse(StationCategory.POP.matches(station()))
    }
}
