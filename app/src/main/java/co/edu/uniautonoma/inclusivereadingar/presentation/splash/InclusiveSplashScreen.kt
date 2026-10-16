package co.edu.uniautonoma.inclusivereadingar.presentation.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.edu.uniautonoma.inclusivereadingar.R

@Composable
fun InclusiveSplashScreen() {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        IconDecoration(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 28.dp, top = 76.dp),
            icon = Icons.Rounded.Star
        )
        IconDecoration(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 32.dp, bottom = 96.dp),
            icon = Icons.AutoMirrored.Rounded.MenuBook
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(156.dp),
                shape = CircleShape,
                color = colors.surface,
                tonalElevation = 3.dp,
                shadowElevation = 3.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.padding(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Lectura Inclusiva",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Preparamos tu lectura",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                modifier = Modifier
                    .size(36.dp)
                    .semantics {
                        contentDescription = "Preparando la aplicación"
                        liveRegion = LiveRegionMode.Polite
                    },
                color = colors.secondary,
                trackColor = colors.secondaryContainer,
                strokeWidth = 4.dp
            )
        }
    }
}

@Composable
private fun IconDecoration(modifier: Modifier, icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.outlineVariant,
        modifier = modifier
            .size(38.dp)
            .alpha(0.34f)
    )
}
