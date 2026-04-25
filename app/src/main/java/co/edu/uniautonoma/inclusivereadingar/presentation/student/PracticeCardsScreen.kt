package co.edu.uniautonoma.inclusivereadingar.presentation.student

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    val viewModel: PracticeCardsViewModel = viewModel(
        factory = PracticeCardsViewModelFactory(container.studentContentRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(categoryId, categoryName) {
        viewModel.load(categoryId, categoryName)
    }

    PracticeCardsScreen(
        categoryName = categoryName,
        uiState = uiState,
        onBackClick = onBackClick,
        onRetry = { viewModel.load(categoryId, categoryName) },
        onNextClick = viewModel::moveToNextCard,
        onPreviousClick = viewModel::moveToPreviousCard,
        onCompleteClick = viewModel::completeCurrentWord
    )
}

@Composable
fun PracticeCardsScreen(
    categoryName: String,
    uiState: PracticeCardsUiState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onCompleteClick: () -> Unit
) {
    val currentCard = uiState.currentCard

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
            Text(
                text = categoryName,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(uiState.cards.size.coerceAtMost(7)) { index ->
                val activeIndex = uiState.currentIndex.coerceAtMost(6)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(10.dp)
                        .background(
                            color = if (index == activeIndex) Color(0xFFE53734) else Color(0xFFE5E7EB),
                            shape = CircleShape
                        )
                )
            }
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
                            Text(
                                text = "Intentar de nuevo",
                                color = Color(0xFFE53734),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color.Transparent)
                                    .padding(4.dp)
                            )
                            Button(onClick = onRetry) {
                                Text(text = "Reintentar")
                            }
                        }
                    }
                }
            }

            currentCard == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No hay palabras disponibles para esta temática.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentCard.word.uppercase(),
                            color = Color(0xFFE53734),
                            fontSize = 72.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 78.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Palabra ${uiState.currentIndex + 1} de ${uiState.cards.size}",
                            color = Color(0xFF64748B),
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Completadas: ${uiState.completedCount}",
                            color = Color(0xFF475569),
                            fontSize = 16.sp
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onCompleteClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onPreviousClick,
                                enabled = uiState.currentIndex > 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(text = "Anterior")
                            }
                            Button(
                                onClick = onNextClick,
                                enabled = uiState.currentIndex < uiState.cards.lastIndex,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4A90E2),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(text = "Siguiente")
                            }
                        }
                    }
                }
            }
        }
    }
}
