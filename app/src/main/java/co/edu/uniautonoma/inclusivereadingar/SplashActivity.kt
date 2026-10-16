package co.edu.uniautonoma.inclusivereadingar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import co.edu.uniautonoma.inclusivereadingar.presentation.splash.InclusiveSplashScreen
import co.edu.uniautonoma.inclusivereadingar.presentation.theme.InclusiveReadingArTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView)
            .hide(WindowInsetsCompat.Type.ime())
        setContent {
            InclusiveReadingArTheme(darkTheme = false) {
                InclusiveSplashScreen()
            }
        }

        lifecycleScope.launch {
            delay(MIN_SPLASH_DURATION_MS)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }

    companion object {
        private const val MIN_SPLASH_DURATION_MS = 550L
    }
}
