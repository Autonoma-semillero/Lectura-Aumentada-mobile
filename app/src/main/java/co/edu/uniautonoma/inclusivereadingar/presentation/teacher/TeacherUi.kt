package co.edu.uniautonoma.inclusivereadingar.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class TeacherTab {
    THEMES,
    STUDENTS,
    WORD_CARDS
}

@Composable
fun TeacherBottomBar(
    activeTab: TeacherTab,
    onThemesClick: () -> Unit,
    onStudentsClick: () -> Unit,
    onWordCardsClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color.White,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            TeacherBottomBarItem(
                label = "Temáticas",
                selected = activeTab == TeacherTab.THEMES,
                onClick = onThemesClick
            )
            TeacherBottomBarItem(
                label = "Estudiantes",
                selected = activeTab == TeacherTab.STUDENTS,
                onClick = onStudentsClick
            )
            TeacherBottomBarItem(
                label = "Tarjetas",
                selected = activeTab == TeacherTab.WORD_CARDS,
                onClick = onWordCardsClick
            )
        }
    }
}

@Composable
private fun TeacherBottomBarItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = Color(0xFFE53734)
    val inactiveColor = Color(0xFF64748B)
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    color = if (selected) activeColor else Color.Transparent,
                    shape = CircleShape
                )
        )
        Text(
            text = label,
            color = if (selected) activeColor else inactiveColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

fun categoryIcon(iconName: String?): ImageVector = when (iconName) {
    "restaurant" -> Icons.Rounded.Restaurant
    "brush" -> Icons.Rounded.Brush
    "rocket_launch" -> Icons.Rounded.RocketLaunch
    "pets" -> Icons.Rounded.Pets
    "music_note" -> Icons.Rounded.MusicNote
    "sports_soccer" -> Icons.Rounded.SportsSoccer
    else -> Icons.Rounded.Category
}
