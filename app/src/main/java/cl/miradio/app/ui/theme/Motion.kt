package cl.miradio.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/** Resortes compartidos, para que toda la app se mueva con el mismo carácter. */
object Motion {
    /** Respuesta al presionar: rápida y sin rebote. */
    val press = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    /** Cambios que el usuario provoca (favorita, pausa): con un rebote breve. */
    val bouncy = spring<Float>(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)

    /** Elementos grandes que se acomodan (portada, tarjetas que entran). */
    val gentle = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
}
