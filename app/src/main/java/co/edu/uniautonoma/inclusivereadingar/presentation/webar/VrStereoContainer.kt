package co.edu.uniautonoma.inclusivereadingar.presentation.webar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import co.edu.uniautonoma.inclusivereadingar.config.WebArConfig

/**
 * Hosts the Cardboard-style stereo document. The web layer owns a single camera stream and
 * renders two synchronized eye views, so Android never attempts to open the camera twice.
 */
@Composable
fun VrStereoContainer(
    modifier: Modifier = Modifier,
    displayText: String? = null,
    onCameraPermissionDenied: () -> Unit = {},
    onRuntimeError: (String) -> Unit = {},
) {
    WebArContainer(
        modifier = modifier,
        displayText = displayText,
        documentUrl = WebArConfig.WEB_AR_VR_URL,
        onCameraPermissionDenied = onCameraPermissionDenied,
        onRuntimeError = onRuntimeError,
    )
}
