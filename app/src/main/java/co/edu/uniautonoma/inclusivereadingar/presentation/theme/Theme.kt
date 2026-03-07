package co.edu.uniautonoma.inclusivereadingar.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = OceanBlue,
    onPrimary = Color.White,
    secondary = WarmOrange,
    tertiary = SkyBlue,
    background = MistBackground,
    surface = SoftCream,
    onSurface = InkDark
)

private val DarkColorScheme = darkColorScheme(
    primary = SkyBlue,
    onPrimary = InkDark,
    secondary = WarmOrange,
    background = InkDark,
    surface = DeepBlue
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
