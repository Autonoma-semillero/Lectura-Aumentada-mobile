package co.edu.uniautonoma.inclusivereadingar.presentation.student

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
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.domain.model.DailyPlanSummary
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.StudentTodayUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.StudentTodayViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.StudentTodayViewModelFactory

@Composable
fun StudentTodayRoute(
    onCategoryClick: (DailyPlanSummary) -> Unit,
    onOpenAr: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: StudentTodayViewModel = viewModel(
        factory = StudentTodayViewModelFactory(container.domanRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StudentTodayScreen(
        uiState = uiState,
        onCategoryClick = onCategoryClick,
        onOpenAr = onOpenAr,
        onRetry = viewModel::load,
        onLogoutClick = onLogoutClick
    )
}

@Composable
fun StudentTodayScreen(
    uiState: StudentTodayUiState,
    onCategoryClick: (DailyPlanSummary) -> Unit,
    onOpenAr: () -> Unit,
    onRetry: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F7))
    ) {
        StudentTodayHeader(uiState = uiState, onLogoutClick = onLogoutClick)

        Box(modifier = Modifier.weight(1f)) {
        when (uiState) {
            is StudentTodayUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFE53734))
                }
            }

            is StudentTodayUiState.NoActivePlan -> {
                NoActivePlanEmptyState(onRetry = onRetry)
            }

            is StudentTodayUiState.UpToDate -> {
                AllDoneTodayEmptyState(planName = uiState.planName)
            }

            is StudentTodayUiState.Error -> {
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
                                text = uiState.message,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Toca para intentar de nuevo",
                                modifier = Modifier.clickable(onClick = onRetry),
                                color = Color(0xFFE53734),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            is StudentTodayUiState.DueToday -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.activities, key = { it.planId + it.categoryId }) { activity ->
                        DueActivityCard(activity = activity, onClick = { onCategoryClick(activity) })
                    }
                }
            }
        }
        }

        // The AR scan entry point is relocated here from `ThemesScreen`. Per design, it is
        // rendered unconditionally regardless of due-work state: `ScanCardScreen` reads physical
        // markers whose content is independent of today's daily plan.
        ArScanEntryPoint(onOpenAr = onOpenAr)
    }
}

@Composable
private fun StudentTodayHeader(uiState: StudentTodayUiState, onLogoutClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFE53734).copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoStories,
                        contentDescription = null,
                        tint = Color(0xFFE53734)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Hoy", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text(text = headerSubtitle(uiState), color = Color(0xFF64748B))
                }
            }
            Row(
                modifier = Modifier
                    .clickable(onClick = onLogoutClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                    contentDescription = "Cerrar sesión",
                    tint = Color(0xFFE53734),
                    modifier = Modifier.size(20.dp)
                )
                Text(text = "Salir", color = Color(0xFFE53734), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

private fun headerSubtitle(uiState: StudentTodayUiState): String = when (uiState) {
    is StudentTodayUiState.DueToday -> uiState.levelName?.let { "${uiState.planName} · $it" }
        ?: uiState.planName.orEmpty()
    is StudentTodayUiState.UpToDate -> uiState.levelName?.let { "${uiState.planName} · $it" }
        ?: uiState.planName.orEmpty()
    else -> "Tu actividad de hoy"
}

@Composable
private fun DueActivityCard(activity: DailyPlanSummary, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = activity.categoryId, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = "${activity.pendingSessionsCount} sesiones pendientes",
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun ArScanEntryPoint(onOpenAr: () -> Unit) {
    Surface(
        onClick = onOpenAr,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFE53734),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Rounded.CenterFocusStrong, contentDescription = null)
            Text(text = "Explorar modelos 3D", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NoActivePlanEmptyState(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Aún no tienes un plan asignado",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Tu docente te asignará un plan de estudio pronto.",
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Toca para intentar de nuevo",
                modifier = Modifier.clickable(onClick = onRetry),
                color = Color(0xFFE53734),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AllDoneTodayEmptyState(planName: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "¡Ya estás al día!",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Completaste la actividad de hoy de ${planName ?: "tu plan"}.",
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}
