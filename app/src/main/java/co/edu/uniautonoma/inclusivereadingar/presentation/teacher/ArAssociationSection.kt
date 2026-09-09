package co.edu.uniautonoma.inclusivereadingar.presentation.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.edu.uniautonoma.inclusivereadingar.domain.model.ArModelOption
import co.edu.uniautonoma.inclusivereadingar.domain.model.SUPPORTED_AR_MARKERS

@Composable
fun ArAssociationSection(
    models: List<ArModelOption>,
    selectedModelId: String?,
    selectedMarkerId: String?,
    onSelectModel: (String) -> Unit,
    onSelectMarker: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var markerMenuExpanded by remember { mutableStateOf(false) }
    val selectedModel = models.firstOrNull { it.learningUnitId == selectedModelId }
    val selectedMarker = SUPPORTED_AR_MARKERS.firstOrNull { it.markerId == selectedMarkerId }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Realidad aumentada (opcional)", fontWeight = FontWeight.Bold)
            Text(
                text = "Asocia un modelo 3D ya guardado con uno de los marcadores que reconoce la cámara.",
                color = Color(0xFF64748B),
                style = MaterialTheme.typography.bodySmall
            )

            if (models.isEmpty()) {
                Text(
                    text = "No hay modelos 3D disponibles en la base de datos.",
                    color = Color(0xFFB45309),
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { modelMenuExpanded = true },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = selectedModel?.let(::modelLabel)
                                ?: "Seleccionar modelo 3D"
                        )
                    }
                    DropdownMenu(
                        expanded = modelMenuExpanded,
                        onDismissRequest = { modelMenuExpanded = false }
                    ) {
                        models.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(modelLabel(model)) },
                                onClick = {
                                    onSelectModel(model.learningUnitId)
                                    modelMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { markerMenuExpanded = true },
                        enabled = enabled && selectedModel != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = selectedMarker?.let { "Marcador ${it.label}" }
                                ?: "Seleccionar marcador"
                        )
                    }
                    DropdownMenu(
                        expanded = markerMenuExpanded,
                        onDismissRequest = { markerMenuExpanded = false }
                    ) {
                        SUPPORTED_AR_MARKERS.forEach { marker ->
                            DropdownMenuItem(
                                text = { Text("Marcador ${marker.label}") },
                                onClick = {
                                    onSelectMarker(marker.markerId)
                                    markerMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedModel != null && selectedMarker != null) {
                    Text(
                        text = "Listo: ${selectedModel.word} aparecerá al enfocar ${selectedMarker.label}.",
                        color = Color(0xFF166534),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun modelLabel(model: ArModelOption): String {
    val fileName = model.model3dUrl.substringBefore('?').substringAfterLast('/')
        .ifBlank { "modelo 3D" }
    return "${model.word} · $fileName"
}
