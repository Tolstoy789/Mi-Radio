package cl.miradio.app.domain

import cl.miradio.app.domain.model.DayPeriod
import org.junit.Assert.assertEquals
import org.junit.Test

class DayPeriodTest {

    @Test
    fun `saluda segun la hora local`() {
        assertEquals(DayPeriod.NIGHT, DayPeriod.of(0))
        assertEquals(DayPeriod.NIGHT, DayPeriod.of(4))
        assertEquals(DayPeriod.MORNING, DayPeriod.of(5))
        assertEquals(DayPeriod.MORNING, DayPeriod.of(11))
        assertEquals(DayPeriod.AFTERNOON, DayPeriod.of(12))
        assertEquals(DayPeriod.AFTERNOON, DayPeriod.of(19))
        assertEquals(DayPeriod.NIGHT, DayPeriod.of(20))
        assertEquals(DayPeriod.NIGHT, DayPeriod.of(23))
    }
}
