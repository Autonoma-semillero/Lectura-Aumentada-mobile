package co.edu.uniautonoma.inclusivereadingar.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import co.edu.uniautonoma.inclusivereadingar.domain.model.AppUser
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherStudentsUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherStudentsViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherStudentsViewModelFactory

@Composable
fun TeacherStudentsRoute(
    onBack: () -> Unit,
    onThemesClick: () -> Unit,
    onWordCardsClick: () -> Unit,
    onPlansClick: (AppUser) -> Unit,
    onProgressClick: (AppUser) -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: TeacherStudentsViewModel = viewModel(
        factory = TeacherStudentsViewModelFactory(container.teacherContentRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TeacherStudentsScreen(
        uiState = uiState,
        onBack = onBack,
        onThemesClick = onThemesClick,
        onWordCardsClick = onWordCardsClick,
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
    onPlansClick: (AppUser) -> Unit,
    onProgressClick: (AppUser) -> Unit,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF8F7))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Color(0xFFE53734))
                }
                Text(text = "Estudiantes", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                Box(modifier = Modifier.size(40.dp))
            }

            when {
                uiState.isLoading -> Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFE53734))
                }

                !uiState.errorMessage.isNullOrBlank() -> Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.errorContainer) {
                        Text(
                            text = uiState.errorMessage,
                            modifier = Modifier.padding(24.dp).clickable(onClick = onRetry),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.students, key = { it.id }) { student ->
                        TeacherStudentCard(student = student, onPlansClick = { onPlansClick(student) }, onProgressClick = { onProgressClick(student) })
                    }
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

@Composable
private fun TeacherStudentCard(student: AppUser, onPlansClick: () -> Unit, onProgressClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 6.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(58.dp).background(Color(0xFFE53734).copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Groups, contentDescription = null, tint = Color(0xFFE53734))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = student.displayName ?: student.email, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(text = student.email, color = Color(0xFF64748B))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onPlansClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53734), contentColor = Color.White),
                    shape = RoundedCornerShape(18.dp)
                ) { Text("Plan") }
                Button(
                    onClick = onProgressClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2), contentColor = Color.White),
                    shape = RoundedCornerShape(18.dp)
                ) { Text("Progreso") }
            }
        }
    }
}
