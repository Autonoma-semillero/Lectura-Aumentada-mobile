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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudienceStudent
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentGroup
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherGroupsUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherGroupsViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherGroupsViewModelFactory

@Composable
fun TeacherGroupsRoute(onBack: () -> Unit) {
    val container = LocalContext.current.appContainer()
    val viewModel: TeacherGroupsViewModel = viewModel(
        factory = TeacherGroupsViewModelFactory(container.groupsRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TeacherGroupsScreen(
        uiState = uiState,
        onBack = onBack,
        onCreate = viewModel::openCreate,
        onEdit = viewModel::openEdit,
        onArchive = viewModel::requestArchive,
        onSave = viewModel::saveGroup,
        onDismissEditor = viewModel::dismissEditor,
        onConfirmArchive = viewModel::confirmArchive,
        onCancelArchive = viewModel::cancelArchive,
        onDismissMessage = viewModel::dismissMessage,
        onRetry = viewModel::load
    )
}

@Composable
fun TeacherGroupsScreen(
    uiState: TeacherGroupsUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (StudentGroup) -> Unit,
    onArchive: (StudentGroup) -> Unit,
    onSave: (String, String?, Set<String>) -> Unit,
    onDismissEditor: () -> Unit,
    onConfirmArchive: () -> Unit,
    onCancelArchive: () -> Unit,
    onDismissMessage: () -> Unit,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF8F7))) {
        Column(modifier = Modifier.fillMaxSize()) {
            GroupsHeader(onBack)
            when {
                uiState.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFE53734))
                }

                uiState.groups.isEmpty() && uiState.students.isEmpty() -> EmptyGroups(onRetry)
                else -> GroupsContent(uiState, onEdit, onArchive)
            }
        }

        FloatingActionButton(
            onClick = onCreate,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = Color(0xFFE53734),
            contentColor = Color.White
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Crear grupo")
        }

        uiState.errorMessage?.let { message ->
            MessageBanner(
                message = message,
                isError = true,
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
                onDismiss = onDismissMessage
            )
        }
        uiState.successMessage?.let { message ->
            MessageBanner(
                message = message,
                isError = false,
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
                onDismiss = onDismissMessage
            )
        }
    }

    if (uiState.showEditor) {
        GroupEditorDialog(
            group = uiState.editingGroup,
            students = uiState.students,
            isSaving = uiState.isSaving,
            onDismiss = onDismissEditor,
            onSave = onSave
        )
    }

    uiState.archiveConfirmGroup?.let { group ->
        AlertDialog(
            onDismissRequest = onCancelArchive,
            title = { Text("Archivar ${group.name}") },
            text = {
                Text("El grupo dejará de aparecer, pero sus estudiantes y su progreso no se eliminan.")
            },
            confirmButton = {
                TextButton(onClick = onConfirmArchive) { Text("Archivar", color = Color(0xFFE53734)) }
            },
            dismissButton = { TextButton(onClick = onCancelArchive) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun GroupsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = Color(0xFFE53734))
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text("Grupos de estudiantes", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("Organiza sin limitar: un estudiante puede estar en varios grupos", color = Color(0xFF64748B), fontSize = 12.sp)
        }
    }
}

@Composable
private fun GroupsContent(
    uiState: TeacherGroupsUiState,
    onEdit: (StudentGroup) -> Unit,
    onArchive: (StudentGroup) -> Unit
) {
    val studentsById = uiState.students.associateBy { it.id }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OrphanStudentsCard(uiState.unassignedStudents)
        }
        item {
            Text(
                "${uiState.groups.size} grupos activos",
                color = Color(0xFF64748B),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        items(uiState.groups, key = { it.id }) { group ->
            GroupCard(group, studentsById, onEdit, onArchive)
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun OrphanStudentsCard(students: List<AudienceStudent>) {
    Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFFFFE9E6)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.PersonOff, null, tint = Color(0xFFE53734))
                Text("Sin grupo · ${students.size}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Text(
                if (students.isEmpty()) "Todos los estudiantes pertenecen al menos a un grupo."
                else students.take(6).joinToString(" · ") { it.label } + if (students.size > 6) " · …" else "",
                color = Color(0xFF64514F),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun GroupCard(
    group: StudentGroup,
    studentsById: Map<String, AudienceStudent>,
    onEdit: (StudentGroup) -> Unit,
    onArchive: (StudentGroup) -> Unit
) {
    Surface(shape = RoundedCornerShape(22.dp), color = Color.White, shadowElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(Color(0xFFD4E3FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Groups, null, tint = Color(0xFF0060AC))
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(group.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${group.studentIds.size} estudiantes", color = Color(0xFF64748B), fontSize = 13.sp)
                }
                IconButton(onClick = { onEdit(group) }) {
                    Icon(Icons.Rounded.Edit, "Editar ${group.name}", tint = Color(0xFF0060AC))
                }
                IconButton(onClick = { onArchive(group) }) {
                    Icon(Icons.Rounded.DeleteOutline, "Archivar ${group.name}", tint = Color(0xFFE53734))
                }
            }
            group.description?.let { Text(it, color = Color(0xFF475569), fontSize = 13.sp) }
            val memberNames = group.studentIds.mapNotNull { studentsById[it]?.label }
            Text(
                text = if (memberNames.isEmpty()) "Aún no tiene estudiantes" else memberNames.joinToString(" · "),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFF64748B),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun GroupEditorDialog(
    group: StudentGroup?,
    students: List<AudienceStudent>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String?, Set<String>) -> Unit
) {
    var name by remember(group?.id) { mutableStateOf(group?.name.orEmpty()) }
    var description by remember(group?.id) { mutableStateOf(group?.description.orEmpty()) }
    var selected by remember(group?.id) { mutableStateOf(group?.studentIds.orEmpty().toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (group == null) "Crear grupo" else "Editar grupo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción opcional") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Text("Estudiantes (${selected.size})", fontWeight = FontWeight.Bold)
                if (students.isEmpty()) {
                    Text("No hay estudiantes disponibles", color = Color(0xFF64748B))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                        items(students, key = { it.id }) { student ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selected = selected.toggle(student.id)
                                }.padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = student.id in selected,
                                    onCheckedChange = { selected = selected.toggle(student.id) }
                                )
                                Column {
                                    Text(student.label, fontWeight = FontWeight.Medium)
                                    Text(
                                        if (student.unassigned) "Sin grupo" else "Ya está en ${student.groupIds.size} grupo(s)",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, description.ifBlank { null }, selected) },
                enabled = name.isNotBlank() && !isSaving
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancelar") } }
    )
}

@Composable
private fun EmptyGroups(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.Groups, null, modifier = Modifier.size(54.dp), tint = Color(0xFF94A3B8))
            Text("No hay grupos ni estudiantes", color = Color(0xFF64748B))
            TextButton(onClick = onRetry) { Text("Reintentar") }
        }
    }
}

@Composable
private fun MessageBanner(
    message: String,
    isError: Boolean,
    modifier: Modifier,
    onDismiss: () -> Unit
) {
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(3_500)
        onDismiss()
    }
    Surface(
        modifier = modifier.clickable(onClick = onDismiss),
        shape = RoundedCornerShape(16.dp),
        color = if (isError) MaterialTheme.colorScheme.errorContainer else Color(0xFFD9FBE5),
        shadowElevation = 8.dp
    ) {
        Text(message, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold)
    }
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (value in this) this - value else this + value
