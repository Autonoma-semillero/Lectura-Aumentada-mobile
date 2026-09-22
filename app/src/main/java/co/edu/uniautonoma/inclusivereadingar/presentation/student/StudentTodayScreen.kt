package co.edu.uniautonoma.inclusivereadingar.presentation.student

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.domain.model.DailyPlanSummary
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.StudentTodayUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.StudentTodayViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.StudentTodayViewModelFactory
import co.edu.uniautonoma.inclusivereadingar.presentation.theme.PrimaryRed

private val KidInk = Color(0xFF26324B)
private val KidMuted = Color(0xFF637083)
private val KidTeal = Color(0xFF147D82)
private val KidBlue = Color(0xFF4F72D8)
private val KidSky = Color(0xFFEAF7FF)
private val KidYellow = Color(0xFFFFD66B)
private val KidGreen = Color(0xFF42A66F)
private val KidPurple = Color(0xFF7969D8)

@Composable
fun StudentTodayRoute(
    sessionUserId: String,
    onCategoryClick: (DailyPlanSummary) -> Unit,
    onOpenAr: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: StudentTodayViewModel = viewModel(
        key = "student-today-$sessionUserId",
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
            .background(Color(0xFFFFFDF7))
    ) {
        StudentTodayHeader(uiState = uiState, onLogoutClick = onLogoutClick)

        Box(modifier = Modifier.weight(1f)) {
        when (uiState) {
            is StudentTodayUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KidTeal)
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
                                color = PrimaryRed,
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
                        DueActivityCard(
                            activity = activity,
                            onClick = { onCategoryClick(activity) }
                        )
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
                        .background(KidYellow.copy(alpha = 0.42f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoStories,
                        contentDescription = null,
                        tint = PrimaryRed
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
                    tint = KidMuted,
                    modifier = Modifier.size(20.dp)
                )
                Text(text = "Salir", color = KidMuted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

private fun headerSubtitle(uiState: StudentTodayUiState): String = when (uiState) {
    is StudentTodayUiState.DueToday -> planAndLevelLabel(uiState.planName, uiState.levelName)
    is StudentTodayUiState.UpToDate -> planAndLevelLabel(uiState.planName, uiState.levelName)
    else -> "Tu actividad de hoy"
}

private fun planAndLevelLabel(planName: String?, levelName: String?): String {
    val planLabel = planName?.takeIf { it.isNotBlank() } ?: "Plan de estudio"
    return levelName?.takeIf { it.isNotBlank() }?.let { "$planLabel · $it" } ?: planLabel
}

@Composable
private fun DueActivityCard(activity: DailyPlanSummary, onClick: () -> Unit) {
    val categoryLabel = activity.categoryName?.takeIf { it.isNotBlank() } ?: "Actividad de lectura"
    val artwork = categoryArtwork(categoryLabel)
    val totalSessions = maxOf(
        activity.sessionsCount,
        activity.completedSessionsCount + activity.pendingSessionsCount
    ).coerceAtLeast(1)
    val completedSessions = activity.completedSessionsCount.coerceIn(0, totalSessions)
    val remainingSessions = (totalSessions - completedSessions).coerceAtLeast(0)
    val progress = completedSessions.toFloat() / totalSessions.toFloat()
    val visibleSteps = totalSessions.coerceIn(1, 5)
    val completedVisibleSteps = (progress * visibleSteps).toInt().coerceIn(0, visibleSteps)
    val remainingLabel = if (remainingSessions == 1) {
        "¡Solo falta una lectura!"
    } else {
        "$remainingSessions lecturas para hoy"
    }
    val actionLabel = if (completedSessions > 0) "¡Sigamos!" else "¡A leer!"
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) 7.dp else 0.dp,
        label = "session-card-press"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 9.dp)
    ) {
        Surface(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 9.dp),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF3D61BB)
        ) {}

        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = pressOffset),
            shape = RoundedCornerShape(32.dp),
            color = KidSky,
            border = BorderStroke(1.5.dp, Color(0xFFCDE7F2)),
            shadowElevation = if (isPressed) 0.dp else 3.dp,
            interactionSource = interactionSource
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFF0FAFF),
                                Color(0xFFF8F3FF)
                            )
                        )
                    )
            ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 34.dp, y = (-38).dp)
                    .size(112.dp)
                    .background(KidYellow.copy(alpha = 0.22f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-28).dp, y = 36.dp)
                    .size(92.dp)
                    .background(KidPurple.copy(alpha = 0.10f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .padding(bottom = 7.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(y = 7.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = artwork.iconColor
                        ) {}
                        Surface(
                            modifier = Modifier.matchParentSize(),
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(artwork.bubbleColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = artwork.icon,
                                        contentDescription = null,
                                        tint = artwork.iconColor,
                                        modifier = Modifier.size(39.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = categoryLabel,
                            color = KidInk,
                            fontSize = 28.sp,
                            lineHeight = 31.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = artwork.message,
                            color = KidMuted,
                            fontSize = 16.sp,
                            lineHeight = 21.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White.copy(alpha = 0.80f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = remainingLabel,
                            color = KidInk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            repeat(visibleSteps) { index ->
                                val isCompleted = index < completedVisibleSteps
                                val isNext = index == completedVisibleSteps && completedVisibleSteps < visibleSteps
                                Surface(
                                    modifier = Modifier.size(28.dp),
                                    shape = CircleShape,
                                    color = when {
                                        isCompleted -> KidGreen
                                        isNext -> KidYellow
                                        else -> Color.White
                                    },
                                    border = if (!isCompleted && !isNext) {
                                        BorderStroke(2.dp, Color(0xFFB9D8E6))
                                    } else {
                                        null
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        when {
                                            isCompleted -> Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(17.dp)
                                            )

                                            isNext -> Icon(
                                                imageVector = Icons.Rounded.Star,
                                                contentDescription = null,
                                                tint = Color(0xFF755400),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .padding(bottom = 7.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = 7.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFFA91F1D)
                    ) {}
                    Surface(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = pressOffset),
                        shape = RoundedCornerShape(22.dp),
                        color = PrimaryRed,
                        contentColor = Color.White,
                        shadowElevation = if (isPressed) 0.dp else 2.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(text = actionLabel, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                }
            }
        }
    }
}

private data class CategoryArtwork(
    val icon: ImageVector,
    val bubbleColor: Color,
    val iconColor: Color,
    val message: String
)

private fun categoryArtwork(categoryName: String): CategoryArtwork {
    val normalizedName = categoryName.lowercase()
    return when {
        normalizedName.contains("cocina") ||
            normalizedName.contains("comida") ||
            normalizedName.contains("alimento") -> CategoryArtwork(
            icon = Icons.Rounded.Restaurant,
            bubbleColor = Color(0xFFFFE3A3),
            iconColor = Color(0xFF8B5A00),
            message = "¡Descubre palabras deliciosas!"
        )

        normalizedName.contains("animal") || normalizedName.contains("mascota") -> CategoryArtwork(
            icon = Icons.Rounded.Pets,
            bubbleColor = Color(0xFFFFDCE7),
            iconColor = Color(0xFFA43E63),
            message = "¡Conoce amigos increíbles!"
        )

        normalizedName.contains("naturaleza") ||
            normalizedName.contains("planta") ||
            normalizedName.contains("bosque") -> CategoryArtwork(
            icon = Icons.Rounded.Park,
            bubbleColor = Color(0xFFDDF3C8),
            iconColor = Color(0xFF34734B),
            message = "¡Explora el mundo verde!"
        )

        normalizedName.contains("transporte") ||
            normalizedName.contains("vehículo") ||
            normalizedName.contains("vehiculo") -> CategoryArtwork(
            icon = Icons.Rounded.DirectionsCar,
            bubbleColor = Color(0xFFD7ECFF),
            iconColor = Color(0xFF356BA4),
            message = "¡Viaja con nuevas palabras!"
        )

        normalizedName.contains("música") || normalizedName.contains("musica") -> CategoryArtwork(
            icon = Icons.Rounded.MusicNote,
            bubbleColor = Color(0xFFE7DFFF),
            iconColor = KidPurple,
            message = "¡Escucha y aprende jugando!"
        )

        normalizedName.contains("arte") || normalizedName.contains("color") -> CategoryArtwork(
            icon = Icons.Rounded.Palette,
            bubbleColor = Color(0xFFFFE0C7),
            iconColor = Color(0xFFA55325),
            message = "¡Crea con nuevas palabras!"
        )

        normalizedName.contains("deporte") || normalizedName.contains("juego") -> CategoryArtwork(
            icon = Icons.Rounded.SportsSoccer,
            bubbleColor = Color(0xFFD6F1E6),
            iconColor = Color(0xFF28725C),
            message = "¡Aprende mientras te diviertes!"
        )

        else -> CategoryArtwork(
            icon = Icons.Rounded.AutoStories,
            bubbleColor = Color(0xFFDCE8FF),
            iconColor = KidBlue,
            message = "¡Descubre nuevas palabras!"
        )
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
        color = KidBlue,
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
                color = PrimaryRed,
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
