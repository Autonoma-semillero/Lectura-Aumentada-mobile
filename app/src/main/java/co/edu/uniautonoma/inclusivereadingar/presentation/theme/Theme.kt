package co.edu.uniautonoma.inclusivereadingar.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryRed,
    onPrimary = Color.White,
    primaryContainer = AccentRose,
    onPrimaryContainer = PrimaryRedDark,
    secondary = SecondaryBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E6FF),
    onSecondaryContainer = ChildInk,
    tertiary = InfoTeal,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC4F0EF),
    onTertiaryContainer = Color(0xFF0E4E51),
    error = ErrorRed,
    onError = Color.White,
    background = BackgroundLight,
    onBackground = ChildInk,
    surface = SurfaceLight,
    onSurface = ChildInk,
    onSurfaceVariant = ChildMuted,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFB4C5FF),
    onSecondary = Color(0xFF102C62),
    secondaryContainer = Color(0xFF29457F),
    onSecondaryContainer = Color(0xFFD9E2FF),
    tertiary = Color(0xFF8BD6D5),
    onTertiary = Color(0xFF003738),
    tertiaryContainer = Color(0xFF005052),
    onTertiaryContainer = Color(0xFFA6F2F0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = Color(0xFF93A4B8),
    outlineVariant = Color(0xFF475569)
)

@Composable
fun InclusiveReadingArTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
