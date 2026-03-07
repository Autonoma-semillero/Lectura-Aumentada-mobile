package co.edu.uniautonoma.inclusivereadingar.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import co.edu.uniautonoma.inclusivereadingar.presentation.navigation.AppNavHost

@Composable
fun InclusiveReadingArApp() {
    Surface(modifier = Modifier.fillMaxSize()) {
        AppNavHost()
    }
}
