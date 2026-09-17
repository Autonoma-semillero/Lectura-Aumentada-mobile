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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PlaylistAdd
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.StudentWithProgress
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherStudentsUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherStudentsViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherStudentsViewModelFactory

@Composable
fun TeacherStudentsRoute(
    onBack: () -> Unit,
    onThemesClick: () -> Unit,
    onWordCardsClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onAssignPlanClick: () -> Unit,
    onPlansClick: (id: String, name: String) -> Unit,
    onProgressClick: (id: String, name: String) -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: TeacherStudentsViewModel = viewModel(
        factory = TeacherStudentsViewModelFactory(container.docenteProgressRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TeacherStudentsScreen(
        uiState = uiState,
        onBack = onBack,
        onThemesClick = onThemesClick,
        onWordCardsClick = onWordCardsClick,
        onGroupsClick = onGroupsClick,
        onAssignPlanClick = onAssignPlanClick,
        onPlansClick = onPlansClick,
        onProgressClick = onProgressClick,
        onRetry = viewModel::load
    )
}

@Composable
fun TeacherStudentsScreen(
    uiState: TeacherStudentsUiState,
    onBack: () -> Unit,
    onThemesClick: () -> Unit,
    onWordCardsClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onAssignPlanClick: () -> Unit,
    onPlansClick: (id: String, name: String) -> Unit,
    onProgressClick: (id: String, name: String) -> Unit,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF8F7))) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    Text(text = "Mis Estudiantes", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Text(
                        text = "Panel de Avance",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
                Box(modifier = Modifier.size(40.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAssignPlanClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53734))
                ) {
                    Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("Asignar plan", modifier = Modifier.padding(start = 7.dp), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onGroupsClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4E3FF),
                        contentColor = Color(0xFF0060AC)
                    )
                ) {
                    Icon(Icons.Rounded.Groups, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("Grupos", modifier = Modifier.padding(start = 7.dp), fontWeight = FontWeight.Bold)
                }
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

                uiState.students.isEmpty() -> Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aún no tienes estudiantes asignados",
                        color = Color(0xFF64748B),
                        fontSize = 16.sp
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(Modifier.height(4.dp)) }
                    items(uiState.students, key = { it.id }) { student ->
                        StudentProgressCard(
                            student = student,
                            onPlansClick = { onPlansClick(student.id, student.nameOrEmail) },
                            onProgressClick = { onProgressClick(student.id, student.nameOrEmail) }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }

            TeacherBottomBar(
                activeTab = TeacherTab.STUDENTS,
                onThemesClick = onThemesClick,
                onStudentsClick = {},
                onWordCardsClick = onWordCardsClick
            )
        }
    }
}

private val avatarColors = listOf(
    Color(0xFF0060AC),
    Color(0xFF735C00),
    Color(0xFF008080),
    Color(0xFF6A0DAD),
    Color(0xFF2E7D32)
)

@Composable
private fun StudentProgressCard(
    student: StudentWithProgress,
    onPlansClick: () -> Unit,
    onProgressClick: () -> Unit
) {
    val primary = Color(0xFFE53734)
    val blue = Color(0xFF0060AC)
    val avatarColor = avatarColors[student.id.hashCode().and(0x7FFFFFFF) % avatarColors.size]
    val progressFraction = student.progressPercent / 100f

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(avatarColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.initials(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.nameOrEmail,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (student.hasPhase2Ready) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFC8E6C9)
                            ) {
                                Text(
                                    text = "✓ Fase 2",
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFF1D3D0)
                            ) {
                                Text(
                                    text = "Fase 1",
                                    color = Color(0xFF5B403D),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${student.completedWords} de ${student.totalActiveWords} palabras dominadas",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${student.progressPercent}%",
                        color = primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = primary,
                    trackColor = Color(0xFFFADCD8),
                    strokeCap = StrokeCap.Round
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onProgressClick,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Ver Detalles", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Button(
                    onClick = onPlansClick,
                    modifier = Modifier.width(52.dp).height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4E3FF),
                        contentColor = blue
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Icon(Icons.Rounded.BarChart, contentDescription = "Plan", modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
