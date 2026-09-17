package co.edu.uniautonoma.inclusivereadingar.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import co.edu.uniautonoma.inclusivereadingar.domain.model.Category
import co.edu.uniautonoma.inclusivereadingar.domain.model.DailyPlanSummary
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
        factory = TeacherDomanPlansViewModelFactory(
            domanRepository = container.domanRepository,
            contentRepository = container.teacherContentRepository
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(studentId) { viewModel.load(studentId) }

    TeacherDomanPlansScreen(
        studentName = studentName,
        uiState = uiState,
        onBack = onBack,
        onOpenGenerateDialog = viewModel::openGenerateDialog,
        onDismissGenerateDialog = viewModel::dismissGenerateDialog,
        onGeneratePlan = { categoryId -> viewModel.generatePlan(studentId, categoryId) },
        onRegeneratePlan = { planId, categoryId -> viewModel.regeneratePlan(studentId, planId, categoryId) },
        onRequestDeletePlan = viewModel::requestDeletePlan,
        onCancelDeletePlan = viewModel::cancelDeletePlan,
        onConfirmDeletePlan = { viewModel.confirmDeletePlan(studentId) },
        onDismissError = viewModel::dismissError,
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
    onOpenGenerateDialog: () -> Unit,
    onDismissGenerateDialog: () -> Unit,
    onGeneratePlan: (categoryId: String) -> Unit,
    onRegeneratePlan: (planId: String, categoryId: String) -> Unit,
    onRequestDeletePlan: (planId: String) -> Unit,
    onCancelDeletePlan: () -> Unit,
    onConfirmDeletePlan: () -> Unit,
    onDismissError: () -> Unit,
    onStudentsClick: () -> Unit,
    onThemesClick: () -> Unit,
    onWordCardsClick: () -> Unit
) {
    val primary = Color(0xFFE53734)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.errorMessage) {
        val msg = uiState.errorMessage
        if (!msg.isNullOrBlank()) {
            scope.launch {
                snackbarHostState.showSnackbar(msg)
                onDismissError()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F7))
    ) {
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
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Volver",
                        tint = primary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Planes Doman", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                    Text(text = studentName, color = Color(0xFF64748B))
                }
                Box(modifier = Modifier.size(48.dp))
            }

            when {
                uiState.isLoading -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primary)
                }

                else -> LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    uiState.summary?.let { summary ->
                        item {
                            StudentProgressCard(
                                cardsNew = summary.cardsNewCount,
                                cardsActive = summary.cardsActiveCount,
                                cardsCompleted = summary.cardsCompletedCount,
                                sessionsPlanned = summary.plannedSessionsCount,
                                sessionsInProgress = summary.inProgressSessionsCount,
                                sessionsCompleted = summary.completedSessionsCount
                            )
                        }
                    }

                    if (uiState.plans.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No hay planes generados para hoy.\nToca + para crear uno.",
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFF64748B),
                                    fontSize = 15.sp
                                )
                            }
                        }
                    } else {
                        items(uiState.plans, key = { it.planId }) { plan ->
                            val category = uiState.allCategories.find { it.id == plan.categoryId }
                            DomanPlanCard(
                                plan = plan,
                                category = category,
                                isRegenerating = uiState.regeneratingPlanId == plan.planId,
                                isDeleting = uiState.deletingPlanId == plan.planId,
                                onRegenerate = { onRegeneratePlan(plan.planId, plan.categoryId) },
                                onDelete = { onRequestDeletePlan(plan.planId) }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }

            TeacherBottomBar(
                activeTab = TeacherTab.STUDENTS,
                onThemesClick = onThemesClick,
                onStudentsClick = onStudentsClick,
                onWordCardsClick = onWordCardsClick
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp),
            snackbar = { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White
                )
            }
        )

        if (!uiState.isLoading) {
            FloatingActionButton(
                onClick = onOpenGenerateDialog,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 24.dp, bottom = 92.dp),
                containerColor = if (uiState.isGenerating) Color(0xFFCBD5E1) else primary,
                contentColor = Color.White
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "Generar plan")
                }
            }
        }
    }

    if (uiState.showGenerateDialog) {
        GeneratePlanDialog(
            categories = uiState.dialogCategories,
            onDismiss = onDismissGenerateDialog,
            onConfirm = { categoryId -> onGeneratePlan(categoryId) }
        )
    }

    if (uiState.deleteConfirmPlanId != null) {
        val planToDelete = uiState.plans.find { it.planId == uiState.deleteConfirmPlanId }
        val categoryName = uiState.allCategories.find { it.id == planToDelete?.categoryId }?.name ?: "este plan"
        AlertDialog(
            onDismissRequest = onCancelDeletePlan,
            title = { Text("Eliminar plan", fontWeight = FontWeight.Bold) },
            text = { Text("¿Eliminar el plan de $categoryName? Se borrarán también las sesiones asociadas.") },
            confirmButton = {
                Button(
                    onClick = onConfirmDeletePlan,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53734), contentColor = Color.White)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDeletePlan) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudentProgressCard(
    cardsNew: Int,
    cardsActive: Int,
    cardsCompleted: Int,
    sessionsPlanned: Int,
    sessionsInProgress: Int,
    sessionsCompleted: Int
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "Progreso del estudiante", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricChip(label = "Nuevas", value = cardsNew, color = Color(0xFF0369A1), bg = Color(0xFFE0F2FE))
                MetricChip(label = "Activas", value = cardsActive, color = Color(0xFF15803D), bg = Color(0xFFDCFCE7))
                MetricChip(label = "Completadas", value = cardsCompleted, color = Color(0xFF7C3AED), bg = Color(0xFFEDE9FE))
                MetricChip(label = "Sesiones planeadas", value = sessionsPlanned, color = Color(0xFF92400E), bg = Color(0xFFFEF3C7))
                if (sessionsInProgress > 0) {
                    MetricChip(label = "En progreso", value = sessionsInProgress, color = Color(0xFFE53734), bg = Color(0xFFFEE2E2))
                }
                MetricChip(label = "Sesiones completadas", value = sessionsCompleted, color = Color(0xFF15803D), bg = Color(0xFFDCFCE7))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DomanPlanCard(
    plan: DailyPlanSummary,
    category: Category?,
    isRegenerating: Boolean,
    isDeleting: Boolean,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit
) {
    val primary = Color(0xFFE53734)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(category?.icon),
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category?.name ?: "Categoría",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = category?.description ?: "",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricChip(label = "Tarjetas", value = plan.cardsCount, color = Color(0xFF0369A1), bg = Color(0xFFE0F2FE))
                MetricChip(label = "Sesiones", value = plan.sessionsCount, color = Color(0xFF92400E), bg = Color(0xFFFEF3C7))
                if (plan.completedSessionsCount > 0) {
                    MetricChip(label = "Completadas", value = plan.completedSessionsCount, color = Color(0xFF15803D), bg = Color(0xFFDCFCE7))
                }
                if (plan.pendingSessionsCount > 0) {
                    MetricChip(label = "Pendientes", value = plan.pendingSessionsCount, color = Color(0xFF7C3AED), bg = Color(0xFFEDE9FE))
                }
            }

            if (plan.words.isNotEmpty()) {
                Text(
                    text = plan.words.joinToString(" · "),
                    color = Color(0xFF475569),
                    fontSize = 13.sp
                )
            }

            val isBusy = isRegenerating || isDeleting
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRegenerate,
                    enabled = !isBusy && plan.completedSessionsCount == 0,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFEE2E2),
                        contentColor = primary,
                        disabledContainerColor = Color(0xFFF1F5F9),
                        disabledContentColor = Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (isRegenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = primary, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp).size(18.dp)
                        )
                        Text(
                            text = if (plan.completedSessionsCount > 0) "En progreso" else "Regenerar",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Button(
                    onClick = onDelete,
                    enabled = !isBusy,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53734),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFF1F5F9),
                        disabledContentColor = Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Eliminar plan", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: Int, color: Color, bg: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = bg) {
        Text(
            text = "$label: $value",
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun GeneratePlanDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: String) -> Unit
) {
    val primary = Color(0xFFE53734)
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generar plan del día", fontWeight = FontWeight.Bold) },
        text = {
            if (categories.isEmpty()) {
                Text(
                    text = "Todas las categorías ya tienen un plan para hoy.",
                    color = Color(0xFF64748B)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Selecciona una categoría:",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedCategoryId == category.id,
                                    onClick = { selectedCategoryId = category.id }
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selectedCategoryId == category.id,
                                onClick = { selectedCategoryId = category.id },
                                colors = RadioButtonDefaults.colors(selectedColor = primary)
                            )
                            Column {
                                Text(text = category.name, fontWeight = FontWeight.SemiBold)
                                if (!category.description.isNullOrBlank()) {
                                    Text(text = category.description, color = Color(0xFF64748B), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (categories.isNotEmpty()) {
                Button(
                    onClick = { selectedCategoryId?.let { onConfirm(it) } },
                    enabled = selectedCategoryId != null,
                    colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = Color.White)
                ) {
                    Text("Generar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF64748B))
            }
        }
    )
}
