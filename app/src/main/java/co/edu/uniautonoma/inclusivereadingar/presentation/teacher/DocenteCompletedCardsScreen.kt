package co.edu.uniautonoma.inclusivereadingar.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.domain.model.CompletedCard
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.DocenteCompletedCardsUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.DocenteCompletedCardsViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.DocenteCompletedCardsViewModelFactory
import android.media.MediaPlayer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun DocenteCompletedCardsRoute(
    studentId: String,
    categoryId: String,
    categoryName: String,
    phase2Ready: Boolean,
    onBack: () -> Unit,
    onStudentsClick: () -> Unit,
    onThemesClick: () -> Unit,
    onWordCardsClick: () -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: DocenteCompletedCardsViewModel = viewModel(
        factory = DocenteCompletedCardsViewModelFactory(container.docenteProgressRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(studentId, categoryId) { viewModel.init(studentId, categoryId) }

    DocenteCompletedCardsScreen(
        categoryName = categoryName,
        phase2Ready = phase2Ready,
        uiState = uiState,
        onBack = onBack,
        onStudentsClick = onStudentsClick,
        onThemesClick = onThemesClick,
        onWordCardsClick = onWordCardsClick,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::retry
    )
}

@Composable
fun DocenteCompletedCardsScreen(
    categoryName: String,
    phase2Ready: Boolean,
    uiState: DocenteCompletedCardsUiState,
    onBack: () -> Unit,
    onStudentsClick: () -> Unit,
    onThemesClick: () -> Unit,
    onWordCardsClick: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit
) {
    val mediaPlayer = remember { MediaPlayer() }
    var playingUrl by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    fun toggleAudio(url: String) {
        try {
            if (playingUrl == url) {
                mediaPlayer.stop()
                mediaPlayer.reset()
                playingUrl = null
            } else {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(url)
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener { it.start() }
                mediaPlayer.setOnCompletionListener { playingUrl = null }
                playingUrl = url
            }
        } catch (_: Exception) {
            playingUrl = null
        }
    }

    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 3 && !uiState.isLoadingMore && uiState.hasMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF8F7))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color(0xFFE53734)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Palabras dominadas", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text(text = categoryName, color = Color(0xFF64748B), fontSize = 13.sp)
            }
            Box(modifier = Modifier.size(40.dp))
        }

        when {
            uiState.isLoadingInitial -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFE53734))
            }

            !uiState.errorMessage.isNullOrBlank() -> Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Toca para reintentar",
                            modifier = Modifier.clickable(onClick = onRetry),
                            color = Color(0xFFE53734),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            uiState.cards.isEmpty() -> Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aún no hay palabras dominadas en esta temática",
                    color = Color(0xFF64748B),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(24.dp)
                )
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(Modifier.height(4.dp))
                    if (phase2Ready) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF22C55E)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "✓ $categoryName está lista para Fase 2",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFE9E6)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFFE53734),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${uiState.totalCards} palabras dominadas",
                                color = Color(0xFFE53734),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                itemsIndexed(uiState.cards, key = { _, card -> card.id }) { _, card ->
                    CompletedCardItem(
                        card = card,
                        isPlaying = playingUrl == card.audioUrl,
                        onAudioClick = { card.audioUrl?.let { toggleAudio(it) } }
                    )
                }

                if (uiState.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color(0xFFE53734),
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }

                if (!uiState.loadMoreError.isNullOrBlank()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = onLoadMore,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53734))
                            ) {
                                Text("Reintentar", color = Color.White)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        TeacherBottomBar(
            activeTab = TeacherTab.STUDENTS,
            onThemesClick = onThemesClick,
            onStudentsClick = onStudentsClick,
            onWordCardsClick = onWordCardsClick
        )
    }
}

@Composable
private fun CompletedCardItem(
    card: CompletedCard,
    isPlaying: Boolean,
    onAudioClick: () -> Unit
) {
    val green = Color(0xFF22C55E)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(green.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = green,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = card.word.uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp
                )
                val dateText = card.completedAt?.let { "Dominada el ${formatCompletedDate(it)} · " } ?: ""
                Text(
                    text = "${dateText}Vista ${card.timesShown} veces",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }
            if (card.audioUrl != null) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = if (isPlaying) Color(0xFF0060AC) else Color(0xFFD4E3FF),
                            shape = CircleShape
                        )
                        .clickable(onClick = onAudioClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Detener" else "Reproducir audio",
                        tint = if (isPlaying) Color.White else Color(0xFF0060AC),
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Spacer(Modifier.width(42.dp))
            }
        }
    }
}

private fun formatCompletedDate(isoDate: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(isoDate) ?: return ""
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("es-ES"))
        formatter.format(date)
    } catch (_: Exception) {
        ""
    }
}
