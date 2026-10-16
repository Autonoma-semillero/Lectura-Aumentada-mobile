package co.edu.uniautonoma.inclusivereadingar.presentation.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.LiveStudentRoomUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.LiveStudentRoomViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.LiveStudentRoomViewModelFactory

@Composable
fun LiveStudentRoomRoute(onBackClick: () -> Unit) {
    val container = LocalContext.current.appContainer()
    val viewModel: LiveStudentRoomViewModel = viewModel(factory = LiveStudentRoomViewModelFactory(container.liveDomanRepository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LiveStudentRoomScreen(uiState, viewModel::refresh, viewModel::toggleLocalPause, onBackClick)
}

@Composable
fun LiveStudentRoomScreen(uiState: LiveStudentRoomUiState, onRetry: () -> Unit, onToggleLocalPause: () -> Unit, onBackClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.errorMessage != null -> RoomMessage("No pudimos abrir la clase", "Estamos intentando volver a conectar.", "Intentar de nuevo", onRetry)
            uiState.session == null -> EmptyRoom(onBackClick, uiState.isRealtimeConnected)
            else -> StudentClassRoom(uiState, onToggleLocalPause, onBackClick)
        }
    }
}

@Composable
private fun RoomMessage(title: String, message: String, actionLabel: String, onAction: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(message, modifier = Modifier.padding(top = 12.dp, bottom = 28.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Button(onClick = onAction, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text(actionLabel) }
    }
}

@Composable
private fun EmptyRoom(onBackClick: () -> Unit, isRealtimeConnected: Boolean) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Aún no hay clase", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(if (isRealtimeConnected) "Cuando tu profe abra la clase, aparecerá aquí." else "Estamos reconectando para avisarte cuando comience la clase.", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        OutlinedButton(onClick = onBackClick, modifier = Modifier.padding(top = 28.dp).fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text("Volver al inicio") }
    }
}

@Composable
private fun StudentClassRoom(uiState: LiveStudentRoomUiState, onToggleLocalPause: () -> Unit, onBackClick: () -> Unit) {
    val session = requireNotNull(uiState.session)
    val showingCard = session.status == "running" && !session.displayText.isNullOrBlank() && !uiState.isLocallyPaused
    val instruction = when {
        uiState.isLocallyPaused -> "La lectura está pausada en tu pantalla."
        session.status == "paused" -> "Tu profe pausó la clase."
        showingCard -> "Mira la palabra"
        else -> "Espera a tu profe."
    }
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("En clase", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(if (uiState.isRealtimeConnected) "Clase en vivo" else "Sincronización automática", modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelLarge, color = if (uiState.isRealtimeConnected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(instruction, modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (showingCard) {
                Surface(modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 3.dp) {
                    Text(session.displayText.orEmpty(), modifier = Modifier.padding(horizontal = 24.dp, vertical = 56.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 44.sp, lineHeight = 52.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                }
            }
        }
        if (session.status == "running") {
            OutlinedButton(onClick = onToggleLocalPause, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text(if (uiState.isLocallyPaused) "Reanudar lectura" else "Pausar lectura") }
        }
        OutlinedButton(onClick = onBackClick, modifier = Modifier.padding(top = 8.dp).fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text("Volver al inicio") }
    }
}
