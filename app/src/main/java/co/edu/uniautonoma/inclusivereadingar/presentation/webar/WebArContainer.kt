package co.edu.uniautonoma.inclusivereadingar.presentation.webar

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import co.edu.uniautonoma.inclusivereadingar.BuildConfig
import co.edu.uniautonoma.inclusivereadingar.config.WebArConfig
import co.edu.uniautonoma.inclusivereadingar.presentation.screens.LegacyWebArBridge
import co.edu.uniautonoma.inclusivereadingar.presentation.screens.isAllowedWebArNavigation
import co.edu.uniautonoma.inclusivereadingar.presentation.screens.isTrustedWebArOrigin
import co.edu.uniautonoma.inclusivereadingar.presentation.screens.webArDocumentUrl
import java.util.UUID

/** Pushes at most this often; faster than this just floods the bridge without a visible benefit. */
private const val ORIENTATION_PUSH_INTERVAL_MS = 50L

/**
 * Mount for the floating-card WebAR engine (`webar/index-float.html`): camera permission,
 * [WebViewAssetLoader] wiring and lifecycle teardown shared with [co.edu.uniautonoma.inclusivereadingar.presentation.screens.WebArScreen].
 *
 * Loads a document separate from the marker-based `webar/index.html` used by `WebArScreen`
 * (see `WebArConfig.WEB_AR_FLOAT_URL`), so both WebViews keep loading independent documents
 * without interfering with each other. [displayText] is pushed into the live document via
 * `window.WebAR.updateCardWord(...)` whenever it changes.
 *
 * `android.webkit.WebView` never dispatches the web `deviceorientation` event (confirmed live
 * via Chrome DevTools Protocol: zero events over several seconds despite a granted permission
 * and an allowing Permissions Policy), so the phone's physical rotation is read natively via
 * [DeviceOrientationTracker] and pushed in through `window.WebAR.updateCameraOrientation(...)`
 * instead of relying on a browser event the WebView doesn't deliver.
 */
@SuppressLint("SetJavaScriptEnabled") // Required by the bundled A-Frame/AR.js runtime on a locked origin.
@Composable
fun WebArContainer(
    modifier: Modifier = Modifier,
    displayText: String? = null,
    onCameraReady: () -> Unit = {},
    onCameraPermissionDenied: () -> Unit = {},
    onRuntimeError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    val sessionId = remember { UUID.randomUUID().toString() }
    val latestDisplayText by rememberUpdatedState(displayText)
    val orientationTracker = remember {
        DeviceOrientationTracker(context.getSystemService(Context.SENSOR_SERVICE) as SensorManager)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val request = pendingPermissionRequest
        pendingPermissionRequest = null
        if (request != null) {
            if (granted) {
                request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
            } else {
                request.deny()
                onCameraPermissionDenied()
            }
        }
    }

    LaunchedEffect(webViewRef, displayText) {
        val webView = webViewRef ?: return@LaunchedEffect
        val word = displayText?.takeIf(String::isNotBlank) ?: return@LaunchedEffect
        runCatching {
            webView.evaluateJavascript(
                "window.WebAR && window.WebAR.updateCardWord(${org.json.JSONObject.quote(word)});",
                null
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> webViewRef?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose {
            orientationTracker.stop()
            pendingPermissionRequest?.deny()
            pendingPermissionRequest = null
            try {
                webViewRef?.evaluateJavascript("window.WebAR && window.WebAR.stop();", null)
            } catch (_: RuntimeException) {
                // The WebView may already be detaching when the AR card leaves composition.
            }
            webViewRef?.removeJavascriptInterface(WebArConfig.BRIDGE_NAME)
            webViewRef?.stopLoading()
            webViewRef?.loadUrl("about:blank")
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                .build()

            WebView(context).apply {
                webViewRef = this
                WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.safeBrowsingEnabled = true

                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest) {
                        if (!request.origin.isTrustedWebArOrigin() ||
                            !request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) ||
                            request.resources.any { it != PermissionRequest.RESOURCE_VIDEO_CAPTURE }
                        ) {
                            request.deny()
                            return
                        }
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                        } else {
                            pendingPermissionRequest?.deny()
                            pendingPermissionRequest = request
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }

                    override fun onPermissionRequestCanceled(request: PermissionRequest) {
                        if (pendingPermissionRequest == request) pendingPermissionRequest = null
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        return request?.url?.let(assetLoader::shouldInterceptRequest)
                            ?: super.shouldInterceptRequest(view, request)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean = !request?.url.isAllowedWebArNavigation()

                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return !url?.let(Uri::parse).isAllowedWebArNavigation()
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (view !== webViewRef) return
                        view?.evaluateJavascript(
                            """
                                (function () {
                                  var button = document.getElementById('activate-camera');
                                  if (button) button.click();
                                })();
                            """.trimIndent(),
                            null
                        )
                    }
                }

                var lastOrientationPushAtMs = 0L
                val handleBridgeMessage: (String) -> Unit = { rawMessage ->
                    runCatching { org.json.JSONObject(rawMessage) }
                        .onSuccess { message ->
                            post {
                                if (this !== webViewRef) return@post
                                when (message.optString("type")) {
                                    "camera-ready" -> {
                                        onCameraReady()
                                        // The native push in the LaunchedEffect below can land
                                        // before `window.WebAR` exists (page still loading), so
                                        // re-send the latest word once the scene confirms it's
                                        // ready to receive it.
                                        latestDisplayText?.takeIf(String::isNotBlank)?.let { word ->
                                            evaluateJavascript(
                                                "window.WebAR && window.WebAR.updateCardWord(${org.json.JSONObject.quote(word)});",
                                                null
                                            )
                                        }
                                        orientationTracker.start { quaternion ->
                                            val now = System.currentTimeMillis()
                                            if (now - lastOrientationPushAtMs < ORIENTATION_PUSH_INTERVAL_MS) return@start
                                            lastOrientationPushAtMs = now
                                            post {
                                                if (this !== webViewRef) return@post
                                                evaluateJavascript(
                                                    "window.WebAR && window.WebAR.updateCameraOrientation(" +
                                                        "${quaternion.x}, ${quaternion.y}, ${quaternion.z}, ${quaternion.w});",
                                                    null
                                                )
                                            }
                                        }
                                    }
                                    "runtime-error" -> onRuntimeError(
                                        message.optString(
                                            "message",
                                            "Error en la experiencia AR"
                                        )
                                    )
                                }
                            }
                        }
                }

                if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                    WebViewCompat.addWebMessageListener(
                        this,
                        WebArConfig.BRIDGE_NAME,
                        setOf(WebArConfig.TRUSTED_ORIGIN)
                    ) { _, message, sourceOrigin, isMainFrame, _ ->
                        if (isMainFrame && sourceOrigin.isTrustedWebArOrigin()) {
                            message.data?.let(handleBridgeMessage)
                        }
                    }
                } else {
                    addJavascriptInterface(
                        LegacyWebArBridge(handleBridgeMessage),
                        WebArConfig.BRIDGE_NAME
                    )
                }

                loadUrl(webArDocumentUrl(sessionId, WebArConfig.WEB_AR_FLOAT_URL))
            }
        }
    )
}

/**
 * Pure Fase-1 gate: the AR WebView only mounts while the card would otherwise be shown,
 * the device reports usable camera hardware, and nothing already forced a fallback to the
 * plain-text card for this room visit.
 */
internal fun shouldMountArWebView(
    showingCard: Boolean,
    hasCameraHardware: Boolean,
    arUnavailable: Boolean
): Boolean = showingCard && hasCameraHardware && !arUnavailable
