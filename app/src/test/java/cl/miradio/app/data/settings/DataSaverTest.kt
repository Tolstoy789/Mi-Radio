package cl.miradio.app.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataSaverTest {

    @Test
    fun `el modo se activa segun la red`() {
        assertFalse(DataSaverMode.OFF.isActive(isMetered = true))
        assertTrue(DataSaverMode.MOBILE_ONLY.isActive(isMetered = true))
        assertFalse(DataSaverMode.MOBILE_ONLY.isActive(isMetered = false))
        assertTrue(DataSaverMode.ALWAYS.isActive(isMetered = false))
    }

    @Test
    fun `acumula consumo por red y reinicia al cambiar de mes`() {
        val october = DataUsage("2026-10")
            .add("2026-10", 1_000, isMetered = true)
            .add("2026-10", 500, isMetered = false)
            .add("2026-10", 250, isMetered = true)
        assertEquals(DataUsage("2026-10", mobileBytes = 1_250, wifiBytes = 500), october)

        assertEquals(DataUsage("2026-11", mobileBytes = 0, wifiBytes = 100), october.add("2026-11", 100, isMetered = false))
        assertEquals(DataUsage("2026-11"), october.forMonth("2026-11"))
    }

    @Test
    fun `estima megabytes por hora segun el bitrate`() {
        assertEquals(57, megabytesPerHour(128))
        assertEquals(28, megabytesPerHour(64))
    }
}
