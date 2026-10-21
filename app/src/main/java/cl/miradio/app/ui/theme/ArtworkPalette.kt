package cl.miradio.app.ui.theme

/**
 * Elige el color de un logo para teñir el reproductor y las tarjetas destacadas.
 * Toma el tono más presente entre los píxeles con color e ignora blancos, negros,
 * grises y transparencias, porque muchos logos de radios son letras sobre fondo blanco.
 * Trabaja con enteros ARGB para no depender de Android y poder probarlo en la JVM.
 */
object ArtworkPalette {

    private const val HUE_BUCKETS = 24

    /** Diferencia mínima entre canales para considerar que un píxel tiene color. */
    private const val MIN_CHROMA = 0.18f

    /** Fracción mínima de la imagen (ponderada por intensidad) que debe ocupar el color. */
    private const val MIN_COVERAGE = 0.03f

    /** Color ARGB representativo de [pixels], o `null` si la imagen es gris o casi transparente. */
    fun dominantColor(pixels: IntArray): Int? {
        val weights = FloatArray(HUE_BUCKETS)
        val reds = FloatArray(HUE_BUCKETS)
        val greens = FloatArray(HUE_BUCKETS)
        val blues = FloatArray(HUE_BUCKETS)
        var opaque = 0

        for (argb in pixels) {
            if (argb ushr 24 < 128) continue
            opaque++
            val r = (argb shr 16 and 0xFF) / 255f
            val g = (argb shr 8 and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            val max = maxOf(r, g, b)
            val chroma = max - minOf(r, g, b)
            if (chroma < MIN_CHROMA) continue

            val bucket = (hue(r, g, b, max, chroma) / 360f * HUE_BUCKETS).toInt().coerceIn(0, HUE_BUCKETS - 1)
            // Los colores más vivos pesan más que los pasteles.
            weights[bucket] += chroma
            reds[bucket] += r * chroma
            greens[bucket] += g * chroma
            blues[bucket] += b * chroma
        }
        if (opaque == 0) return null

        val best = weights.indices.maxBy { weights[it] }
        val weight = weights[best]
        if (weight / opaque < MIN_COVERAGE) return null
        return argb(reds[best] / weight, greens[best] / weight, blues[best] / weight)
    }

    /**
     * Ajusta [argb] para usarlo de fondo bajo texto blanco: ni tan oscuro que se pierda
     * en el azul noche ni tan claro que el texto deje de leerse.
     */
    fun forDarkTheme(argb: Int): Int {
        val (hue, saturation, value) = toHsv(argb)
        return fromHsv(hue, saturation.coerceIn(0.35f, 0.85f), value.coerceIn(0.45f, 0.72f))
    }

    private fun hue(r: Float, g: Float, b: Float, max: Float, chroma: Float): Float {
        val sector = when (max) {
            r -> ((g - b) / chroma).mod(6f)
            g -> (b - r) / chroma + 2f
            else -> (r - g) / chroma + 4f
        }
        return sector * 60f
    }

    internal fun toHsv(argb: Int): Triple<Float, Float, Float> {
        val r = (argb shr 16 and 0xFF) / 255f
        val g = (argb shr 8 and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val chroma = max - minOf(r, g, b)
        val hue = if (chroma == 0f) 0f else hue(r, g, b, max, chroma)
        val saturation = if (max == 0f) 0f else chroma / max
        return Triple(hue, saturation, max)
    }

    private fun fromHsv(hue: Float, saturation: Float, value: Float): Int {
        val chroma = value * saturation
        val x = chroma * (1f - kotlin.math.abs((hue / 60f).mod(2f) - 1f))
        val m = value - chroma
        val (r, g, b) = when ((hue / 60f).toInt().coerceIn(0, 5)) {
            0 -> Triple(chroma, x, 0f)
            1 -> Triple(x, chroma, 0f)
            2 -> Triple(0f, chroma, x)
            3 -> Triple(0f, x, chroma)
            4 -> Triple(x, 0f, chroma)
            else -> Triple(chroma, 0f, x)
        }
        return argb(r + m, g + m, b + m)
    }

    private fun argb(r: Float, g: Float, b: Float): Int {
        fun channel(value: Float) = (value * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
    }
}
