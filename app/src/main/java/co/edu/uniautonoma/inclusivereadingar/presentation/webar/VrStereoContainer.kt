package co.edu.uniautonoma.inclusivereadingar.presentation.webar

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import co.edu.uniautonoma.inclusivereadingar.config.WebArConfig

/** One camera stream and one A-Frame renderer feed the two synchronized Cardboard eye views. */
@Composable
fun VrStereoContainer(
    modifier: Modifier = Modifier,
    displayText: String? = null,
    onCameraPermissionDenied: () -> Unit = {},
    onRuntimeError: (String) -> Unit = {},
) {
    val context = LocalContext.current
    DisposableEffect(context) {
        val activity = context.findActivity()
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { if (previous != null) activity.requestedOrientation = previous }
    }
    WebArContainer(
        modifier = modifier,
        displayText = displayText,
        documentUrl = WebArConfig.WEB_AR_VR_URL,
        onCameraPermissionDenied = onCameraPermissionDenied,
        onRuntimeError = onRuntimeError,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
