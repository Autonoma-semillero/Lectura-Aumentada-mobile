package co.edu.uniautonoma.inclusivereadingar.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import co.edu.uniautonoma.inclusivereadingar.domain.model.AppUser
import co.edu.uniautonoma.inclusivereadingar.domain.model.WordCard
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.CategoryCardsViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.CategoryCardsViewModelFactory

@Composable
fun CategoryCardsRoute(
    categoryId: String,
    categoryName: String,
    onBack: () -> Unit,
    onCreateCardClick: (categoryId: String) -> Unit,
    onEditCardClick: (cardId: String) -> Unit,
    onThemesClick: () -> Unit,
    onStudentsClick: () -> Unit,
    onWordCardsClick: () -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: CategoryCardsViewModel = viewModel(
        factory = CategoryCardsViewModelFactory(container.teacherContentRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(categoryId) {
        viewModel.load(categoryId)
    }

    CategoryCardsScreen(
        categoryName = categoryName,
        categoryId = categoryId,
        cards = uiState.cards,
        students = uiState.students,
        isLoading = uiState.isLoading,
        isArchiving = uiState.isArchiving,
        errorMessage = uiState.errorMessage,
        archiveConfirmCardId = uiState.archiveConfirmCardId,
        onBack = onBack,
        onCreateCardClick = { onCreateCardClick(categoryId) },
        onEditCardClick = onEditCardClick,
        onArchiveRequest = viewModel::requestArchive,
        onArchiveConfirm = viewModel::confirmArchive,
        onArchiveCancel = viewModel::cancelArchive,
        onRetry = { viewModel.load(categoryId) },
        onErrorDismiss = viewModel::clearError,
        onThemesClick = onThemesClick,
        onStudentsClick = onStudentsClick,
        onWordCardsClick = onWordCardsClick
    )
}

@Composable
fun CategoryCardsScreen(
    categoryName: String,
    categoryId: String,
    cards: List<WordCard>,
    students: List<AppUser>,
    isLoading: Boolean,
    isArchiving: Boolean,
    errorMessage: String?,
    archiveConfirmCardId: String?,
    onBack: () -> Unit,
    onCreateCardClick: () -> Unit,
    onEditCardClick: (String) -> Unit,
    onArchiveRequest: (String) -> Unit,
    onArchiveConfirm: () -> Unit,
    onArchiveCancel: () -> Unit,
    onRetry: () -> Unit,
    onErrorDismiss: () -> Unit,
    onThemesClick: () -> Unit,
    onStudentsClick: () -> Unit,
    onWordCardsClick: () -> Unit
) {
    val primary = Color(0xFFE53734)
    val studentsById = students.associateBy { it.id }

    if (archiveConfirmCardId != null) {
        val card = cards.firstOrNull { it.id == archiveConfirmCardId }
        AlertDialog(
            onDismissRequest = onArchiveCancel,
            title = { Text("Eliminar tarjeta") },
            text = {
                Text(
                    text = if (card != null)
                        "¿Eliminar \"${card.word.uppercase()}\"? Esta acción no se puede deshacer."
                    else
                        "¿Eliminar esta tarjeta?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onArchiveConfirm,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = primary
                    )
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onArchiveCancel) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (!errorMessage.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = onErrorDismiss,
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = onErrorDismiss) { Text("OK") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFF8F7))
        ) {
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
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Volver",
                        tint = primary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Tarjetas",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = categoryName,
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Box(modifier = Modifier.size(48.dp))
            }

            when {
                isLoading || isArchiving -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primary)
                    }
                }

                cards.isEmpty() -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "No hay tarjetas en esta temática.\nToca + para crear la primera.",
                                textAlign = TextAlign.Center,
                                color = Color(0xFF64748B)
                            )
                            Button(
                                onClick = onCreateCardClick,
                                colors = ButtonDefaults.buttonColors(containerColor = primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Nueva tarjeta",
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            top = 8.dp,
                            bottom = 80.dp
                        )
                    ) {
                        items(cards, key = { it.id }) { card ->
                            WordCardListItem(
                                card = card,
                                studentName = studentsById[card.studentId]
                                    ?.let { it.displayName ?: it.email }
                                    ?: card.studentId,
                                onEditClick = { onEditCardClick(card.id) },
                                onDeleteClick = { onArchiveRequest(card.id) }
                            )
                        }
                    }
                }
            }

            TeacherBottomBar(
                activeTab = TeacherTab.WORD_CARDS,
                onThemesClick = onThemesClick,
                onStudentsClick = onStudentsClick,
                onWordCardsClick = onWordCardsClick
            )
        }

        if (cards.isNotEmpty() && !isLoading) {
            FloatingActionButton(
                onClick = onCreateCardClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 80.dp),
                containerColor = primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Nueva tarjeta")
            }
        }
    }
}

@Composable
private fun WordCardListItem(
    card: WordCard,
    studentName: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val primary = Color(0xFFE53734)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(primary.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = card.word.take(2).uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = card.word.uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color(0xFF111827)
                    )
                    if (!card.audioUrl.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = "Con audio",
                            tint = Color(0xFF0EA5E9),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = studentName,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )

                val statusLabel = when (card.status) {
                    "new" -> "Nueva"
                    "active" -> "Activa"
                    "completed" -> "Completada"
                    else -> card.status
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (card.status) {
                        "completed" -> Color(0xFFDCFCE7)
                        "active" -> Color(0xFFEFF6FF)
                        else -> Color(0xFFF1F5F9)
                    }
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = when (card.status) {
                            "completed" -> Color(0xFF166534)
                            "active" -> Color(0xFF1D4ED8)
                            else -> Color(0xFF475569)
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(onClick = onEditClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Editar",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Eliminar",
                    tint = primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
