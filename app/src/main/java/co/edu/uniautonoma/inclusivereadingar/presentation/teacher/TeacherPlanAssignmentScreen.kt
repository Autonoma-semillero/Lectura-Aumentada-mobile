package co.edu.uniautonoma.inclusivereadingar.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceGroup
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceSearchItem
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceStudent
import co.edu.uniautonoma.inclusivereadingar.domain.model.BulkPlanStatus
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherPlanAssignmentUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherPlanAssignmentViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherPlanAssignmentViewModelFactory

@Composable
fun TeacherPlanAssignmentRoute(
    preselectedStudentId: String?,
    preselectedStudentName: String?,
    onBack: () -> Unit,
    onManageGroups: () -> Unit
) {
    val container = LocalContext.current.appContainer()
    val viewModel: TeacherPlanAssignmentViewModel = viewModel(
        factory = TeacherPlanAssignmentViewModelFactory(
            groupsRepository = container.groupsRepository,
            contentRepository = container.teacherContentRepository,
            domanRepository = container.domanRepository
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(preselectedStudentId) {
        viewModel.load(preselectedStudentId, preselectedStudentName)
    }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAudience()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    TeacherPlanAssignmentScreen(
        uiState = uiState,
        onBack = onBack,
        onManageGroups = onManageGroups,
        onQueryChange = viewModel::onQueryChange,
        onToggle = viewModel::toggle,
        onRemoveGroup = viewModel::removeGroup,
        onRemoveStudent = viewModel::removeStudent,
        onSelectCategory = viewModel::selectCategory,
        onGenerate = viewModel::generate,
        onDismissError = viewModel::dismissError,
        onRetry = viewModel::retry
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherPlanAssignmentScreen(
    uiState: TeacherPlanAssignmentUiState,
    onBack: () -> Unit,
    onManageGroups: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggle: (AudienceSearchItem) -> Unit,
    onRemoveGroup: (String) -> Unit,
    onRemoveStudent: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onGenerate: () -> Unit,
    onDismissError: () -> Unit,
    onRetry: () -> Unit
) {
    val hasInitialLoadError = !uiState.isLoading &&
        uiState.errorMessage != null &&
        uiState.categories.isEmpty() &&
        uiState.searchItems.isEmpty()
    Box(Modifier.fillMaxSize().background(Color(0xFFFFF8F7))) {
        Column(Modifier.fillMaxSize()) {
            AssignmentHeader(onBack, onManageGroups)
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFE53734))
                }
            } else if (hasInitialLoadError) {
                InitialAssignmentError(
                    message = requireNotNull(uiState.errorMessage),
                    onRetry = onRetry
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            "1. Elige el público",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    item {
                        AudienceSearchBox(
                            query = uiState.query,
                            isSearching = uiState.isSearching,
                            onQueryChange = onQueryChange
                        )
                    }
                    if (uiState.searchItems.isEmpty() && !uiState.isSearching) {
                        item {
                            Text(
                                "No encontramos estudiantes ni grupos con esa búsqueda.",
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        items(uiState.searchItems, key = { it.audienceKey }) { item ->
                            AudienceResultRow(
                                item = item,
                                selected = when (item) {
                                    is AudienceGroup -> item.id in uiState.selection.groupIds
                                    is AudienceStudent -> item.id in uiState.selection.studentIds
                                },
                                onToggle = { onToggle(item) }
                            )
                        }
                    }
                    item {
                        SelectionSummary(
                            uiState = uiState,
                            onRemoveGroup = onRemoveGroup,
                            onRemoveStudent = onRemoveStudent
                        )
                    }
                    item {
                        Text("2. Elige la categoría", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    item {
                        if (uiState.categories.isEmpty()) {
                            Text("No hay categorías activas.", color = Color(0xFFB91C1C))
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.categories.forEach { category ->
                                    FilterChip(
                                        selected = uiState.selectedCategoryId == category.id,
                                        onClick = { onSelectCategory(category.id) },
                                        label = { Text(category.name) },
                                        leadingIcon = if (uiState.selectedCategoryId == category.id) {
                                            { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                    uiState.result?.let { result ->
                        item { AssignmentResultCard(uiState) }
                    }
                    item { Spacer(Modifier.height(10.dp)) }
                }

                Surface(color = Color.White, shadowElevation = 8.dp) {
                    Button(
                        onClick = onGenerate,
                        enabled = uiState.canGenerate,
                        modifier = Modifier.fillMaxWidth().padding(20.dp).height(54.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Rounded.Send, null)
                            Text(
                                "Asignar a ${uiState.resolvedStudentIds.size} estudiante(s)",
                                modifier = Modifier.padding(start = 10.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        uiState.errorMessage?.takeUnless { hasInitialLoadError }?.let { message ->
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp).clickable(onClick = onDismissError),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shadowElevation = 8.dp
            ) {
                Text(
                    message,
                    Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(4_000)
                onDismissError()
            }
        }
    }
}

@Composable
private fun InitialAssignmentError(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Button(onClick = onRetry) {
                    Text("Reintentar")
                }
            }
        }
    }
}

@Composable
private fun AssignmentHeader(onBack: () -> Unit, onManageGroups: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = Color(0xFFE53734))
        }
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text("Asignar plan Doman", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("Combina grupos y estudiantes sin duplicarlos", color = Color(0xFF64748B), fontSize = 12.sp)
        }
        TextButton(onClick = onManageGroups) { Text("Grupos") }
    }
}

@Composable
private fun AudienceSearchBox(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Buscar estudiante o grupo") },
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = {
            if (isSearching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        },
        singleLine = true,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun AudienceResultRow(
    item: AudienceSearchItem,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Color(0xFFFFE9E6) else Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(38.dp).background(
                    if (item is AudienceGroup) Color(0xFFD4E3FF) else Color(0xFFD9FBE5),
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (item is AudienceGroup) Icons.Rounded.Groups else Icons.Rounded.Person,
                    null,
                    tint = if (item is AudienceGroup) Color(0xFF0060AC) else Color(0xFF166534),
                    modifier = Modifier.size(21.dp)
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                val title = when (item) {
                    is AudienceGroup -> item.name
                    is AudienceStudent -> item.label
                }
                val subtitle = when (item) {
                    is AudienceGroup -> "Grupo · ${item.studentIds.size} estudiantes"
                    is AudienceStudent -> if (item.unassigned) "Estudiante · Sin grupo" else "Estudiante · ${item.email}"
                }
                Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = Color(0xFF64748B), fontSize = 12.sp, maxLines = 1)
            }
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectionSummary(
    uiState: TeacherPlanAssignmentUiState,
    onRemoveGroup: (String) -> Unit,
    onRemoveStudent: (String) -> Unit
) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFF1F5F9)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Selección", fontWeight = FontWeight.Bold)
            if (uiState.selection.groupIds.isEmpty() && uiState.selection.studentIds.isEmpty()) {
                Text("Selecciona uno o varios resultados.", color = Color(0xFF64748B), fontSize = 13.sp)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    uiState.selection.groupIds.forEach { id ->
                        val label = uiState.knownGroups[id]?.name ?: "Grupo"
                        FilterChip(
                            selected = true,
                            onClick = { onRemoveGroup(id) },
                            label = { Text(label) },
                            leadingIcon = { Icon(Icons.Rounded.Groups, null, Modifier.size(16.dp)) },
                            trailingIcon = { Icon(Icons.Rounded.Close, "Quitar", Modifier.size(16.dp)) }
                        )
                    }
                    uiState.selection.studentIds.forEach { id ->
                        val label = uiState.knownStudents[id]?.label ?: "Estudiante"
                        FilterChip(
                            selected = true,
                            onClick = { onRemoveStudent(id) },
                            label = { Text(label) },
                            leadingIcon = { Icon(Icons.Rounded.Person, null, Modifier.size(16.dp)) },
                            trailingIcon = { Icon(Icons.Rounded.Close, "Quitar", Modifier.size(16.dp)) }
                        )
                    }
                }
                Text(
                    "${uiState.resolvedStudentIds.size} estudiantes únicos recibirán el plan. Las coincidencias se deduplican.",
                    color = Color(0xFF166534),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun AssignmentResultCard(uiState: TeacherPlanAssignmentUiState) {
    val result = requireNotNull(uiState.result)
    val success = result.summary.generated + result.summary.existing
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (result.summary.failed == 0) Color(0xFFD9FBE5) else Color(0xFFFFF3CD)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Resultado", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text("$success de ${result.summary.total} asignaciones listas")
            Text(
                "${result.summary.generated} creadas · ${result.summary.existing} ya existían · ${result.summary.failed} fallidas",
                color = Color(0xFF475569),
                fontSize = 13.sp
            )
            result.results.filter { it.status == BulkPlanStatus.FAILED }.forEach { failure ->
                val name = uiState.knownStudents[failure.studentId]?.label ?: failure.studentId
                Text("• $name: ${failure.error ?: "No se pudo generar"}", color = Color(0xFFB91C1C), fontSize = 12.sp)
            }
        }
    }
}
