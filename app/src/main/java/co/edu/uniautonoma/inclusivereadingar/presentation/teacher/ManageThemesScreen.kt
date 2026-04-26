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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.domain.model.Category
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.ManageThemesUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.ManageThemesViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.ManageThemesViewModelFactory

@Composable
fun ManageThemesRoute(
    onBack: () -> Unit,
    onCreateThemeClick: () -> Unit,
    onEditThemeClick: (String) -> Unit,
    onWordCardsClick: () -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: ManageThemesViewModel = viewModel(
        factory = ManageThemesViewModelFactory(container.teacherContentRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ManageThemesScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::load,
        onCreateThemeClick = onCreateThemeClick,
        onEditThemeClick = onEditThemeClick,
        onDeleteThemeClick = viewModel::requestDelete,
        onDeleteConfirm = viewModel::confirmDelete,
        onDeleteDismiss = viewModel::cancelDelete,
        onWordCardsClick = onWordCardsClick
    )
}

@Composable
fun ManageThemesScreen(
    uiState: ManageThemesUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCreateThemeClick: () -> Unit,
    onEditThemeClick: (String) -> Unit,
    onDeleteThemeClick: (String) -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onWordCardsClick: () -> Unit
) {
    val primary = Color(0xFFE53734)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F7))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    text = "Gestionar Temáticas",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                )
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = primary
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primary)
                    }
                }

                !uiState.errorMessage.isNullOrBlank() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = uiState.errorMessage,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Toca para intentar de nuevo",
                                    modifier = Modifier.clickable(onClick = onRetry),
                                    color = primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.categories, key = { it.id }) { category ->
                            TeacherThemeCard(
                                category = category,
                                onEditClick = { onEditThemeClick(category.id) },
                                onDeleteClick = { onDeleteThemeClick(category.id) }
                            )
                        }
                    }
                }
            }

            TeacherBottomBar(
                activeTab = TeacherTab.THEMES,
                onThemesClick = {},
                onStudentsClick = {},
                onWordCardsClick = onWordCardsClick
            )
        }

        FloatingActionButton(
            onClick = onCreateThemeClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 92.dp),
            containerColor = primary,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Nueva temática"
            )
        }
    }

    if (uiState.deleteConfirmId != null) {
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = { Text("Eliminar temática") },
            text = { Text("Esta acción eliminará la temática si no tiene referencias activas.") },
            confirmButton = {
                TextButton(onClick = onDeleteConfirm) {
                    Text("Eliminar", color = primary)
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun TeacherThemeCard(
    category: Category,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val primary = Color(0xFFE53734)
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(category.icon),
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE0F2FE)
                        ) {
                            Text(
                                text = "Orden: ${category.sortOrder}",
                                color = Color(0xFF0369A1),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "${category.wordCardsCount} tarjetas",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (!category.description.isNullOrBlank()) {
                Text(
                    text = category.description,
                    color = Color(0xFF475569)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onEditClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A90E2),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("Editar")
                }
                Button(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFEE2E2),
                        contentColor = primary
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("Eliminar")
                }
            }
        }
    }
}
