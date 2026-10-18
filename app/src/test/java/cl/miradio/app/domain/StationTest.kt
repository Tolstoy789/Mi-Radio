package cl.miradio.app.domain

import cl.miradio.app.domain.model.Station
import cl.miradio.app.domain.model.dialAndPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationTest {

    private fun station(frequency: String?, state: String?) = Station(
        id = "id", name = "Radio", streamUrl = "http://x", faviconUrl = null, homepage = null,
        tags = emptyList(), frequency = frequency, state = state, codec = null, bitrate = 0,
        isHls = false, votes = 0, isFavorite = false,
    )

    @Test
    fun `combina dial y lugar con solo lo que se conoce`() {
        assertEquals("93.3 FM · Santiago", station("93.3 FM", "Santiago").dialAndPlace)
        assertEquals("93.3 FM", station("93.3 FM", null).dialAndPlace)
        assertEquals("Biobío", station(null, "Biobío").dialAndPlace)
        assertNull(station(null, " ").dialAndPlace)
    }
}
