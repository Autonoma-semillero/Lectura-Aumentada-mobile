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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.domain.model.DailyPlanSummary
import co.edu.uniautonoma.inclusivereadingar.presentation.auth.LaunchBrandLogo
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.StudentTodayUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.StudentTodayViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.StudentTodayViewModelFactory
import co.edu.uniautonoma.inclusivereadingar.presentation.theme.PrimaryRedDark

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
            .background(MaterialTheme.colorScheme.background)
    ) {
        StudentTodayHeader(uiState = uiState, onLogoutClick = onLogoutClick)

        Box(modifier = Modifier.weight(1f)) {
        when (uiState) {
            is StudentTodayUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                }
            }

            is StudentTodayUiState.NoActivePlan -> {
                NoActivePlanEmptyState(onRetry = onRetry)
            }

            is StudentTodayUiState.UpToDate -> {
                AllDoneTodayEmptyState(planName = uiState.planName, onOpenAr = onOpenAr)
            }

            is StudentTodayUiState.Error -> {
                StudentTodayErrorState(message = uiState.message, onRetry = onRetry)
            }

            is StudentTodayUiState.DueToday -> {
                val primaryActivity = uiState.activities.first()
                val laterActivities = uiState.activities.drop(1)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = primaryActivity.planId + primaryActivity.categoryId) {
                        DueActivityCard(
                            activity = primaryActivity,
                            onClick = { onCategoryClick(primaryActivity) }
                        )
                    }
                    if (laterActivities.isNotEmpty()) {
                        items(laterActivities, key = { it.planId + it.categoryId }) { activity ->
                            if (activity.pendingSessionsCount == 0) {
                                CompletedActivityCard(activity = activity)
                            } else {
                                LaterActivityCard(
                                    activity = activity,
                                    onClick = { onCategoryClick(activity) }
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun StudentTodayHeader(uiState: StudentTodayUiState, onLogoutClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
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
                LaunchBrandLogo(size = 48.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Hoy", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text(text = headerSubtitle(uiState), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                modifier = Modifier
                    .clickable(onClick = onLogoutClick)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Cerrar sesión"
                    }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(text = "Salir", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
    val remainingLabel = "$remainingSessions lecturas pendientes"
    val progressLabel = "$completedSessions de $totalSessions lecturas completadas"
    val actionLabel = "Leer"
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
            color = MaterialTheme.colorScheme.secondary
        ) {}

        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = pressOffset)
                .semantics {
                    role = Role.Button
                    contentDescription = "Leer $categoryLabel. $remainingLabel. $progressLabel"
                },
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = if (isPressed) 0.dp else 3.dp,
            interactionSource = interactionSource
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
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
                            color = MaterialTheme.colorScheme.surface,
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
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 28.sp,
                            lineHeight = 31.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                        isCompleted -> MaterialTheme.colorScheme.tertiary
                                        isNext -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    border = if (!isCompleted && !isNext) {
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                                    } else {
                                        null
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        when {
                                            isCompleted -> Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onTertiary,
                                                modifier = Modifier.size(17.dp)
                                            )

                                            isNext -> Icon(
                                                imageVector = Icons.Rounded.Star,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
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
                        color = PrimaryRedDark
                    ) {}
                    Surface(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = pressOffset),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
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

@Composable
private fun categoryArtwork(categoryName: String): CategoryArtwork {
    val colors = MaterialTheme.colorScheme
    val normalizedName = categoryName.lowercase()
    return when {
        normalizedName.contains("cocina") ||
            normalizedName.contains("comida") ||
            normalizedName.contains("alimento") -> CategoryArtwork(
            icon = Icons.Rounded.Restaurant,
            bubbleColor = colors.tertiaryContainer,
            iconColor = colors.onTertiaryContainer,
            message = "¡Descubre palabras deliciosas!"
        )

        normalizedName.contains("animal") || normalizedName.contains("mascota") -> CategoryArtwork(
            icon = Icons.Rounded.Pets,
            bubbleColor = colors.primaryContainer,
            iconColor = colors.onPrimaryContainer,
            message = "¡Conoce amigos increíbles!"
        )

        normalizedName.contains("naturaleza") ||
            normalizedName.contains("planta") ||
            normalizedName.contains("bosque") -> CategoryArtwork(
            icon = Icons.Rounded.Park,
            bubbleColor = colors.secondaryContainer,
            iconColor = colors.onSecondaryContainer,
            message = "¡Explora el mundo verde!"
        )

        normalizedName.contains("transporte") ||
            normalizedName.contains("vehículo") ||
            normalizedName.contains("vehiculo") -> CategoryArtwork(
            icon = Icons.Rounded.DirectionsCar,
            bubbleColor = colors.secondaryContainer,
            iconColor = colors.onSecondaryContainer,
            message = "¡Viaja con nuevas palabras!"
        )

        normalizedName.contains("música") || normalizedName.contains("musica") -> CategoryArtwork(
            icon = Icons.Rounded.MusicNote,
            bubbleColor = colors.tertiaryContainer,
            iconColor = colors.onTertiaryContainer,
            message = "¡Escucha y aprende jugando!"
        )

        normalizedName.contains("arte") || normalizedName.contains("color") -> CategoryArtwork(
            icon = Icons.Rounded.Palette,
            bubbleColor = colors.primaryContainer,
            iconColor = colors.onPrimaryContainer,
            message = "¡Crea con nuevas palabras!"
        )

        normalizedName.contains("deporte") || normalizedName.contains("juego") -> CategoryArtwork(
            icon = Icons.Rounded.SportsSoccer,
            bubbleColor = colors.tertiaryContainer,
            iconColor = colors.onTertiaryContainer,
            message = "¡Aprende mientras te diviertes!"
        )

        else -> CategoryArtwork(
            icon = Icons.Rounded.AutoStories,
            bubbleColor = colors.secondaryContainer,
            iconColor = colors.onSecondaryContainer,
            message = "¡Descubre nuevas palabras!"
        )
    }
}

@Composable
private fun LaterActivityCard(activity: DailyPlanSummary, onClick: () -> Unit) {
    val categoryLabel = activity.categoryName?.takeIf { it.isNotBlank() } ?: "Otra lectura"
    val artwork = categoryArtwork(categoryLabel)
    val remaining = activity.pendingSessionsCount.coerceAtLeast(1)
    val readingLabel = if (remaining == 1) "1 lectura pendiente" else "$remaining lecturas pendientes"

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "Leer después: $categoryLabel. $readingLabel"
            },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(artwork.bubbleColor, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = artwork.icon,
                    contentDescription = null,
                    tint = artwork.iconColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = categoryLabel, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun CompletedActivityCard(activity: DailyPlanSummary) {
    val categoryLabel = activity.categoryName?.takeIf { it.isNotBlank() } ?: "Lectura"
    val artwork = categoryArtwork(categoryLabel)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$categoryLabel terminada" },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(artwork.bubbleColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = categoryLabel,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ArExploreCard(onOpenAr: () -> Unit) {
    Surface(
        onClick = onOpenAr,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "Explorar modelos 3D cuando termines de leer"
            },
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Rounded.CenterFocusStrong, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Explora en 3D", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(text = "Cuando termines de leer", fontSize = 14.sp)
            }
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
                text = "Pídele a tu docente una lectura para empezar.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            RetryButton(onRetry = onRetry)
        }
    }
}

@Composable
private fun StudentTodayErrorState(message: String, onRetry: () -> Unit) {
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
                RetryButton(onRetry = onRetry)
            }
        }
    }
}

@Composable
private fun RetryButton(onRetry: () -> Unit) {
    Surface(
        onClick = onRetry,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Intentar de nuevo"
            },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = "Intentar de nuevo", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun AllDoneTodayEmptyState(planName: String?, onOpenAr: () -> Unit) {
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
                tint = MaterialTheme.colorScheme.tertiary,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            ArExploreCard(onOpenAr = onOpenAr)
        }
    }
}
