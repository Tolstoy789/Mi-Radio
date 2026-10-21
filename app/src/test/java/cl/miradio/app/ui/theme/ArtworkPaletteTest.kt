package cl.miradio.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkPaletteTest {

    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()
    private val gray = 0xFF808080.toInt()
    private val red = 0xFFD32F2F.toInt()
    private val blue = 0xFF1E40C8.toInt()

    private fun image(vararg parts: Pair<Int, Int>): IntArray =
        parts.flatMap { (color, count) -> List(count) { color } }.toIntArray()

    private fun hueOf(argb: Int) = ArtworkPalette.toHsv(argb).first

    @Test
    fun `encuentra el color de un logo con letras sobre fondo blanco`() {
        val color = ArtworkPalette.dominantColor(image(white to 80, red to 20))
        assertNotNull(color)
        assertTrue(hueOf(color!!) < 15f || hueOf(color) > 345f)
    }

    @Test
    fun `gana el color que mas ocupa`() {
        val color = ArtworkPalette.dominantColor(image(red to 10, blue to 30, white to 60))
        assertEquals(blue, color)
    }

    @Test
    fun `los logos grises o transparentes no tienen color`() {
        assertNull(ArtworkPalette.dominantColor(image(white to 50, black to 30, gray to 20)))
        assertNull(ArtworkPalette.dominantColor(image(0x00FF0000 to 100)))
        assertNull(ArtworkPalette.dominantColor(IntArray(0)))
    }

    @Test
    fun `un punto de color en un logo gris no alcanza`() {
        assertNull(ArtworkPalette.dominantColor(image(white to 999, red to 1)))
    }

    @Test
    fun `ajusta el brillo para usarlo bajo texto blanco`() {
        val (_, _, darkValue) = ArtworkPalette.toHsv(ArtworkPalette.forDarkTheme(0xFF0A0F30.toInt()))
        val (_, _, neonValue) = ArtworkPalette.toHsv(ArtworkPalette.forDarkTheme(0xFFFFFF00.toInt()))
        assertTrue(darkValue >= 0.44f)
        assertTrue(neonValue <= 0.73f)
        // Mantiene el tono: el amarillo sigue siendo amarillo.
        assertEquals(60f, hueOf(ArtworkPalette.forDarkTheme(0xFFFFFF00.toInt())), 2f)
    }
}
