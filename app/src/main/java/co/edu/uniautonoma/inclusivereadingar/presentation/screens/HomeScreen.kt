package co.edu.uniautonoma.inclusivereadingar.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeRoute(
    onScanClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    HomeScreen(
        onScanClick = onScanClick,
        onProfileClick = onProfileClick
    )
}

@Composable
fun HomeScreen(
    onScanClick: () -> Unit,
    onProfileClick: () -> Unit,
    onProgressClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val primary = Color(0xFFE53734)
    val secondary = Color(0xFF4A90E2)
    val lightBackground = Color(0xFFF8F6F6)
    val darkBackground = Color(0xFF211111)

    val darkMode = false
    val config = LocalConfiguration.current
    val compactScreen = config.screenWidthDp < 360

    val pageBackground = if (darkMode) darkBackground else lightBackground
    val headerBackground = if (darkMode) darkBackground.copy(alpha = 0.5f) else Color.White
    val mainText = if (darkMode) Color.White else Color(0xFF0F172A)
    val secondaryText = if (darkMode) Color(0xFF94A3B8) else Color(0xFF475569)
    val navMuted = if (darkMode) Color(0xFF64748B) else Color(0xFF94A3B8)
    val cardSurface = if (darkMode) Color(0xFF1F2937) else Color.White
    val cardBorder = if (darkMode) Color(0xFF334155) else Color(0xFFF1F5F9)
    val navBorder = if (darkMode) Color(0xFF1F2937) else Color(0xFFE2E8F0)

    Scaffold(
        containerColor = pageBackground,
        topBar = {
            HomeTopBar(
                compact = compactScreen,
                darkMode = darkMode,
                primary = primary,
                mainText = mainText,
                background = headerBackground,
                onSettingsClick = onSettingsClick
            )
        },
        bottomBar = {
            HomeBottomBar(
                compact = compactScreen,
                darkMode = darkMode,
                navBorder = navBorder,
                navMuted = navMuted,
                primary = primary,
                background = darkBackground,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val ultraCompactWidth = maxWidth < 340.dp
            val compactWidth = maxWidth < 390.dp
            val shortHeight = maxHeight < 700.dp
            val wideLayout = maxWidth >= 580.dp

            val horizontalPadding = when {
                ultraCompactWidth -> 12.dp
                compactWidth -> 16.dp
                else -> 24.dp
            }
            val sectionGap = when {
                shortHeight -> 12.dp
                compactWidth -> 16.dp
                else -> 18.dp
            }
            val headlineSize = when {
                ultraCompactWidth -> 24.sp
                compactWidth -> 26.sp
                else -> 30.sp
            }
            val subtitleSize = when {
                ultraCompactWidth -> 14.sp
                compactWidth -> 15.sp
                else -> 18.sp
            }
            val cardTitleSize = when {
                ultraCompactWidth -> 19.sp
                compactWidth -> 21.sp
                else -> 24.sp
            }
            val iconContainerSize = when {
                ultraCompactWidth -> 84.dp
                compactWidth -> 92.dp
                else -> 104.dp
            }
            val cardIconSize = when {
                ultraCompactWidth -> 48.dp
                compactWidth -> 52.dp
                else -> 60.dp
            }
            val summaryGap = if (compactWidth) 8.dp else 12.dp
            val contentVerticalPadding = when {
                shortHeight -> 10.dp
                else -> 18.dp
            }
            val cardGap = if (compactWidth) 12.dp else 14.dp
            val summaryHeightEstimate = when {
                ultraCompactWidth -> 124.dp
                compactWidth -> 80.dp
                else -> 88.dp
            }
            val greetingHeightEstimate = when {
                compactWidth -> 78.dp
                else -> 92.dp
            }
            val reservedHeight =
                greetingHeightEstimate + summaryHeightEstimate + (sectionGap * 2) + (contentVerticalPadding * 2) + cardGap
            val minCardHeight = when {
                ultraCompactWidth -> 130.dp
                compactWidth -> 144.dp
                else -> 158.dp
            }
            val maxCardHeight = if (wideLayout) 250.dp else 232.dp
            val rawCardHeight = (maxHeight - reservedHeight) / 2
            val cardHeight = rawCardHeight.coerceIn(minCardHeight, maxCardHeight)
            val shouldScroll = rawCardHeight < minCardHeight
            val useStackedSummary = ultraCompactWidth

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding),
                contentAlignment = if (shortHeight) Alignment.TopCenter else Alignment.Center
            ) {
                val contentModifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = if (wideLayout) 540.dp else 512.dp)
                    .then(
                        if (shouldScroll) {
                            Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = contentVerticalPadding)
                        } else {
                            Modifier
                                .fillMaxHeight()
                                .padding(vertical = contentVerticalPadding)
                        }
                    )

                Column(
                    modifier = contentModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (shouldScroll) {
                        Arrangement.spacedBy(sectionGap)
                    } else {
                        Arrangement.SpaceBetween
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(if (compactWidth) 6.dp else 8.dp)
                    ) {
                        Text(
                            text = "¡Hola, valiente!",
                            color = mainText,
                            fontSize = headlineSize,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "¿Qué quieres aprender hoy?",
                            color = secondaryText,
                            fontSize = subtitleSize,
                            textAlign = TextAlign.Center
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(cardGap)
                    ) {
                        LargeActionCard(
                            backgroundColor = primary,
                            icon = Icons.Rounded.PhotoCamera,
                            title = "Escanear Palabra",
                            height = cardHeight,
                            iconContainerSize = iconContainerSize,
                            iconSize = cardIconSize,
                            titleFontSize = cardTitleSize,
                            onClick = onScanClick
                        )
                        LargeActionCard(
                            backgroundColor = secondary,
                            icon = Icons.Rounded.Star,
                            title = "Mi Progreso",
                            height = cardHeight,
                            iconContainerSize = iconContainerSize,
                            iconSize = cardIconSize,
                            titleFontSize = cardTitleSize,
                            onClick = onProgressClick
                        )
                    }

                    if (useStackedSummary) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryCard(
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Rounded.History,
                                iconColor = primary,
                                title = "Reciente",
                                value = "Casa",
                                compact = true,
                                background = cardSurface,
                                border = cardBorder,
                                titleColor = secondaryText,
                                valueColor = mainText
                            )
                            SummaryCard(
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Rounded.EmojiEvents,
                                iconColor = secondary,
                                title = "Logros",
                                value = "5 Palabras",
                                compact = true,
                                background = cardSurface,
                                border = cardBorder,
                                titleColor = secondaryText,
                                valueColor = mainText
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(summaryGap)
                        ) {
                            SummaryCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.History,
                                iconColor = primary,
                                title = "Reciente",
                                value = "Casa",
                                compact = compactWidth,
                                background = cardSurface,
                                border = cardBorder,
                                titleColor = secondaryText,
                                valueColor = mainText
                            )
                            SummaryCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.EmojiEvents,
                                iconColor = secondary,
                                title = "Logros",
                                value = "5 Palabras",
                                compact = compactWidth,
                                background = cardSurface,
                                border = cardBorder,
                                titleColor = secondaryText,
                                valueColor = mainText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    compact: Boolean,
    darkMode: Boolean,
    primary: Color,
    mainText: Color,
    background: Color,
    onSettingsClick: () -> Unit
) {
    val horizontalPadding = if (compact) 16.dp else 24.dp
    val verticalPadding = if (compact) 14.dp else 18.dp
    val logoSize = if (compact) 44.dp else 48.dp
    val logoIconSize = if (compact) 28.dp else 32.dp
    val titleSize = if (compact) 18.sp else 20.sp
    val settingsSize = if (compact) 44.dp else 48.dp

    Surface(
        modifier = Modifier.statusBarsPadding(),
        color = background,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(logoSize)
                        .clip(RoundedCornerShape(18.dp))
                        .background(primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoStories,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(logoIconSize)
                    )
                }

                Text(
                    text = "Lectura Inclusiva",
                    color = mainText,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(settingsSize)
                    .clip(CircleShape)
                    .background(if (darkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Configuración",
                    tint = if (darkMode) Color(0xFFCBD5E1) else Color(0xFF475569),
                    modifier = Modifier.size(if (compact) 22.dp else 24.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeBottomBar(
    compact: Boolean,
    darkMode: Boolean,
    navBorder: Color,
    navMuted: Color,
    primary: Color,
    background: Color,
    onProfileClick: () -> Unit
) {
    Surface(
        modifier = Modifier.navigationBarsPadding(),
        color = if (darkMode) background else Color.White,
        border = BorderStroke(width = 1.dp, color = navBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .padding(
                        horizontal = if (compact) 18.dp else 24.dp,
                        vertical = if (compact) 10.dp else 14.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    modifier = Modifier.weight(1f),
                    label = "Inicio",
                    icon = Icons.Rounded.Home,
                    selected = true,
                    compact = compact,
                    selectedColor = primary,
                    unselectedColor = navMuted,
                    onClick = {}
                )
                BottomNavItem(
                    modifier = Modifier.weight(1f),
                    label = "Librería",
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    selected = false,
                    compact = compact,
                    selectedColor = primary,
                    unselectedColor = navMuted,
                    onClick = {}
                )
                BottomNavItem(
                    modifier = Modifier.weight(1f),
                    label = "Perfil",
                    icon = Icons.Rounded.Person,
                    selected = false,
                    compact = compact,
                    selectedColor = primary,
                    unselectedColor = navMuted,
                    onClick = onProfileClick
                )
            }
        }
    }
}

@Composable
private fun LargeActionCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    icon: ImageVector,
    title: String,
    height: androidx.compose.ui.unit.Dp,
    iconContainerSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    titleFontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = backgroundColor,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(iconContainerSize)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = titleFontSize,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    compact: Boolean,
    background: Color,
    border: Color,
    titleColor: Color,
    valueColor: Color
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .border(width = 1.5.dp, color = border, shape = RoundedCornerShape(24.dp))
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 8.dp else 10.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(if (compact) 20.dp else 24.dp)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = titleColor,
                fontSize = if (compact) 9.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                maxLines = 1
            )
            Text(
                text = value,
                color = valueColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (compact) 12.sp else 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    compact: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit
) {
    val color = if (selected) selectedColor else unselectedColor
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val itemBackground = when {
        selected -> selectedColor.copy(alpha = 0.14f)
        isPressed -> selectedColor.copy(alpha = 0.09f)
        else -> Color.Transparent
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(if (compact) 12.dp else 14.dp))
            .background(itemBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(if (compact) 24.dp else 28.dp)
        )
        Text(
            text = label,
            color = color,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
