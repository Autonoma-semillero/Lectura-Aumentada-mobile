package co.edu.uniautonoma.inclusivereadingar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import co.edu.uniautonoma.inclusivereadingar.presentation.splash.InclusiveSplashScreen
import co.edu.uniautonoma.inclusivereadingar.presentation.theme.InclusiveReadingArTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashActivity : ComponentActivity() {

    private val _uiState = MutableStateFlow(SplashUiState())
    private val uiState = _uiState.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val state by uiState.collectAsState()
            InclusiveReadingArTheme(darkTheme = false) {
                InclusiveSplashScreen(
                    progress = state.progress,
                    loadingText = "${state.label} ${state.percent}%"
                )
            }
        }

        lifecycleScope.launch {
            runStartupProgress()
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }

    private suspend fun runStartupProgress() {
        var from = 0f
        for (phase in phases) {
            animatePhase(from = from, to = phase.target, durationMs = phase.durationMs, label = phase.label)
            from = phase.target
        }
        _uiState.value = _uiState.value.copy(progress = 1f, label = "Listo")
        delay(120)
    }

    private suspend fun animatePhase(from: Float, to: Float, durationMs: Long, label: String) {
        val steps = (durationMs / FRAME_MS).coerceAtLeast(1L).toInt()
        for (step in 1..steps) {
            val t = step.toFloat() / steps.toFloat()
            val progressValue = from + ((to - from) * t)
            _uiState.value = SplashUiState(progress = progressValue, label = label)
            delay(FRAME_MS)
        }
    }

    private data class SplashUiState(
        val progress: Float = 0f,
        val label: String = "Cargando..."
    ) {
        val percent: Int
            get() = (progress.coerceIn(0f, 1f) * 100f).toInt()
    }

    private data class SplashPhase(
        val label: String,
        val target: Float,
        val durationMs: Long
    )

    companion object {
        private const val FRAME_MS = 30L

        private val phases = listOf(
            SplashPhase(label = "Inicializando", target = 0.35f, durationMs = 700L),
            SplashPhase(label = "Preparando contenido", target = 0.75f, durationMs = 900L),
            SplashPhase(label = "Casi listo", target = 1.0f, durationMs = 700L)
        )
    }
}
