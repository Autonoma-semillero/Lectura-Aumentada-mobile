package co.edu.uniautonoma.inclusivereadingar.presentation.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanRepertoire
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentGroup
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherLiveSessionUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherLiveSessionViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherLiveSessionViewModelFactory

@Composable
fun TeacherLiveSessionRoute(onBack: () -> Unit) {
    val container = LocalContext.current.appContainer()
    val viewModel: TeacherLiveSessionViewModel = viewModel(
        factory = TeacherLiveSessionViewModelFactory(container.groupsRepository, container.liveDomanRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TeacherLiveSessionScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::loadSetup,
        onSelectGroup = viewModel::selectGroup,
        onSelectRepertoire = viewModel::selectRepertoire,
        onOpenRoom = viewModel::openRoom,
        onRefreshRoom = viewModel::refreshRoom,
        onCommand = viewModel::command
    )
}

@Composable
fun TeacherLiveSessionScreen(
    uiState: TeacherLiveSessionUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelectGroup: (String) -> Unit,
    onSelectRepertoire: (String) -> Unit,
    onOpenRoom: () -> Unit,
    onRefreshRoom: () -> Unit,
    onCommand: (String) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.session == null -> RoomSetup(
                state = uiState,
                onBack = onBack,
                onRetry = onRetry,
                onSelectGroup = onSelectGroup,
                onSelectRepertoire = onSelectRepertoire,
                onOpenRoom = onOpenRoom
            )
            else -> TeacherRoomControl(
                state = uiState,
                onBack = onBack,
                onRefresh = onRefreshRoom,
                onCommand = onCommand
            )
        }
    }
}

@Composable
private fun RoomSetup(
    state: TeacherLiveSessionUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelectGroup: (String) -> Unit,
    onSelectRepertoire: (String) -> Unit,
    onOpenRoom: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TeacherHeader("Nueva sala Doman", onBack) }
        state.errorMessage?.let { message ->
            item { ErrorCard(message, onRetry) }
        }
        item { Text("1. Elige el grupo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (state.groups.isEmpty()) {
            item { EmptySetup("No hay grupos activos. Crea un grupo antes de abrir una sala.") }
        } else {
            items(state.groups, key = { it.id }) { group ->
                SelectableGroup(group, group.id == state.selectedGroupId, onSelectGroup)
            }
        }
        item { Text("2. Elige el repertorio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (state.repertoires.isEmpty()) {
            item { EmptySetup("No hay repertorios disponibles. Crea tarjetas y un repertorio desde el contenido docente.") }
        } else {
            items(state.repertoires, key = { it.id }) { repertoire ->
                SelectableRepertoire(repertoire, repertoire.id == state.selectedRepertoireId, onSelectRepertoire)
            }
        }
        item {
            Button(
                onClick = onOpenRoom,
                enabled = !state.isSubmitting && state.selectedGroupId != null && state.selectedRepertoireId != null,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).sizeIn(minHeight = 48.dp)
            ) {
                if (state.isSubmitting) CircularProgressIndicator(modifier = Modifier.sizeIn(maxWidth = 20.dp, maxHeight = 20.dp), strokeWidth = 2.dp)
                else Text("Abrir sala")
            }
        }
    }
}

@Composable
private fun TeacherRoomControl(
    state: TeacherLiveSessionUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onCommand: (String) -> Unit
) {
    val session = requireNotNull(state.session)
    val participants = state.detail?.participants.orEmpty()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Sala Doman", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${participants.size} estudiantes conectados", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRefresh, enabled = !state.isSubmitting, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Actualizar estado de la sala")
            }
        }
        state.errorMessage?.let { ErrorCard(it, onRefresh) }
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(statusLabel(session.status), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    session.displayText ?: "La tarjeta aparecerá cuando inicies.",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        TeacherControls(session.status, state.isSubmitting, onCommand)
    }
}

@Composable
private fun TeacherControls(status: String, busy: Boolean, onCommand: (String) -> Unit) {
    when (status) {
        "lobby" -> Button(onClick = { onCommand("start") }, enabled = !busy, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text("Iniciar presentación") }
        "running" -> {
            Button(onClick = { onCommand("next") }, enabled = !busy, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text("Siguiente tarjeta") }
            OutlinedButton(onClick = { onCommand("repeat") }, enabled = !busy, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text("Repetir tarjeta") }
            OutlinedButton(onClick = { onCommand("pause") }, enabled = !busy, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text("Pausar") }
            OutlinedButton(onClick = { onCommand("end") }, enabled = !busy, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text("Finalizar sala") }
        }
        "paused" -> {
            Button(onClick = { onCommand("resume") }, enabled = !busy, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text("Continuar") }
            OutlinedButton(onClick = { onCommand("end") }, enabled = !busy, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text("Finalizar sala") }
        }
        else -> Text("La sala ya terminó.", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SelectableGroup(group: StudentGroup, selected: Boolean, onSelect: (String) -> Unit) {
    Card(
        onClick = { onSelect(group.id) },
        modifier = Modifier.fillMaxWidth().semantics { this.selected = selected; role = Role.RadioButton },
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${group.studentIds.size} estudiantes", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SelectableRepertoire(item: LiveDomanRepertoire, selected: Boolean, onSelect: (String) -> Unit) {
    Card(
        onClick = { onSelect(item.id) },
        modifier = Modifier.fillMaxWidth().semantics { this.selected = selected; role = Role.RadioButton },
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${item.cardCount} tarjetas", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TeacherHeader(title: String, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptySetup(message: String) = Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large) {
    Text(message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) = Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.large) {
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
        ElevatedButton(onClick = onRetry, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text("Reintentar") }
    }
}

private fun statusLabel(status: String): String = when (status) {
    "lobby" -> "SALA ABIERTA"
    "running" -> "PRESENTANDO"
    "paused" -> "EN PAUSA"
    else -> "FINALIZADA"
}
