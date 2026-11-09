package co.edu.uniautonoma.inclusivereadingar.presentation.webar

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

/** Native stereo camera preview. CameraX avoids black WebRTC frames on Xiaomi WebView. */
@Composable
fun VrStereoContainer(
    modifier: Modifier = Modifier,
    displayText: String? = null,
    onCameraPermissionDenied: () -> Unit = {},
    onRuntimeError: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val leftPreviewView = remember { createPreviewView(context) }
    val rightPreviewView = remember { createPreviewView(context) }

    DisposableEffect(lifecycleOwner, cameraProviderFuture) {
        val listener = Runnable {
            runCatching {
                val provider = cameraProviderFuture.get()
                val leftPreview = Preview.Builder().build().also { it.setSurfaceProvider(leftPreviewView.surfaceProvider) }
                val rightPreview = Preview.Builder().build().also { it.setSurfaceProvider(rightPreviewView.surfaceProvider) }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, leftPreview, rightPreview)
            }.onFailure { error ->
                val message = error.message.orEmpty()
                if (message.contains("permission", ignoreCase = true)) onCameraPermissionDenied()
                else onRuntimeError(message.ifBlank { "No fue posible iniciar la cámara VR" })
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose { runCatching { cameraProviderFuture.get().unbindAll() } }
    }

    Row(modifier = modifier.background(Color.Black)) {
        StereoEye(leftPreviewView, displayText, -6f, Modifier.weight(1f).fillMaxSize())
        StereoEye(rightPreviewView, displayText, 6f, Modifier.weight(1f).fillMaxSize())
    }
}

private fun createPreviewView(context: android.content.Context) = PreviewView(context).apply {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    scaleType = PreviewView.ScaleType.FILL_CENTER
    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
}

@Composable
private fun StereoEye(previewView: PreviewView, displayText: String?, eyeOffset: Float, modifier: Modifier) {
    Box(modifier = modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        if (!displayText.isNullOrBlank()) VrStereoCard(displayText, eyeOffset, Modifier.align(Alignment.Center))
    }
}

@Composable
private fun VrStereoCard(text: String, eyeOffset: Float, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "vr-card-float")
    val floatY = transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "vr-card-y",
    ).value
    val rotation = transition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "vr-card-rotation",
    ).value
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .offset(x = eyeOffset.dp)
            .graphicsLayer { translationY = floatY; rotationY = rotation + eyeOffset * 0.25f }
            .size(width = 210.dp, height = 150.dp),
    ) {
        repeat(5) { depth ->
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = (10 - depth * 2).dp, y = (12 - depth * 2).dp)
                    .background(Color(0xFF173C2A).copy(alpha = 0.88f), shape),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(16.dp, shape)
                .background(Color(0xF2FFFFFF), shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${animalGlyph(text)}\n${text.uppercase()}",
                color = Color(0xFF18352A),
                fontSize = 28.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun animalGlyph(text: String): String = when (text.trim().lowercase()) {
    "gato", "gata" -> "🐱"
    "perro", "perra" -> "🐶"
    else -> "📖"
}
