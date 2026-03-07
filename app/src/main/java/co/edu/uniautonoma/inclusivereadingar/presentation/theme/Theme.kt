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
    secondary = PrimaryRedDark,
    tertiary = AccentRose,
    background = BackgroundLight,
    surface = SurfaceLight,
    onSurface = Color(0xFF2D2222)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryRed,
    onPrimary = Color.White,
    secondary = AccentRose,
    background = BackgroundDark,
    surface = SurfaceDark,
    onSurface = Color(0xFFFFEDED)
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
