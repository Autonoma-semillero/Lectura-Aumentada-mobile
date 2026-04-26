package co.edu.uniautonoma.inclusivereadingar.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.domain.model.DomanSessionHistoryItem
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherStudentProgressUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherStudentProgressViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherStudentProgressViewModelFactory

@Composable
fun TeacherStudentProgressRoute(
    studentId: String,
    studentName: String,
    onBack: () -> Unit,
    onStudentsClick: () -> Unit,
    onThemesClick: () -> Unit,
    onWordCardsClick: () -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: TeacherStudentProgressViewModel = viewModel(
        factory = TeacherStudentProgressViewModelFactory(container.domanRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(studentId) { viewModel.load(studentId) }

    TeacherStudentProgressScreen(
        studentName = studentName,
        uiState = uiState,
        onBack = onBack,
        onStudentsClick = onStudentsClick,
        onThemesClick = onThemesClick,
        onWordCardsClick = onWordCardsClick
    )
}

@Composable
fun TeacherStudentProgressScreen(
    studentName: String,
    uiState: TeacherStudentProgressUiState,
    onBack: () -> Unit,
    onStudentsClick: () -> Unit,
    onThemesClick: () -> Unit,
    onWordCardsClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF8F7))) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Color(0xFFE53734))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Progreso Doman", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                Text(text = studentName, color = Color(0xFF64748B))
            }
            androidx.compose.foundation.layout.Box(modifier = Modifier.padding(20.dp))
        }

        when {
            uiState.isLoading -> androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE53734))
            }

            !uiState.errorMessage.isNullOrBlank() -> androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text(text = uiState.errorMessage, modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            else -> LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Surface(shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 6.dp) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(text = "Resumen", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(text = "Sesiones planeadas: ${uiState.summary?.plannedSessionsCount ?: 0}")
                            Text(text = "Sesiones en curso: ${uiState.summary?.inProgressSessionsCount ?: 0}")
                            Text(text = "Sesiones completadas: ${uiState.summary?.completedSessionsCount ?: 0}")
                            Text(text = "Tarjetas nuevas: ${uiState.summary?.cardsNewCount ?: 0}")
                            Text(text = "Tarjetas activas: ${uiState.summary?.cardsActiveCount ?: 0}")
                            Text(text = "Tarjetas completadas: ${uiState.summary?.cardsCompletedCount ?: 0}")
                        }
                    }
                }
                items(uiState.history, key = { it.sessionId }) { item ->
                    SessionHistoryCard(item)
                }
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
private fun SessionHistoryCard(item: DomanSessionHistoryItem) {
    Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 4.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Sesión ${item.sessionIndex}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = "Estado: ${item.status}")
            Text(text = "Duración por tarjeta: ${item.displayMs} ms")
            Text(text = "Inicio: ${item.startedAt ?: "-"}")
            Text(text = "Fin: ${item.completedAt ?: "-"}")
        }
    }
}
