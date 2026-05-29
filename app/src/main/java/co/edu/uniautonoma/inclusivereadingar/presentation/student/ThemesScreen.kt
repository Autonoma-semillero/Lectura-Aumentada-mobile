package co.edu.uniautonoma.inclusivereadingar.presentation.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.domain.model.CategorySummary
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.ThemesUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.ThemesViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.ThemesViewModelFactory

@Composable
fun ThemesRoute(
    onThemeClick: (CategorySummary) -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: ThemesViewModel = viewModel(
        factory = ThemesViewModelFactory(container.studentContentRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ThemesScreen(
        uiState = uiState,
        onThemeClick = onThemeClick,
        onRetry = viewModel::loadThemes,
        onLogoutClick = onLogoutClick
    )
}

@Composable
fun ThemesScreen(
    uiState: ThemesUiState,
    onThemeClick: (CategorySummary) -> Unit,
    onRetry: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val cards = uiState.categories

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F7))
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            color = Color.White,
            shadowElevation = 3.dp
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFE53734).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoStories,
                            contentDescription = null,
                            tint = Color(0xFFE53734)
                        )
                    }
                    Column {
                        Text(
                            text = "Temas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                        Text(
                            text = "Elige una tematica para iniciar la sesion",
                            color = Color(0xFF64748B)
                        )
                    }
                }
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .clickable(onClick = onLogoutClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                        contentDescription = "Cerrar sesión",
                        tint = Color(0xFFE53734),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Salir",
                        color = Color(0xFFE53734),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFE53734))
                }
            }

            !uiState.errorMessage.isNullOrBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = uiState.errorMessage,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Toca para intentar de nuevo",
                                modifier = Modifier.clickable(onClick = onRetry),
                                color = Color(0xFFE53734),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            cards.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "No hay tarjetas disponibles para hoy",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Cierra sesion y vuelve a ingresar para sincronizar tus tarjetas del dia.",
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Toca para intentar de nuevo",
                            modifier = Modifier.clickable(onClick = onRetry),
                            color = Color(0xFFE53734),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(cards, key = { it.id }) { category ->
                        ThemeCard(
                            category = category,
                            icon = iconForCategory(category.slug),
                            onClick = { onThemeClick(category) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    category: CategorySummary,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val accent = accentForCategory(category.slug)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(42.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = category.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${category.availableWordCardsCount} palabras",
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun iconForCategory(slug: String): ImageVector {
    return when {
        slug.contains("cocin") -> Icons.Rounded.Restaurant
        slug.contains("famil") -> Icons.Rounded.FamilyRestroom
        slug.contains("anim") -> Icons.Rounded.Pets
        slug.contains("natur") -> Icons.Rounded.Forest
        slug.contains("jugu") -> Icons.Rounded.SportsEsports
        else -> Icons.Rounded.AutoStories
    }
}

private fun accentForCategory(slug: String): Color {
    return when {
        slug.contains("cocin") -> Color(0xFFE53734)
        slug.contains("famil") -> Color(0xFF4A90E2)
        slug.contains("anim") -> Color(0xFF16A34A)
        slug.contains("natur") -> Color(0xFFB45309)
        slug.contains("jugu") -> Color(0xFF7C3AED)
        else -> Color(0xFFE53734)
    }
}

