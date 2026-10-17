package co.edu.uniautonoma.inclusivereadingar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import co.edu.uniautonoma.inclusivereadingar.presentation.mascot.MascotPlaygroundRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.theme.InclusiveReadingArTheme

/**
 * Debug-only entry to the mascot playground that skips login:
 * `adb shell am start -n co.edu.uniautonoma.inclusivereadingar/.MascotPlaygroundActivity`
 */
class MascotPlaygroundActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InclusiveReadingArTheme(darkTheme = false) {
                MascotPlaygroundRoute(onBackClick = ::finish)
            }
        }
    }
}
