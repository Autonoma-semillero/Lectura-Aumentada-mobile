package co.edu.uniautonoma.inclusivereadingar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import co.edu.uniautonoma.inclusivereadingar.presentation.InclusiveReadingArApp
import co.edu.uniautonoma.inclusivereadingar.presentation.theme.InclusiveReadingArTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            InclusiveReadingArTheme(darkTheme = false) {
                InclusiveReadingArApp()
            }
        }
    }
}
