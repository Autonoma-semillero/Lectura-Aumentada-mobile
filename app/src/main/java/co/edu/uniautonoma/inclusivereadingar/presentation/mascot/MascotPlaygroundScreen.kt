package co.edu.uniautonoma.inclusivereadingar.presentation.mascot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val eventLabels = listOf(
    MascotEvent.Wave to "Saludar",
    MascotEvent.Celebrate to "Celebrar",
    MascotEvent.Encourage to "Dar ánimo",
    MascotEvent.TryAgain to "Intentar otra vez",
    MascotEvent.Think to "Pensar",
    MascotEvent.Idle to "Reposo",
)

private val expressionLabels = listOf(
    MascotExpression.Neutral to "Neutral",
    MascotExpression.Happy to "Feliz",
    MascotExpression.Curious to "Curioso",
    MascotExpression.Encouraging to "Ánimo",
    MascotExpression.Empathic to "Empático",
)

private val cueLabels = listOf(
    MascotCue.ScreenStarted to "Inicio de pantalla",
    MascotCue.AnswerCorrect to "Respuesta correcta",
    MascotCue.AnswerIncorrect to "Respuesta incorrecta",
    MascotCue.LessonCompleted to "Lección completada",
    MascotCue.Thinking to "Pensando",
)

/** Debug screen to trigger every mascot animation, expression and gaze by hand. */
@Composable
fun MascotPlaygroundRoute(onBackClick: () -> Unit) {
    val viewModel: MascotViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MascotPlaygroundScreen(
        state = state,
        controller = viewModel.controller,
        onEvent = viewModel::play,
        onExpression = viewModel::setExpression,
        onCue = viewModel::onCue,
        onLook = viewModel::lookAt,
        onBackClick = onBackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MascotPlaygroundScreen(
    state: MascotUiState,
    controller: MascotController,
    onEvent: (MascotEvent) -> Unit,
    onExpression: (MascotExpression) -> Unit,
    onCue: (MascotCue) -> Unit,
    onLook: (Float, Float) -> Unit,
    onBackClick: () -> Unit,
) {
    var paused by remember { mutableStateOf(false) }
    var lookX by remember { mutableFloatStateOf(0f) }
    var lookY by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBackClick) { Text("Volver") }
            Text("Mascot Playground", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        MascotaRive(
            controller = controller,
            paused = paused,
            modifier = Modifier.fillMaxWidth().height(320.dp),
        )

        Text("Última acción: ${state.lastEvent?.name ?: "—"}", style = MaterialTheme.typography.bodyMedium)

        Section("Animaciones")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            eventLabels.forEach { (event, label) ->
                Button(onClick = { onEvent(event) }) { Text(label) }
            }
        }

        Section("Expresión")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            expressionLabels.forEach { (expression, label) ->
                FilterChip(
                    selected = state.expression == expression,
                    onClick = { onExpression(expression) },
                    label = { Text(label) },
                )
            }
        }

        Section("Mirada")
        Slider(value = lookX, onValueChange = { lookX = it; onLook(lookX, lookY) }, valueRange = -1f..1f)
        Slider(value = lookY, onValueChange = { lookY = it; onLook(lookX, lookY) }, valueRange = -1f..1f)

        Section("Eventos de aprendizaje")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cueLabels.forEach { (cue, label) ->
                OutlinedButton(onClick = { onCue(cue) }) { Text(label) }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pausar animación", modifier = Modifier.weight(1f))
            Switch(checked = paused, onCheckedChange = { paused = it })
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}
