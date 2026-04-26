package co.edu.uniautonoma.inclusivereadingar.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.WordCardCategoryListViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.WordCardCategoryListViewModelFactory

@Composable
fun WordCardCategoryListRoute(
    onBack: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    onThemesClick: () -> Unit,
    onStudentsClick: () -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: WordCardCategoryListViewModel = viewModel(
        factory = WordCardCategoryListViewModelFactory(container.teacherContentRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WordCardCategoryListScreen(
        isLoading = uiState.isLoading,
        categories = uiState.categories,
        errorMessage = uiState.errorMessage,
        onBack = onBack,
        onCategoryClick = onCategoryClick,
        onRetry = viewModel::load,
        onThemesClick = onThemesClick,
        onStudentsClick = onStudentsClick,
        onWordCardsClick = {}
    )
}

@Composable
fun WordCardCategoryListScreen(
    isLoading: Boolean,
    categories: List<Category>,
    errorMessage: String?,
    onBack: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    onRetry: () -> Unit,
    onThemesClick: () -> Unit,
    onStudentsClick: () -> Unit,
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
                text = "Gestionar Tarjetas",
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

        when {
            isLoading -> {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primary)
                }
            }

            !errorMessage.isNullOrBlank() -> {
                Box(
                    modifier = Modifier.weight(1f).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF64748B)
                        )
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = primary)
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            }

            categories.isEmpty() -> {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No hay temáticas creadas.\nCrea una desde la pestaña Temáticas.",
                        textAlign = TextAlign.Center,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                ) {
                    items(categories, key = { it.id }) { category ->
                        CategoryCardItem(
                            category = category,
                            onClick = { onCategoryClick(category) }
                        )
                    }
                }
            }
        }

        TeacherBottomBar(
            activeTab = TeacherTab.WORD_CARDS,
            onThemesClick = onThemesClick,
            onStudentsClick = onStudentsClick,
            onWordCardsClick = onWordCardsClick
        )
    }
}

@Composable
private fun CategoryCardItem(
    category: Category,
    onClick: () -> Unit
) {
    val primary = Color(0xFFE53734)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(category.icon),
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF111827)
                )
                val countLabel = when (category.wordCardsCount) {
                    0 -> "Sin tarjetas"
                    1 -> "1 tarjeta"
                    else -> "${category.wordCardsCount} tarjetas"
                }
                Text(
                    text = countLabel,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
