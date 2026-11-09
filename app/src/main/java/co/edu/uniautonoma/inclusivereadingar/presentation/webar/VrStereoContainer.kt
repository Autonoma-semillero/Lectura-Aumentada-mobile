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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
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
        StereoEye(leftPreviewView, displayText, Modifier.weight(1f).fillMaxSize())
        StereoEye(rightPreviewView, displayText, Modifier.weight(1f).fillMaxSize())
    }
}

private fun createPreviewView(context: android.content.Context) = PreviewView(context).apply {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    scaleType = PreviewView.ScaleType.FILL_CENTER
    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
}

@Composable
private fun StereoEye(previewView: PreviewView, displayText: String?, modifier: Modifier) {
    Box(modifier = modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        if (!displayText.isNullOrBlank()) {
            Text(
                text = displayText,
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
