package co.edu.uniautonoma.inclusivereadingar.presentation.student

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.config.BackendConfig
import co.edu.uniautonoma.inclusivereadingar.domain.model.DomanSessionCard
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.DomanSessionUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.DomanSessionViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.DomanSessionViewModelFactory
import kotlinx.coroutines.delay

@Composable
fun DomanSessionRoute(
    categoryId: String,
    categoryName: String,
    onBackClick: () -> Unit,
    onSessionCompleted: (Int) -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    var backendBaseUrl by remember { mutableStateOf(BackendConfig.DEFAULT_REMOTE_BASE_URL) }
    val viewModel: DomanSessionViewModel = viewModel(
        factory = DomanSessionViewModelFactory(container.domanRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        backendBaseUrl = container.sessionStore.resolveBackendBaseUrl()
    }

    LaunchedEffect(categoryId, categoryName) {
        viewModel.resumeOrStart(categoryId, categoryName)
    }

    LaunchedEffect(uiState.isCompleted, uiState.session?.cards?.size) {
        if (uiState.isCompleted) {
            onSessionCompleted(uiState.session?.cards?.size ?: 0)
        }
    }

    DomanSessionScreen(
        categoryName = categoryName,
        uiState = uiState,
        backendBaseUrl = backendBaseUrl,
        onBackClick = onBackClick,
        onTogglePause = viewModel::togglePause,
        onAdvance = viewModel::advance,
        onSkip = viewModel::skip,
        onPlayAudio = viewModel::registerAudioPlayed,
        onDismissError = viewModel::clearError
    )
}

@Composable
fun DomanSessionScreen(
    categoryName: String,
    uiState: DomanSessionUiState,
    backendBaseUrl: String,
    onBackClick: () -> Unit,
    onTogglePause: () -> Unit,
    onAdvance: () -> Unit,
    onSkip: () -> Unit,
    onPlayAudio: () -> Unit,
    onDismissError: () -> Unit
) {
    val session = uiState.session
    var remainingMillis by remember(uiState.currentIndex, session?.sessionId) {
        mutableIntStateOf(session?.displayMs ?: 0)
    }
    var progress by remember(uiState.currentIndex, session?.sessionId) {
        mutableFloatStateOf(1f)
    }

    LaunchedEffect(uiState.currentIndex, uiState.isPaused, session?.sessionId) {
        if (session == null || uiState.isPaused || uiState.isCompleted) {
            return@LaunchedEffect
        }
        remainingMillis = session.displayMs
        progress = 1f
        while (remainingMillis > 0 && !uiState.isPaused) {
            delay(100)
            remainingMillis = (remainingMillis - 100).coerceAtLeast(0)
            progress = remainingMillis.toFloat() / session.displayMs.toFloat()
        }
        if (!uiState.isPaused && remainingMillis == 0) {
            onAdvance()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F7))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color(0xFFE53734)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Sesión del día", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                Text(text = categoryName, color = Color(0xFF64748B))
            }
            IconButton(onClick = onTogglePause) {
                Icon(
                    imageVector = if (uiState.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = if (uiState.isPaused) "Reanudar" else "Pausar",
                    tint = Color(0xFFE53734)
                )
            }
        }

        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE53734))
            }

            !uiState.errorMessage.isNullOrBlank() -> Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        text = uiState.errorMessage,
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                LaunchedEffect(Unit) { onDismissError() }
            }

            session == null || uiState.currentCard == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No hay una sesión disponible.", color = Color(0xFF64748B))
            }

            else -> {
                val currentCard = requireNotNull(uiState.currentCard)
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Tarjeta ${uiState.currentIndex + 1} de ${session.cards.size}",
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFE53734),
                            trackColor = Color(0xFFF4C7C7)
                        )
                        Text(
                            text = if (uiState.isPaused) "Pausado" else "${(remainingMillis / 1000f).coerceAtLeast(0f)} s",
                            color = Color(0xFFE53734),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    WordDisplayCard(
                        card = currentCard,
                        backendBaseUrl = backendBaseUrl,
                        onPlayAudio = onPlayAudio,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onAdvance,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE53734),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = if (uiState.currentIndex == session.cards.lastIndex) "Finalizar sesión" else "Siguiente palabra",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Button(
                            onClick = onSkip,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFFE53734)
                            )
                        ) {
                            Text(text = "Omitir tarjeta", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordDisplayCard(
    card: DomanSessionCard,
    backendBaseUrl: String,
    onPlayAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(vertical = 24.dp),
        shape = RoundedCornerShape(36.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = card.word.uppercase(),
                color = Color(0xFFE53734),
                fontSize = 68.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            if (!card.audioUrl.isNullOrBlank()) {
                AudioPlayButton(
                    audioUrl = card.audioUrl,
                    backendBaseUrl = backendBaseUrl,
                    onPlayAudio = onPlayAudio,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AudioPlayButton(
    audioUrl: String,
    backendBaseUrl: String,
    onPlayAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember { MediaPlayer() }

    DisposableEffect(audioUrl) {
        onDispose { player.release() }
    }

    val resolvedUrl = resolvePlaybackUrl(audioUrl, backendBaseUrl)

    IconButton(
        onClick = {
            runCatching {
                onPlayAudio()
                player.reset()
                player.setDataSource(context, Uri.parse(resolvedUrl))
                player.setOnPreparedListener { it.start() }
                player.prepareAsync()
            }
        },
        modifier = modifier.size(56.dp).background(Color(0xFFE53734).copy(alpha = 0.12f), CircleShape)
    ) {
        Icon(Icons.Rounded.VolumeUp, contentDescription = "Escuchar palabra", tint = Color(0xFFE53734))
    }
}

private fun resolvePlaybackUrl(audioUrl: String, backendBaseUrl: String): String {
    val value = audioUrl.trim()
    val base = backendBaseUrl.removeSuffix("/")
    return when {
        value.startsWith("http://") || value.startsWith("https://") -> value
        value.startsWith("/") -> "$base$value"
        else -> "$base/$value"
    }
}




