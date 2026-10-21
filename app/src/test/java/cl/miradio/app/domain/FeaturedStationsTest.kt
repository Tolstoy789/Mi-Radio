package cl.miradio.app.domain

import cl.miradio.app.domain.model.FeaturedStations
import cl.miradio.app.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeaturedStationsTest {

    private fun station(id: String, votes: Int, unavailable: Boolean = false) = Station(
        id = id, name = "Radio $id", streamUrl = "http://$id", faviconUrl = null, homepage = null,
        tags = emptyList(), frequency = null, state = null, codec = null, bitrate = 0,
        isHls = false, votes = votes, isFavorite = false, isUnavailable = unavailable,
    )

    @Test
    fun `elige las mas votadas que estan transmitiendo`() {
        val stations = listOf(
            station("a", votes = 10),
            station("b", votes = 500, unavailable = true),
            station("c", votes = 300),
            station("d", votes = 50),
            station("e", votes = 120),
        )
        assertEquals(listOf("c", "e", "d"), FeaturedStations.pick(stations, count = 3).map { it.id })
    }

    @Test
    fun `con muy pocas radios no muestra el carrusel`() {
        val stations = listOf(station("a", votes = 10), station("b", votes = 5))
        assertTrue(FeaturedStations.pick(stations).isEmpty())
    }
}
