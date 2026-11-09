package co.edu.uniautonoma.inclusivereadingar.presentation.webar

import android.view.ViewGroup
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
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

    DisposableEffect(context) {
        val activity = context.findActivity()
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            if (previousOrientation != null) activity.requestedOrientation = previousOrientation
        }
    }
    val headPose = rememberVrHeadPose()

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
        StereoEye(leftPreviewView, displayText, headPose, -6f, Modifier.weight(1f).fillMaxSize())
        StereoEye(rightPreviewView, displayText, headPose, 6f, Modifier.weight(1f).fillMaxSize())
    }
}

private fun createPreviewView(context: android.content.Context) = PreviewView(context).apply {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    scaleType = PreviewView.ScaleType.FILL_CENTER
    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
}

@Composable
private fun StereoEye(previewView: PreviewView, displayText: String?, headPose: Offset, eyeOffset: Float, modifier: Modifier) {
    Box(modifier = modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        if (!displayText.isNullOrBlank()) VrStereoCard(displayText, headPose, eyeOffset, Modifier.align(Alignment.Center))
    }
}

@Composable
private fun VrStereoCard(text: String, headPose: Offset, eyeOffset: Float, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "vr-card-float")
    val floatY = transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "vr-card-y",
    ).value
    val rotation = transition.animateFloat(
        initialValue = -16f,
        targetValue = -9f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "vr-card-rotation",
    ).value
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .offset(x = (headPose.x + eyeOffset).dp, y = headPose.y.dp)
            .graphicsLayer {
                translationY = floatY
                rotationX = 12f
                rotationY = rotation + eyeOffset * 0.18f
                rotationZ = -1.5f
                cameraDistance = 14f * density
            }
            .size(width = 160.dp, height = 107.dp),
    ) {
        val depthColors = listOf(0xFFD9D2C0, 0xFFDDD6C5, 0xFFE3DDCD, 0xFFE9E4D6, 0xFFF0ECE0)
        depthColors.forEachIndexed { depth, color ->
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = (9 - depth * 1.6f).dp, y = (10 - depth * 1.6f).dp)
                    .background(Color(color), shape),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(16.dp, shape)
                .background(Color.White, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text.uppercase(),
                color = Color(0xFFC62828),
                fontSize = 27.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun rememberVrHeadPose(): Offset {
    val context = LocalContext.current
    var pose by remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val rotation = FloatArray(9)
        val orientation = FloatArray(3)
        var originYaw: Float? = null
        var originPitch: Float? = null
        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                if (originYaw == null) {
                    originYaw = orientation[0]
                    originPitch = orientation[1]
                }
                val yaw = angleDelta(orientation[0], originYaw ?: 0f)
                val pitch = angleDelta(orientation[1], originPitch ?: 0f)
                pose = Offset(
                    x = (-yaw * 150f).coerceIn(-145f, 145f),
                    y = (pitch * 115f).coerceIn(-85f, 85f),
                )
            }
        }
        if (sensor != null) manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { manager.unregisterListener(listener) }
    }
    return pose
}

private fun angleDelta(value: Float, origin: Float): Float {
    var delta = value - origin
    while (delta > Math.PI) delta -= (Math.PI * 2).toFloat()
    while (delta < -Math.PI) delta += (Math.PI * 2).toFloat()
    return delta
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
