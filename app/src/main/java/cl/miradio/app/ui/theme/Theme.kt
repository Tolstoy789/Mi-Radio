package cl.miradio.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RadioColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = TextPrimary,
    primaryContainer = Accent.copy(alpha = 0.18f),
    onPrimaryContainer = Accent,
    secondary = Accent,
    onSecondary = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = Background,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Background,
    surfaceContainerLow = CardSurface,
    surfaceContainer = Background,
    surfaceContainerHigh = CardSurface,
    surfaceContainerHighest = MiniPlayerSurface,
    outline = CardBorder,
    outlineVariant = CardBorder,
    error = LiveRed,
)

/** La app es siempre oscura, independiente del tema del sistema. */
@Composable
fun MiRadioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RadioColorScheme,
        typography = RadioTypography,
        content = content,
    )
}
