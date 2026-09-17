package co.edu.uniautonoma.inclusivereadingar.presentation.student

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.config.BackendConfig
import co.edu.uniautonoma.inclusivereadingar.domain.model.WordCard
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.PracticeCardsUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.PracticeCardsViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.PracticeCardsViewModelFactory

@Composable
fun PracticeCardsRoute(
    categoryId: String,
    categoryName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    var backendBaseUrl by remember { mutableStateOf(BackendConfig.DEFAULT_REMOTE_BASE_URL) }
    val viewModel: PracticeCardsViewModel = viewModel(
        factory = PracticeCardsViewModelFactory(container.studentContentRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        backendBaseUrl = container.sessionStore.resolveBackendBaseUrl()
    }

    LaunchedEffect(categoryId, categoryName) {
        viewModel.load(categoryId, categoryName)
    }

    PracticeCardsScreen(
        categoryName = categoryName,
        uiState = uiState,
        onBackClick = onBackClick,
        backendBaseUrl = backendBaseUrl,
        onRetry = { viewModel.load(categoryId, categoryName) },
        onCompleteClick = viewModel::completeCurrentWord,
        onCardChanged = viewModel::moveToCard
    )
}

@Composable
fun PracticeCardsScreen(
    categoryName: String,
    uiState: PracticeCardsUiState,
    onBackClick: () -> Unit,
    backendBaseUrl: String,
    onRetry: () -> Unit,
    onCompleteClick: () -> Unit,
    onCardChanged: (Int) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { uiState.cards.size })

    // Pager → ViewModel
    LaunchedEffect(pagerState.currentPage) {
        onCardChanged(pagerState.currentPage)
    }
    // ViewModel → Pager (e.g. after completeCurrentWord advances the index)
    LaunchedEffect(uiState.currentIndex) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != uiState.currentIndex) {
            pagerState.animateScrollToPage(uiState.currentIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
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
            Text(
                text = categoryName,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFE53734))
                }
            }

            !uiState.errorMessage.isNullOrBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = uiState.errorMessage,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(onClick = onRetry) {
                                Text(text = "Reintentar")
                            }
                        }
                    }
                }
            }

            uiState.cards.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No hay palabras disponibles para esta temática.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp),
                        color = Color(0xFF64748B)
                    )
                }
            }

            else -> {
                // Progress dots
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val dotCount = uiState.cards.size.coerceAtMost(7)
                    val activeIndex = pagerState.currentPage.coerceAtMost(6)
                    repeat(dotCount) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(10.dp)
                                .background(
                                    color = if (index == activeIndex) Color(0xFFE53734)
                                    else Color(0xFFE5E7EB),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Word pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    WordCardPage(
                        card = uiState.cards[page],
                        backendBaseUrl = backendBaseUrl
                    )
                }

                // Mark as read button
                Button(
                    onClick = onCompleteClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .navigationBarsPadding()
                        .height(60.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53734),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Marcar como leída",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WordCardPage(card: WordCard, backendBaseUrl: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "wordPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = card.word.uppercase(),
            color = Color(0xFFE53734),
            fontSize = 96.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 100.sp,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
        )

        if (!card.audioUrl.isNullOrBlank()) {
            AudioButton(
                audioUrl = card.audioUrl,
                backendBaseUrl = backendBaseUrl,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 28.dp, bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun AudioButton(audioUrl: String, backendBaseUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    val player = remember { MediaPlayer() }

    DisposableEffect(audioUrl) {
        onDispose {
            player.release()
        }
    }

    val resolvedUrl = resolvePlaybackUrl(audioUrl, backendBaseUrl)

    IconButton(
        onClick = {
            if (!isPlaying) {
                runCatching {
                    player.reset()
                    player.setDataSource(context, Uri.parse(resolvedUrl))
                    player.setOnPreparedListener { it.start() }
                    player.setOnCompletionListener { isPlaying = false }
                    player.prepareAsync()
                    isPlaying = true
                }
            }
        },
        modifier = modifier.size(56.dp)
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
            contentDescription = if (isPlaying) "Reproduciendo" else "Reproducir palabra",
            tint = Color(0xFFE53734),
            modifier = Modifier.size(32.dp)
        )
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
