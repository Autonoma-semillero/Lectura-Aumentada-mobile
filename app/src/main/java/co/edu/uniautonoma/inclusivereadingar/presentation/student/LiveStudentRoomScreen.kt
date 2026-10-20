package co.edu.uniautonoma.inclusivereadingar.presentation.student

import android.content.pm.PackageManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.R
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.LiveStudentRoomUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.LiveStudentRoomViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.LiveStudentRoomViewModelFactory
import co.edu.uniautonoma.inclusivereadingar.presentation.theme.ChildYellow
import co.edu.uniautonoma.inclusivereadingar.presentation.theme.InfoTeal
import co.edu.uniautonoma.inclusivereadingar.presentation.theme.OnChildYellow
import co.edu.uniautonoma.inclusivereadingar.presentation.theme.SecondaryBlue
import co.edu.uniautonoma.inclusivereadingar.presentation.theme.SuccessGreen
import co.edu.uniautonoma.inclusivereadingar.presentation.theme.LearningTokens
import co.edu.uniautonoma.inclusivereadingar.presentation.webar.WebArContainer
import co.edu.uniautonoma.inclusivereadingar.presentation.webar.shouldMountArWebView

@Composable
fun LiveStudentRoomRoute(onBackClick: () -> Unit) {
    val container = LocalContext.current.appContainer()
    val viewModel: LiveStudentRoomViewModel = viewModel(factory = LiveStudentRoomViewModelFactory(container.liveDomanRepository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LiveStudentRoomScreen(uiState, viewModel::refresh, viewModel::toggleLocalPause, onBackClick)
}

@Composable
fun LiveStudentRoomScreen(uiState: LiveStudentRoomUiState, onRetry: () -> Unit, onToggleLocalPause: () -> Unit, onBackClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.errorMessage != null -> RoomMessage("No pudimos abrir la clase", "Estamos intentando volver a conectar.", "Intentar de nuevo", onRetry)
            uiState.session == null -> EmptyRoom(uiState.isRealtimeConnected)
            else -> StudentClassRoom(uiState, onToggleLocalPause, onBackClick)
        }
    }
}

@Composable
private fun RoomMessage(title: String, message: String, actionLabel: String, onAction: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(message, modifier = Modifier.padding(top = 12.dp, bottom = 28.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Button(onClick = onAction, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text(actionLabel) }
    }
}

@Composable
private fun EmptyRoom(isRealtimeConnected: Boolean) {
    val statusLabel = if (isRealtimeConnected) {
        "Tu profe está preparando la clase."
    } else {
        "Estamos reconectando para avisarte cuando comience la clase."
    }

    val drift = rememberInfiniteTransition(label = "emptyRoomDrift")
    val cloudOffset by drift.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "cloudOffset",
    )
    val cloudOffset2 by drift.animateFloat(
        initialValue = 10f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "cloudOffset2",
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics {},
        ) {
            Box(
                modifier = Modifier
                    .offset(x = (24 + cloudOffset).dp, y = 72.dp)
                    .size(96.dp, 48.dp)
                    .clip(CircleShape)
                    .background(SecondaryBlue.copy(alpha = 0.18f)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-24 + cloudOffset2).dp, y = 140.dp)
                    .size(72.dp, 36.dp)
                    .clip(CircleShape)
                    .background(SecondaryBlue.copy(alpha = 0.14f)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset(y = 140.dp)
                    .size(screenWidth * 1.46f)
                    .clip(CircleShape)
                    .background(ChildYellow.copy(alpha = 0.55f)),
            )
            FloatingLetter(letter = "A", color = SecondaryBlue, alignment = Alignment.TopStart, sizeDp = screenWidth * 0.154f, offsetDp = Pair(56, 150), periodMs = 3200)
            FloatingLetter(letter = "B", color = ChildYellow, alignment = Alignment.TopEnd, sizeDp = screenWidth * 0.144f, offsetDp = Pair(-48, 210), periodMs = 3600)
            FloatingLetter(letter = "C", color = InfoTeal, alignment = Alignment.CenterStart, sizeDp = screenWidth * 0.133f, offsetDp = Pair(32, 90), periodMs = 4000)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = LearningTokens.ScreenGutter, vertical = LearningTokens.SectionGap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatusBadge(isRealtimeConnected = isRealtimeConnected, sizeDp = screenWidth * 0.174f)

            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                val breathe = rememberInfiniteTransition(label = "mascotBreathe")
                val breatheScale by breathe.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.03f,
                    animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                    label = "mascotBreatheScale",
                )
                Image(
                    painter = painterResource(R.drawable.mascot_curious),
                    contentDescription = "Mascota pensando mientras espera a que el profe abra la clase",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(screenWidth * 0.646f).scale(breatheScale),
                )
            }

            BouncingDots(
                modifier = Modifier.padding(bottom = LearningTokens.SectionGap),
                statusDescription = statusLabel,
            )
        }
    }
}

