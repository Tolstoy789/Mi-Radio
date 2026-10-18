package cl.miradio.app.domain

import cl.miradio.app.domain.model.ChileRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChileRegionTest {

    @Test
    fun `reconoce regiones y ciudades con o sin tildes`() {
        mapOf(
            "Región Metropolitana" to ChileRegion.METROPOLITANA,
            "Santiago" to ChileRegion.METROPOLITANA,
            "Bio-Bio" to ChileRegion.BIOBIO,
            "Concepcion" to ChileRegion.BIOBIO,
            "Valparaíso" to ChileRegion.VALPARAISO,
            "Viña del Mar" to ChileRegion.VALPARAISO,
            "O'Higgins" to ChileRegion.OHIGGINS,
            "Ñuble" to ChileRegion.NUBLE,
            "Chillán" to ChileRegion.NUBLE,
            "Region de Los Rios" to ChileRegion.LOS_RIOS,
            "Puerto Montt" to ChileRegion.LOS_LAGOS,
            "Punta Arenas" to ChileRegion.MAGALLANES,
            "San Pedro de Atacama" to ChileRegion.ANTOFAGASTA,
            "ARICA" to ChileRegion.ARICA,
        ).forEach { (state, region) -> assertEquals(state, region, ChileRegion.fromState(state)) }
    }

    @Test
    fun `devuelve null si no reconoce el lugar`() {
        listOf(null, "", "  ", "Chile", "Online", "Mendoza").forEach { assertNull(it, ChileRegion.fromState(it)) }
    }

    @Test
    fun `las regiones van de norte a sur`() {
        assertEquals(ChileRegion.ARICA, ChileRegion.entries.first())
        assertEquals(ChileRegion.MAGALLANES, ChileRegion.entries.last())
    }
}
