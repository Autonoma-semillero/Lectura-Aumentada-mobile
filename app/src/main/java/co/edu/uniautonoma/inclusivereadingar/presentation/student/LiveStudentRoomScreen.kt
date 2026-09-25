package co.edu.uniautonoma.inclusivereadingar.presentation.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.domain.model.LiveDomanSession
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.LiveStudentRoomUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.LiveStudentRoomViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.LiveStudentRoomViewModelFactory

@Composable
fun LiveStudentRoomRoute(onLogoutClick: () -> Unit) {
    val container = LocalContext.current.appContainer()
    val viewModel: LiveStudentRoomViewModel = viewModel(
        factory = LiveStudentRoomViewModelFactory(container.liveDomanRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LiveStudentRoomScreen(uiState, viewModel::refresh, onLogoutClick)
}

@Composable
fun LiveStudentRoomScreen(
    uiState: LiveStudentRoomUiState,
    onRefresh: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            uiState.isLoading -> LoadingRoom()
            uiState.errorMessage != null -> RoomMessage(
                title = "No pudimos abrir la clase",
                message = "Pide ayuda a un adulto e intenta otra vez.",
                actionLabel = "Intentar de nuevo",
                onAction = onRefresh
            )
            uiState.session == null -> RoomMessage(
                title = "Aún no hay clase",
                message = "Cuando tu profe la inicie, aparecerá aquí.",
                actionLabel = "Buscar clase",
                onAction = onRefresh
            )
            else -> StudentClassRoom(uiState.session, onRefresh, onLogoutClick)
        }
    }
}

@Composable
private fun LoadingRoom() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
}

@Composable
private fun RoomMessage(title: String, message: String, actionLabel: String, onAction: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(
            message,
            modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onAction, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun StudentClassRoom(session: LiveDomanSession, onRefresh: () -> Unit, onLogoutClick: () -> Unit) {
    val showingCard = session.status == "running" && !session.displayText.isNullOrBlank()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("En clase", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            if (showingCard) "Mira la palabra" else "Espera a tu profe.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (showingCard) {
                Surface(
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 3.dp
                ) {
                    Text(
                        text = session.displayText.orEmpty(),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 56.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 44.sp,
                        lineHeight = 52.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) {
            Text("Actualizar")
        }
        androidx.compose.material3.TextButton(
            onClick = onLogoutClick,
            modifier = Modifier.padding(top = 4.dp).sizeIn(minHeight = 48.dp)
        ) { Text("Salir") }
    }
}
