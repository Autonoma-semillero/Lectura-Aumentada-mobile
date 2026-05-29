package co.edu.uniautonoma.inclusivereadingar.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentCategoryProgress
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
    onWordCardsClick: () -> Unit,
    onCompletedCardsClick: (studentId: String, categoryId: String, categoryName: String, phase2Ready: Boolean) -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: TeacherStudentProgressViewModel = viewModel(
        factory = TeacherStudentProgressViewModelFactory(container.docenteProgressRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(studentId) { viewModel.load(studentId) }

    TeacherStudentProgressScreen(
        studentName = studentName,
        uiState = uiState,
        onBack = onBack,
        onStudentsClick = onStudentsClick,
        onThemesClick = onThemesClick,
        onWordCardsClick = onWordCardsClick,
        onRetry = { viewModel.load(studentId) },
        onCompletedCardsClick = { cat ->
            onCompletedCardsClick(studentId, cat.categoryId, cat.categoryName, cat.phase2Ready)
        }
    )
}

@Composable
fun TeacherStudentProgressScreen(
    studentName: String,
    uiState: TeacherStudentProgressUiState,
    onBack: () -> Unit,
    onStudentsClick: () -> Unit,
    onThemesClick: () -> Unit,
    onWordCardsClick: () -> Unit,
    onRetry: () -> Unit,
    onCompletedCardsClick: (StudentCategoryProgress) -> Unit
) {
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
                Text(text = "Progreso por temática", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text(text = studentName, color = Color(0xFF64748B), fontSize = 14.sp)
            }
            Box(modifier = Modifier.size(40.dp))
        }

        when {
            uiState.isLoading -> Box(
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

            uiState.categories.isEmpty() -> Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Este estudiante aún no tiene tarjetas asignadas",
                    color = Color(0xFF64748B),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(24.dp)
                )
            }

            else -> LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                items(uiState.categories, key = { it.categoryId }) { category ->
                    CategoryProgressCard(
                        category = category,
                        onCompletedCardsClick = { onCompletedCardsClick(category) }
                    )
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
private fun CategoryProgressCard(
    category: StudentCategoryProgress,
    onCompletedCardsClick: () -> Unit
) {
    val green = Color(0xFF22C55E)
    val yellow = Color(0xFFFFD54F)
    val blue = Color(0xFF0060AC)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 5.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (category.phase2Ready) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomStart = 16.dp),
                    color = green
                ) {
                    Text(
                        text = "✓ Lista para Fase 2",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFE53734).copy(alpha = 0.10f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon(null),
                            contentDescription = null,
                            tint = Color(0xFFE53734),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.categoryName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${category.byStatus.completed}/${category.activeWords} palabras",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StackedProgressBar(
                        completedFraction = category.completedFraction,
                        activeFraction = category.activeFraction
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val label = when {
                            category.byStatus.completed == category.activeWords && category.activeWords > 0 -> "Dominado"
                            category.byStatus.completed > 0 -> "En progreso"
                            category.byStatus.active > 0 -> "Iniciado"
                            else -> "No iniciado"
                        }
                        Text(text = label, color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        val pct = (category.completedFraction * 100).toInt()
                        Text(text = "$pct%", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        StatusChip(label = "Nuevas", count = category.byStatus.new, color = Color(0xFF64748B))
                        StatusChip(label = "En progreso", count = category.byStatus.active, color = yellow)
                        StatusChip(label = "Dominadas", count = category.byStatus.completed, color = green)
                    }
                }

                if (category.byStatus.completed > 0) {
                    Button(
                        onClick = onCompletedCardsClick,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = blue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Ver dominadas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StackedProgressBar(
    completedFraction: Float,
    activeFraction: Float,
    modifier: Modifier = Modifier
) {
    val green  = Color(0xFF22C55E)
    val yellow = Color(0xFFFFD54F)
    val gray   = Color(0xFFFADCD8)

    val clamped   = completedFraction.coerceIn(0f, 1f)
    val combined  = (completedFraction + activeFraction).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(gray)
    ) {
        // Yellow layer (active + completed width combined), drawn first
        if (combined > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(combined)
                    .background(yellow)
            )
        }
        // Green layer (completed only) drawn on top of yellow
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .background(green)
            )
        }
    }
}

@Composable
private fun StatusChip(label: String, count: Int, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = "$count $label",
            fontSize = 11.sp,
            color = Color(0xFF64748B)
        )
    }
}
