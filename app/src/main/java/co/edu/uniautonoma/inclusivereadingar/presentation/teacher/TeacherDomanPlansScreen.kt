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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherDomanPlansUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherDomanPlansViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherDomanPlansViewModelFactory

@Composable
fun TeacherDomanPlansRoute(
    studentId: String,
    studentName: String,
    onBack: () -> Unit,
    onStudentsClick: () -> Unit,
    onThemesClick: () -> Unit,
    onWordCardsClick: () -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: TeacherDomanPlansViewModel = viewModel(
        factory = TeacherDomanPlansViewModelFactory(container.domanRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(studentId) { viewModel.load(studentId) }

    TeacherDomanPlansScreen(
        studentName = studentName,
        uiState = uiState,
        onBack = onBack,
        onRegenerate = { viewModel.regenerate(studentId, uiState.plan?.categoryId) },
        onStudentsClick = onStudentsClick,
        onThemesClick = onThemesClick,
        onWordCardsClick = onWordCardsClick
    )
}

@Composable
fun TeacherDomanPlansScreen(
    studentName: String,
    uiState: TeacherDomanPlansUiState,
    onBack: () -> Unit,
    onRegenerate: () -> Unit,
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
                Text(text = "Plan Doman", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                Text(text = studentName, color = Color(0xFF64748B))
            }
            Button(
                onClick = onRegenerate,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53734), contentColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) { Text(if (uiState.isGenerating) "..." else "Regenerar") }
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
                            Text(text = "Plan de hoy", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(text = "Tarjetas: ${uiState.plan?.cardsCount ?: 0}")
                            Text(text = "Sesiones: ${uiState.plan?.sessionsCount ?: 0}")
                            Text(text = "Pendientes: ${uiState.plan?.pendingSessionsCount ?: 0}")
                            Text(text = "Completadas: ${uiState.plan?.completedSessionsCount ?: 0}")
                        }
                    }
                }
                item {
                    Surface(shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 6.dp) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(text = "Palabras del plan", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            (uiState.plan?.words ?: emptyList()).forEach { word ->
                                Text(text = word, color = Color(0xFF475569))
                            }
                        }
                    }
                }
                item {
                    Surface(shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 6.dp) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(text = "Resumen del estudiante", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(text = "Tarjetas nuevas: ${uiState.summary?.cardsNewCount ?: 0}")
                            Text(text = "Tarjetas activas: ${uiState.summary?.cardsActiveCount ?: 0}")
                            Text(text = "Tarjetas completadas: ${uiState.summary?.cardsCompletedCount ?: 0}")
                        }
                    }
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
