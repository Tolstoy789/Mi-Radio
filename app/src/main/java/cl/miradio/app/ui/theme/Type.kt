package cl.miradio.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cl.miradio.app.R

/** Títulos y marca. */
val Unbounded = FontFamily(
    Font(R.font.unbounded_semibold, FontWeight.SemiBold),
    Font(R.font.unbounded_bold, FontWeight.Bold),
)

/** Texto de interfaz. */
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

val RadioTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = Unbounded, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontFamily = Unbounded, fontWeight = FontWeight.Bold, fontSize = 22.sp),
        titleLarge = titleLarge.copy(fontFamily = Unbounded, fontWeight = FontWeight.Bold, fontSize = 18.sp),
        titleMedium = titleMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
        titleSmall = titleSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
        bodyLarge = bodyLarge.copy(fontFamily = Manrope),
        bodyMedium = bodyMedium.copy(fontFamily = Manrope),
        bodySmall = bodySmall.copy(fontFamily = Manrope),
        labelLarge = labelLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(
            fontFamily = Manrope,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        ),
    )
}
