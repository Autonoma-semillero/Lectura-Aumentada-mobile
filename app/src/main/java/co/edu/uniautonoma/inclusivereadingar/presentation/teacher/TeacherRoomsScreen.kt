package co.edu.uniautonoma.inclusivereadingar.presentation.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSession
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherRoomsUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherRoomsViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherRoomsViewModelFactory

private val ACTIVE_ROOM_STATUSES = setOf("lobby", "running", "paused")

@Composable
fun TeacherRoomsRoute(
    onCreateRoom: () -> Unit,
    onThemesClick: () -> Unit,
    onStudentsClick: () -> Unit,
    onWordCardsClick: () -> Unit
) {
    val container = LocalContext.current.appContainer()
    val viewModel: TeacherRoomsViewModel = viewModel(
        factory = TeacherRoomsViewModelFactory(container.liveDomanRepository, container.groupsRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TeacherRoomsScreen(
        uiState = uiState,
        onRetry = viewModel::load,
        onEndRoom = viewModel::endRoom,
        onCreateRoom = onCreateRoom,
        onThemesClick = onThemesClick,
        onStudentsClick = onStudentsClick,
        onWordCardsClick = onWordCardsClick
    )
}

@Composable
fun TeacherRoomsScreen(
    uiState: TeacherRoomsUiState,
    onRetry: () -> Unit,
    onEndRoom: (String) -> Unit,
    onCreateRoom: () -> Unit,
    onThemesClick: () -> Unit,
    onStudentsClick: () -> Unit,
    onWordCardsClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Salas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = onRetry, enabled = !uiState.isSubmitting, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Actualizar salas")
            }
        }
        Button(
            onClick = onCreateRoom,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).sizeIn(minHeight = 48.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text("Crear sala")
        }
        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                    ErrorCard(uiState.errorMessage, onRetry)
                }
                uiState.rooms.isEmpty() -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("No hay salas todavía.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.rooms, key = { it.id }) { room ->
                        RoomRow(
                            room = room,
                            groupName = uiState.groupNames[room.groupId],
                            busy = uiState.isSubmitting,
                            onEndRoom = { onEndRoom(room.id) }
                        )
                    }
                }
            }
        }
        TeacherBottomBar(
            activeTab = TeacherTab.ROOMS,
            onThemesClick = onThemesClick,
            onStudentsClick = onStudentsClick,
            onWordCardsClick = onWordCardsClick,
            onRoomsClick = {}
        )
    }
}

@Composable
private fun RoomRow(
    room: LiveDomanSession,
    groupName: String?,
    busy: Boolean,
    onEndRoom: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(roomStatusLabel(room.status), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Text(groupName ?: "Grupo sin nombre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            room.repertoireName?.let { name ->
                Text(name, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (room.status in ACTIVE_ROOM_STATUSES) {
                OutlinedButton(
                    onClick = onEndRoom,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)
                ) { Text("Finalizar sala") }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) = Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.large) {
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
        ElevatedButton(onClick = onRetry, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text("Reintentar") }
    }
}

private fun roomStatusLabel(status: String): String = when (status) {
    "lobby" -> "SALA ABIERTA"
    "running" -> "PRESENTANDO"
    "paused" -> "EN PAUSA"
    else -> "FINALIZADA"
}