@Composable
private fun StatusBadge(isRealtimeConnected: Boolean, sizeDp: androidx.compose.ui.unit.Dp) {
    if (!isRealtimeConnected) return
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(SuccessGreen)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "¡Ya estás en la sala!"
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(sizeDp * 0.4f),
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.FloatingLetter(letter: String, color: Color, alignment: Alignment, sizeDp: androidx.compose.ui.unit.Dp, offsetDp: Pair<Int, Int>, periodMs: Int) {
    val transition = rememberInfiniteTransition(label = "letter$letter")
    val float by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "letterFloat$letter",
    )
    val tilt by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(periodMs + 400, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "letterTilt$letter",
    )
    Box(
        modifier = Modifier
            .align(alignment)
            .offset(x = offsetDp.first.dp, y = (offsetDp.second + float).dp)
            .rotate(tilt)
            .size(sizeDp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun BouncingDots(modifier: Modifier = Modifier, statusDescription: String) {
    val dotColors = listOf(SecondaryBlue, InfoTeal, ChildYellow)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = statusDescription
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        dotColors.forEachIndexed { index, color ->
            val transition = rememberInfiniteTransition(label = "dot$index")
            val bounce by transition.animateFloat(
                initialValue = 0f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 900
                        0f at 0
                        -10f at 150
                        0f at 300
                        0f at 900
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(index * 150),
                ),
                label = "dotBounce$index",
            )
            Box(
                modifier = Modifier
                    .offset(y = bounce.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun StudentClassRoom(uiState: LiveStudentRoomUiState, onToggleLocalPause: () -> Unit, onBackClick: () -> Unit) {
    val session = requireNotNull(uiState.session)
    val showingCard = session.status == "running" && !session.displayText.isNullOrBlank() && !uiState.isLocallyPaused
    val context = LocalContext.current
    val hasCameraHardware = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    var arUnavailable by remember { mutableStateOf(false) }
    val showArCamera = shouldMountArWebView(
        showingCard = showingCard,
        hasCameraHardware = hasCameraHardware,
        arUnavailable = arUnavailable
    )
    val instruction = when {
        uiState.isLocallyPaused -> "La lectura está pausada en tu pantalla."
        session.status == "paused" -> "Tu profe pausó la clase."
        showingCard -> "Mira la palabra"
        else -> "Espera a tu profe."
    }
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("En clase", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(if (uiState.isRealtimeConnected) "Clase en vivo" else "Sincronización automática", modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelLarge, color = if (uiState.isRealtimeConnected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(instruction, modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (showArCamera) {
                WebArContainer(
                    modifier = Modifier.clip(MaterialTheme.shapes.extraLarge),
                    displayText = session.displayText,
                    onCameraPermissionDenied = { arUnavailable = true },
                    onRuntimeError = { arUnavailable = true }
                )
                Text(
                    session.displayText.orEmpty(),
                    modifier = Modifier
                        .clearAndSetSemantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = session.displayText.orEmpty()
                        }
                        .size(1.dp),
                    color = Color.Transparent
                )
            } else if (showingCard) {
                Surface(modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 3.dp) {
                    Text(session.displayText.orEmpty(), modifier = Modifier.padding(horizontal = 24.dp, vertical = 56.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 44.sp, lineHeight = 52.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                }
            }
        }
        if (session.status == "running") {
            OutlinedButton(onClick = onToggleLocalPause, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text(if (uiState.isLocallyPaused) "Reanudar lectura" else "Pausar lectura") }
        }
        OutlinedButton(onClick = onBackClick, modifier = Modifier.padding(top = 8.dp).fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text("Volver al inicio") }
    }
}
