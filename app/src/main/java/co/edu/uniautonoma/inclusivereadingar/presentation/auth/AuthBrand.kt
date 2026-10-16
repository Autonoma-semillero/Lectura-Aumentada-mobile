package co.edu.uniautonoma.inclusivereadingar.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.edu.uniautonoma.inclusivereadingar.R

/** The launcher artwork is the single source of truth for authentication branding. */
@Composable
fun LaunchBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 128.dp
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .semantics { contentDescription = "Logo de Lectura Inclusiva" }
            )
        }
    }
}
