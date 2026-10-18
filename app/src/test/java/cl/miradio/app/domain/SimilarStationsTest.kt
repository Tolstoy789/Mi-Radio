package cl.miradio.app.domain

import cl.miradio.app.domain.model.SimilarStations
import cl.miradio.app.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SimilarStationsTest {

    private fun station(
        id: String,
        vararg tags: String,
        state: String? = null,
        votes: Int = 0,
        unavailable: Boolean = false,
    ) = Station(
        id = id, name = "Radio $id", streamUrl = "http://$id", faviconUrl = null, homepage = null,
        tags = tags.toList(), frequency = null, state = state, codec = null, bitrate = 0,
        isHls = false, votes = votes, isFavorite = false, isUnavailable = unavailable,
    )

    @Test
    fun `prefiere misma categoria y region`() {
        val target = station("t", "noticias", state = "Concepción")
        val candidates = listOf(
            station("santiago", "noticias", state = "Santiago", votes = 900),
            station("conce", "noticias", state = "Talcahuano", votes = 10),
            station("rock", "rock", state = "Concepción", votes = 500),
        )
        assertEquals("conce", SimilarStations.pick(target, candidates)?.id)
    }

    @Test
    fun `desempata por votos y descarta caidas y la misma radio`() {
        val target = station("t", "pop")
        val candidates = listOf(
            target,
            station("a", "pop", votes = 10),
            station("b", "pop", votes = 50, unavailable = true),
            station("c", "pop", votes = 30),
        )
        assertEquals("c", SimilarStations.pick(target, candidates)?.id)
    }

    @Test
    fun `sin nada en comun no sugiere nada`() {
        assertNull(SimilarStations.pick(station("t", "jazz"), listOf(station("a", "cumbia"))))
    }
}
