package cl.miradio.app.data.mapper

import cl.miradio.app.data.remote.model.StationDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StationMappersTest {

    private fun dto(
        uuid: String,
        name: String = "Radio $uuid",
        url: String = "http://stream.example.cl/$uuid",
        urlResolved: String = "",
        favicon: String = "",
        tags: String = "",
        lastCheckOk: Int = 1,
    ) = StationDto(
        stationUuid = uuid,
        name = name,
        url = url,
        urlResolved = urlResolved,
        favicon = favicon,
        tags = tags,
        lastCheckOk = lastCheckOk,
    )

    @Test
    fun `prefiere url_resolved y cae a url si viene vacia`() {
        val entities = listOf(
            dto("a", urlResolved = "https://resolved.cl/a"),
            dto("b", urlResolved = "  "),
        ).toEntities(syncedAt = 10)

        assertEquals("https://resolved.cl/a", entities[0].streamUrl)
        assertEquals("http://stream.example.cl/b", entities[1].streamUrl)
    }

    @Test
    fun `descarta estaciones sin stream, caidas o duplicadas y conserva el orden`() {
        val entities = listOf(
            dto("a"),
            dto("b", url = ""),
            dto("c", lastCheckOk = 0),
            dto("a", name = "Duplicada"),
            dto("d"),
        ).toEntities(syncedAt = 10)

        assertEquals(listOf("a", "d"), entities.map { it.stationUuid })
        assertEquals(listOf(0, 1), entities.map { it.popularityRank })
        assertTrue(entities.all { it.syncedAt == 10L })
    }

    @Test
    fun `normaliza nombre, tags y favicon`() {
        val entity = listOf(
            dto("a", name = "  Radio   Bío Bío ", tags = "News, NOTICIAS,,  Chile ", favicon = "data:image/png"),
        ).toEntities(syncedAt = 1).single()

        assertEquals("Radio Bío Bío", entity.name)
        assertEquals("news,noticias,chile", entity.tags)
        assertNull(entity.faviconUrl)
    }

    @Test
    fun `extrae el dial FM o AM`() {
        assertEquals("89.7 FM", extractFrequency("Radio Corazón 89.7"))
        assertEquals("101.3 FM", extractFrequency("Pudahuel 101,3 FM"))
        assertEquals("1180 AM", extractFrequency("Radio Agricultura 1180 AM"))
        assertNull(extractFrequency("Radio Online 24/7"))
        assertNull(extractFrequency("Versión 2.0"))
    }

    @Test
    fun `mapea a dominio con dial desde los tags y estado de favorito`() {
        val entity = listOf(dto("a", name = "Radio Cooperativa", tags = "noticias,93.3")).toEntities(1).single()

        val station = entity.toDomain(isFavorite = true)

        assertEquals("93.3 FM", station.frequency)
        assertEquals(listOf("noticias", "93.3"), station.tags)
        assertTrue(station.isFavorite)
        assertFalse(entity.toDomain().isFavorite)
    }
}
