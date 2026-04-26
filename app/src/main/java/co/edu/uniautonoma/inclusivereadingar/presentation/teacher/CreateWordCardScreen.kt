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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.domain.model.AppUser
import co.edu.uniautonoma.inclusivereadingar.domain.model.Category
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.CreateWordCardUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.CreateWordCardViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.CreateWordCardViewModelFactory

@Composable
fun CreateWordCardRoute(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: CreateWordCardViewModel = viewModel(
        factory = CreateWordCardViewModelFactory(container.teacherContentRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            onBack()
        }
    }

    CreateWordCardScreen(
        uiState = uiState,
        onBack = onBack,
        onSelectStudent = viewModel::selectStudent,
        onWordChange = viewModel::updateWord,
        onSelectCategory = viewModel::selectCategory,
        onAudioUrlChange = viewModel::updateAudioUrl,
        onSaveClick = viewModel::save
    )
}

@Composable
fun CreateWordCardScreen(
    uiState: CreateWordCardUiState,
    onBack: () -> Unit,
    onSelectStudent: (String) -> Unit,
    onWordChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onAudioUrlChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    val primary = Color(0xFFE53734)
    var studentMenuExpanded by remember { mutableStateOf(false) }

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
                text = "Registrar Nueva Tarjeta",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            )
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = primary
                )
            }
        }

        if (uiState.isLoadingStudents || uiState.isLoadingCategories) {
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
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Selecciona el Estudiante",
                    fontWeight = FontWeight.Bold
                )
                StudentDropdown(
                    students = uiState.students,
                    selectedStudentId = uiState.selectedStudentId,
                    expanded = studentMenuExpanded,
                    onExpandedChange = { studentMenuExpanded = it },
                    onSelectStudent = {
                        onSelectStudent(it)
                        studentMenuExpanded = false
                    }
                )

                Text(
                    text = "Detalles de la Palabra",
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = uiState.word,
                    onValueChange = onWordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Palabra") },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primary
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primary,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Text(
                    text = "Selecciona el Tema",
                    fontWeight = FontWeight.Bold
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(220.dp)
                ) {
                    items(uiState.categories, key = { it.id }) { category ->
                        val selected = uiState.selectedCategoryId == category.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .clickable { onSelectCategory(category.id) },
                            shape = RoundedCornerShape(22.dp),
                            color = if (selected) primary.copy(alpha = 0.12f) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                if (selected) primary else Color(0xFFE2E8F0)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = categoryIcon(category.icon),
                                    contentDescription = null,
                                    tint = if (selected) primary else Color(0xFF64748B)
                                )
                                Text(
                                    text = category.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.audioUrl,
                    onValueChange = onAudioUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL de pronunciación (opcional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primary,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Estado",
                            fontWeight = FontWeight.Bold
                        )
                        StatusRow(
                            label = "Palabra escrita",
                            done = uiState.word.isNotBlank()
                        )
                        StatusRow(
                            label = "Categoría elegida",
                            done = uiState.selectedCategoryId != null
                        )
                        StatusRow(
                            label = "Estudiante seleccionado",
                            done = uiState.selectedStudentId != null
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
                    enabled = uiState.canSave && !uiState.isSaving,
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
                            text = "Guardar Tarjeta",
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
            activeTab = TeacherTab.WORD_CARDS,
            onThemesClick = onBack,
            onStudentsClick = onBack,
            onWordCardsClick = {}
        )
    }
}

@Composable
private fun StudentDropdown(
    students: List<AppUser>,
    selectedStudentId: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectStudent: (String) -> Unit
) {
    val selectedStudent = students.firstOrNull { it.id == selectedStudentId }
    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(true) },
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Text(
                text = selectedStudent?.displayName ?: selectedStudent?.email ?: "Selecciona un estudiante",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                color = if (selectedStudent == null) Color(0xFF64748B) else Color(0xFF111827)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            students.forEach { student ->
                DropdownMenuItem(
                    text = {
                        Text(student.displayName ?: student.email)
                    },
                    onClick = { onSelectStudent(student.id) }
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, done: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (done) {
                Icons.Rounded.CheckCircle
            } else {
                Icons.Rounded.RadioButtonUnchecked
            },
            contentDescription = null,
            tint = if (done) Color(0xFF16A34A) else Color(0xFF94A3B8)
        )
        Text(
            text = label,
            color = if (done) Color(0xFF166534) else Color(0xFF475569),
            fontWeight = if (done) FontWeight.Bold else FontWeight.Medium
        )
    }
}
