package co.edu.uniautonoma.inclusivereadingar.presentation.student

import android.media.MediaPlayer
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
import androidx.compose.material3.OutlinedButton
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
        onRetry = { viewModel.retry(categoryId, categoryName) }
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
    onRetry: () -> Unit
) {
    val session = uiState.session
    val protectedDisplayMs = session?.displayMs?.coerceAtLeast(1_000) ?: 0
    var remainingMillis by remember(uiState.currentIndex, session?.sessionId) {
        mutableIntStateOf(protectedDisplayMs)
    }
    var progress by remember(uiState.currentIndex, session?.sessionId) {
        mutableFloatStateOf(1f)
    }

    LaunchedEffect(uiState.currentIndex, uiState.canRunTimer, session?.sessionId) {
        if (session == null || !uiState.canRunTimer) {
            return@LaunchedEffect
        }
        remainingMillis = protectedDisplayMs
        progress = 1f
        while (remainingMillis > 0) {
            delay(100)
            remainingMillis = (remainingMillis - 100).coerceAtLeast(0)
            progress = remainingMillis.toFloat() / protectedDisplayMs.toFloat()
        }
        if (uiState.canRunTimer && remainingMillis == 0) {
            onAdvance()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Sesión del día", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                Text(text = categoryName, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onTogglePause) {
                Icon(
                    imageVector = if (uiState.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = if (uiState.isPaused) "Reanudar" else "Pausar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            !uiState.errorMessage.isNullOrBlank() -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        text = uiState.errorMessage,
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Button(
                    onClick = onRetry,
                    modifier = Modifier.padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = if (session == null) "Reintentar" else "Volver a la sesión",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            session == null || uiState.currentCard == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No hay una sesión disponible.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        Text(
                            text = if (uiState.isPaused) "Pausado" else "${kotlin.math.ceil(remainingMillis / 1000.0).toInt().coerceAtLeast(0)} s",
                            color = MaterialTheme.colorScheme.primary,
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
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = if (uiState.currentIndex == session.cards.lastIndex) "Finalizar sesión" else "Siguiente palabra",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        OutlinedButton(
                            onClick = onSkip,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(20.dp)
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
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = card.word.uppercase(),
                color = MaterialTheme.colorScheme.primary,
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
    val resolvedUrl = resolvePlaybackUrl(audioUrl, backendBaseUrl)
    val player = remember(resolvedUrl) { MediaPlayer() }
    var isPlaying by remember(resolvedUrl) { mutableStateOf(false) }
    var audioError by remember(resolvedUrl) { mutableStateOf(false) }

    DisposableEffect(resolvedUrl) {
        onDispose { player.release() }
    }

    // Auto-play al aparecer cada tarjeta
    LaunchedEffect(resolvedUrl) {
        runCatching {
            audioError = false
            onPlayAudio()
            player.reset()
            player.setDataSource(resolvedUrl)
            player.setOnErrorListener { _, _, _ -> isPlaying = false; audioError = true; true }
            player.setOnCompletionListener { isPlaying = false }
            player.setOnPreparedListener { it.start() }
            isPlaying = true
            player.prepareAsync()
        }.onFailure { isPlaying = false; audioError = true }
    }

    IconButton(
        onClick = {
            if (!isPlaying) {
                runCatching {
                    audioError = false
                    onPlayAudio()
                    player.reset()
                    player.setDataSource(resolvedUrl)
                    player.setOnErrorListener { _, _, _ -> isPlaying = false; audioError = true; true }
                    player.setOnCompletionListener { isPlaying = false }
                    player.setOnPreparedListener { it.start() }
                    isPlaying = true
                    player.prepareAsync()
                }.onFailure { isPlaying = false; audioError = true }
            }
        },
        enabled = !isPlaying,
        modifier = modifier
            .size(56.dp)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = if (isPlaying) 0.05f else 0.12f),
                CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.Rounded.VolumeUp,
            contentDescription = "Escuchar palabra",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = if (isPlaying) 0.35f else 1f)
        )
    }
    if (audioError) {
        Text("No pudimos reproducir el audio. Toca el altavoz para reintentar.", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
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


