package co.edu.uniautonoma.inclusivereadingar.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.CreateEditThemeUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.CreateEditThemeViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.CreateEditThemeViewModelFactory

private val TeacherThemeIcons = listOf(
    "restaurant",
    "brush",
    "rocket_launch",
    "pets",
    "music_note",
    "sports_soccer"
)

@Composable
fun CreateEditThemeRoute(
    themeId: String?,
    onBack: () -> Unit,
    onWordCardsClick: () -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: CreateEditThemeViewModel = viewModel(
        factory = CreateEditThemeViewModelFactory(themeId, container.teacherContentRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            onBack()
        }
    }

    CreateEditThemeScreen(
        themeId = themeId,
        uiState = uiState,
        onBack = onBack,
        onNameChange = viewModel::updateName,
        onIconChange = viewModel::updateIcon,
        onSortOrderChange = viewModel::updateSortOrder,
        onDescriptionChange = viewModel::updateDescription,
        onSaveClick = viewModel::save,
        onWordCardsClick = onWordCardsClick
    )
}

@Composable
fun CreateEditThemeScreen(
    themeId: String?,
    uiState: CreateEditThemeUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onSortOrderChange: (Int) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onWordCardsClick: () -> Unit
) {
    val primary = Color(0xFFE53734)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F7))
    ) {
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
            Text(
                text = if (themeId == null) "Nueva Temática" else "Editar Temática",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )
            Box(modifier = Modifier.size(48.dp))
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre") },
                    shape = RoundedCornerShape(20.dp),
                    colors = teacherFieldColors()
                )

                Text(
                    text = "Selecciona un icono",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(TeacherThemeIcons) { iconName ->
                        val selected = uiState.icon == iconName
                        Surface(
                            modifier = Modifier
                                .size(80.dp)
                                .clickable { onIconChange(iconName) },
                            shape = RoundedCornerShape(22.dp),
                            color = if (selected) primary.copy(alpha = 0.12f) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 2.dp,
                                color = if (selected) primary else Color(0xFFE2E8F0)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = categoryIcon(iconName),
                                    contentDescription = null,
                                    tint = if (selected) primary else Color(0xFF64748B),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.sortOrder.toString(),
                    onValueChange = { raw -> onSortOrderChange(raw.toIntOrNull() ?: 0) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Orden visual") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(20.dp),
                    colors = teacherFieldColors()
                )

                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Descripción") },
                    minLines = 3,
                    shape = RoundedCornerShape(20.dp),
                    colors = teacherFieldColors()
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = primary.copy(alpha = 0.08f)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Consejo del Docente",
                            fontWeight = FontWeight.Bold,
                            color = primary
                        )
                        Text(
                            text = "Usa nombres cortos y claros para que el estudiante identifique la temática rápidamente.",
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                if (!uiState.errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                Button(
                    onClick = onSaveClick,
                    enabled = !uiState.isSaving && uiState.name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Guardar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }

                Text(
                    text = "Cancelar",
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(onClick = onBack)
                        .padding(vertical = 8.dp)
                )
            }
        }

        TeacherBottomBar(
            activeTab = TeacherTab.THEMES,
            onThemesClick = onBack,
            onStudentsClick = onBack,
            onWordCardsClick = onWordCardsClick
        )
    }
}

@Composable
private fun teacherFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFFE53734),
    unfocusedBorderColor = Color(0xFFE2E8F0),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)
