package co.edu.uniautonoma.inclusivereadingar.presentation.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import co.edu.uniautonoma.inclusivereadingar.BuildConfig
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.config.WebArConfig
import co.edu.uniautonoma.inclusivereadingar.domain.model.ArAsset
import co.edu.uniautonoma.inclusivereadingar.domain.ocr.OcrWordEvent
import co.edu.uniautonoma.inclusivereadingar.domain.ocr.OcrWordStabilizer
import co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel.WebArCommand
import co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel.WebArUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel.WebArViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel.WebArViewModelFactory
import co.edu.uniautonoma.inclusivereadingar.presentation.webar.WebArOcrPipeline
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

@Composable
fun WebArRoute(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: WebArViewModel = viewModel(
        factory = WebArViewModelFactory(container.arAssetRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WebArScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onMarkerFound = viewModel::onMarkerFound,
        onMarkerLost = viewModel::onMarkerLost,
        onOcrWordDetected = viewModel::onOcrWordDetected,
        onOcrWordPositionUpdated = viewModel::onOcrWordPositionUpdated,
        onOcrWordLost = viewModel::onOcrWordLost,
        onCameraReady = viewModel::onCameraReady,
        onExperienceStopped = viewModel::onExperienceStopped,
        onModelReady = viewModel::onModelReady,
        onModelError = viewModel::onModelError,
        onWebError = viewModel::onWebError,
        onCameraPermissionDenied = viewModel::onCameraPermissionDenied,
        onRetry = viewModel::retryActiveTarget,
        onCommandDelivered = viewModel::onCommandDelivered
    )
}

@SuppressLint("SetJavaScriptEnabled") // Required by the bundled A-Frame/AR.js runtime on a locked origin.
@Composable
fun WebArScreen(
    uiState: WebArUiState,
    onBackClick: () -> Unit,
    onMarkerFound: (String) -> Unit,
    onMarkerLost: (String) -> Unit,
    onOcrWordDetected: (String, Float, Float, Float) -> Unit,
    onOcrWordPositionUpdated: (String, Float, Float, Float) -> Unit,
    onOcrWordLost: (String) -> Unit,
    onCameraReady: () -> Unit,
    onExperienceStopped: () -> Unit,
    onModelReady: (String) -> Unit,
    onModelError: (String, String) -> Unit,
    onWebError: (String) -> Unit,
    onCameraPermissionDenied: () -> Unit,
    onRetry: () -> Unit,
    onCommandDelivered: (Long) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.findActivity() }
    val ocrPipeline = remember(activity) { activity?.window?.let(::WebArOcrPipeline) }
    val ocrStabilizer = remember { OcrWordStabilizer() }
    val reloadGeneration = remember { AtomicLong(0L) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var isCameraReady by remember { mutableStateOf(false) }
    val resumeCoordinator = remember { WebArResumeCoordinator() }
    var autoActivateAfterReload by remember { mutableStateOf(false) }
    var acceptsCameraReady by remember { mutableStateOf(false) }
    var isDocumentReloading by remember { mutableStateOf(false) }
    var expectedDocumentSessionId by remember { mutableStateOf<String?>(null) }
    var activeDocumentSessionId by remember { mutableStateOf<String?>(null) }

    val resetOcr = remember(ocrStabilizer, onOcrWordLost) {
        {
            ocrStabilizer.reset().forEach { event ->
                if (event is OcrWordEvent.Lost) onOcrWordLost(event.word)
            }
        }
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

    val stopExperience = remember(resetOcr, onExperienceStopped) {
        {
            isCameraReady = false
            resumeCoordinator.cancelRestart()
            acceptsCameraReady = false
            isDocumentReloading = false
            expectedDocumentSessionId = null
            activeDocumentSessionId = null
            autoActivateAfterReload = false
            reloadGeneration.incrementAndGet()
            pendingPermissionRequest?.deny()
            pendingPermissionRequest = null
            onExperienceStopped()
            resetOcr()
            try {
                webViewRef?.evaluateJavascript(
                    "window.WebAR && window.WebAR.stop({showActivation:true});",
                    null
                )
            } catch (_: RuntimeException) {
                // The WebView may already be detaching after a main-frame failure.
            }
        }
    }

    val reloadExperienceDocument = remember(resetOcr, onExperienceStopped) {
        reload@{ autoActivate: Boolean ->
            isCameraReady = false
            resumeCoordinator.cancelRestart()
            acceptsCameraReady = false
            isDocumentReloading = true
            val documentSessionId = UUID.randomUUID().toString()
            expectedDocumentSessionId = documentSessionId
            activeDocumentSessionId = null
            pendingPermissionRequest?.deny()
            pendingPermissionRequest = null
            onExperienceStopped()
            resetOcr()
            autoActivateAfterReload = autoActivate
            val webView = webViewRef ?: return@reload
            val generation = reloadGeneration.incrementAndGet()
            val reloadGate = WebArReloadGate(generation)
            lateinit var fallback: Runnable
            val reloadOnce = {
                if (reloadGate.tryAcquire(
                        currentGeneration = reloadGeneration.get(),
                        isCurrentWebView = webViewRef === webView
                    )
                ) {
                    webView.removeCallbacks(fallback)
                    webView.loadUrl(webArDocumentUrl(documentSessionId))
                }
            }
            fallback = Runnable { reloadOnce() }
            webView.postDelayed(fallback, WEB_AR_RELOAD_FALLBACK_MS)
            try {
                webView.evaluateJavascript("window.WebAR && window.WebAR.stop();") {
                    webView.post { reloadOnce() }
                }
            } catch (_: RuntimeException) {
                webView.post { reloadOnce() }
            }
        }
    }

    BackHandler {
        stopExperience()
        onBackClick()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    webViewRef?.onResume()
                    if (resumeCoordinator.consumeRestartOnResume()) {
                        // JS stops AR on visibility loss; rebuild instead of trusting stale readiness.
                        reloadExperienceDocument(true)
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    resumeCoordinator.onPause(isCameraReady)
                    isCameraReady = false
                    resetOcr()
                    webViewRef?.onPause()
                }
                // A document reload releases every AR.js listener/timer before reactivation.
                Lifecycle.Event.ON_STOP -> reloadExperienceDocument(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(ocrPipeline) {
        onDispose {
            reloadGeneration.incrementAndGet()
            pendingPermissionRequest?.deny()
            pendingPermissionRequest = null
            isCameraReady = false
            resumeCoordinator.cancelRestart()
            acceptsCameraReady = false
            isDocumentReloading = false
            expectedDocumentSessionId = null
            activeDocumentSessionId = null
            onExperienceStopped()
            resetOcr()
            webViewRef?.evaluateJavascript("window.WebAR && window.WebAR.stop();", null)
            webViewRef?.removeJavascriptInterface(WebArConfig.BRIDGE_NAME)
            webViewRef?.stopLoading()
            webViewRef?.loadUrl("about:blank")
            webViewRef?.destroy()
            webViewRef = null
            ocrPipeline?.close()
        }
    }

    LaunchedEffect(isCameraReady, webViewRef, uiState.activeMarkerId, ocrPipeline) {
        val webView = webViewRef
        if (!isCameraReady || webView == null || ocrPipeline == null ||
            uiState.activeMarkerId != null
        ) {
            resetOcr()
            return@LaunchedEffect
        }

        delay(WebArOcrPipeline.CAMERA_WARM_UP_MS)
        while (currentCoroutineContext().isActive) {
            try {
                val candidates = ocrPipeline.scan(webView)
                ocrStabilizer.update(candidates).forEach { event ->
                    when (event) {
                        is OcrWordEvent.Detected -> with(event.candidate) {
                            onOcrWordDetected(word, confidence, centerX, centerY)
                        }
                        is OcrWordEvent.Updated -> with(event.candidate) {
                            onOcrWordPositionUpdated(word, confidence, centerX, centerY)
                        }
                        is OcrWordEvent.Lost -> onOcrWordLost(event.word)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // OCR is an enhancement; marker tracking remains usable if one frame fails.
                Log.w(WEB_AR_LOG_TAG, "On-device OCR frame failed", error)
            }
            delay(WebArOcrPipeline.SCAN_INTERVAL_MS)
        }
    }

    LaunchedEffect(uiState.outboundCommand?.id, webViewRef) {
        val envelope = uiState.outboundCommand ?: return@LaunchedEffect
        val webView = webViewRef ?: return@LaunchedEffect
        val payload = envelope.command.toJson().toString()
        webView.evaluateJavascript(
            "window.WebAR && window.WebAR.receiveNativeMessage(${JSONObject.quote(payload)});",
            null
        )
        onCommandDelivered(envelope.id)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val assetLoader = WebViewAssetLoader.Builder()
                    .addPathHandler(
                        "/assets/",
                        WebViewAssetLoader.AssetsPathHandler(context)
                    )
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
                            if (!acceptsCameraReady || isDocumentReloading) {
                                request.deny()
                                return
                            }
                            val isTrusted = request.origin.isTrustedWebArOrigin()
                            val asksForCamera = request.resources.contains(
                                PermissionRequest.RESOURCE_VIDEO_CAPTURE
                            )
                            val asksForUnsupportedResource = request.resources.any {
                                it != PermissionRequest.RESOURCE_VIDEO_CAPTURE
                            }
                            if (!isTrusted || !asksForCamera || asksForUnsupportedResource) {
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

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            val failedSessionId = request?.url?.toString()
                                .webArDocumentSessionId()
                            if (request?.isForMainFrame == true &&
                                failedSessionId != null &&
                                view === webViewRef &&
                                (failedSessionId == activeDocumentSessionId ||
                                    failedSessionId == expectedDocumentSessionId)
                            ) {
                                stopExperience()
                                onWebError("No fue posible iniciar la experiencia AR")
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val finishedSessionId = url.webArDocumentSessionId()
                            if (view === webViewRef &&
                                finishedSessionId != null &&
                                finishedSessionId == expectedDocumentSessionId
                            ) {
                                isDocumentReloading = false
                                expectedDocumentSessionId = null
                                activeDocumentSessionId = finishedSessionId
                                acceptsCameraReady = true
                            }
                            if (!autoActivateAfterReload ||
                                finishedSessionId != activeDocumentSessionId
                            ) {
                                return
                            }
                            autoActivateAfterReload = false
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

                    val handleBridgeMessage: (String) -> Unit = { rawMessage ->
                        runCatching { JSONObject(rawMessage) }
                            .onSuccess { message ->
                                post {
                                    val type = message.optString("type")
                                    val messageSessionId = message.optString("sessionId")
                                        .takeIf(String::isNotBlank)
                                    val isResumed = lifecycleOwner.lifecycle.currentState
                                        .isAtLeast(Lifecycle.State.RESUMED)
                                    if (!shouldProcessWebArBridgeEvent(
                                            type = type,
                                            messageSessionId = messageSessionId,
                                            activeSessionId = activeDocumentSessionId,
                                            acceptsSessionEvents = acceptsCameraReady,
                                            isDocumentReloading = isDocumentReloading,
                                            isLifecycleResumed = isResumed
                                        )
                                    ) {
                                        return@post
                                    }
                                    when (type) {
                                        "camera-ready" -> {
                                            if (isResumed) {
                                                resumeCoordinator.cancelRestart()
                                                isCameraReady = true
                                                onCameraReady()
                                            } else {
                                                resumeCoordinator.requestRestart()
                                            }
                                        }
                                        "model-ready" -> message.optString("assetId")
                                            .takeIf(String::isNotBlank)
                                            ?.let(onModelReady)
                                        "model-error" -> message.optString("assetId")
                                            .takeIf(String::isNotBlank)
                                            ?.let { assetId ->
                                                onModelError(
                                                    assetId,
                                                    message.optString(
                                                        "message",
                                                        "No fue posible renderizar el modelo 3D"
                                                    )
                                                )
                                            }
                                        "camera-retry-requested" -> {
                                            reloadExperienceDocument(true)
                                        }
                                        "marker-found" -> onMarkerFound(message.optString("markerId"))
                                        "marker-lost" -> onMarkerLost(message.optString("markerId"))
                                        "runtime-error" -> {
                                            stopExperience()
                                            onWebError(
                                                message.optString(
                                                    "message",
                                                    "Error en la experiencia AR"
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            .onFailure {
                                post { onWebError("La experiencia AR envió un evento inválido") }
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

                    val initialSessionId = UUID.randomUUID().toString()
                    expectedDocumentSessionId = initialSessionId
                    activeDocumentSessionId = null
                    loadUrl(webArDocumentUrl(initialSessionId))
                }
            }
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp),
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.58f)
        ) {
            IconButton(onClick = {
                stopExperience()
                onBackClick()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Cerrar realidad aumentada",
                    tint = Color.White
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.Black.copy(alpha = 0.68f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.isAssetLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(4.dp),
                        color = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.statusMessage,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    uiState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = Color(0xFFFFB4AB),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (uiState.errorMessage != null &&
                    (uiState.requiresCameraRestart ||
                        uiState.activeMarkerId != null ||
                        uiState.activeWordTarget != null)
                ) {
                    Button(
                        onClick = {
                            if (uiState.requiresCameraRestart) {
                                reloadExperienceDocument(true)
                            } else {
                                onRetry()
                            }
                        }
                    ) {
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}

private class LegacyWebArBridge(
    private val onMessage: (String) -> Unit
) {
    @JavascriptInterface
    fun postMessage(message: String) = onMessage(message)
}

private fun Uri?.isTrustedWebArOrigin(): Boolean {
    if (this == null) return false
    val trusted = Uri.parse(WebArConfig.TRUSTED_ORIGIN)
    return scheme == trusted.scheme && host == trusted.host && effectivePort() == trusted.effectivePort()
}

private fun Uri?.isAllowedWebArNavigation(): Boolean {
    if (this == null) return false
    if (toString() == "about:blank") return true
    return isTrustedWebArOrigin()
}

private fun Uri.effectivePort(): Int = when {
    port != -1 -> port
    scheme == "https" -> 443
    else -> 80
}

private fun WebArCommand.toJson(): JSONObject = when (this) {
    is WebArCommand.AssetReady -> JSONObject()
        .put("type", "asset-ready")
        .put("asset", asset.toJson())
        .apply {
            wordTarget?.let { target ->
                put(
                    "target",
                    JSONObject()
                        .put("type", "word")
                        .put("centerX", target.centerX)
                        .put("centerY", target.centerY)
                )
            }
        }

    is WebArCommand.MarkerNotFound -> JSONObject()
        .put("type", "marker-not-found")
        .put("markerId", markerId)

    is WebArCommand.WordNotFound -> JSONObject()
        .put("type", "word-not-found")
        .put("word", target.word)
        .put(
            "target",
            JSONObject()
                .put("type", "word")
                .put("centerX", target.centerX)
                .put("centerY", target.centerY)
        )

    is WebArCommand.AssetError -> JSONObject()
        .put("type", "asset-error")
        .put("markerId", markerId)
        .put("message", message)

    is WebArCommand.ClearMarker -> JSONObject()
        .put("type", "clear-marker")
        .put("markerId", markerId)

    is WebArCommand.ActivateWordTarget -> JSONObject()
        .put("type", "activate-word-target")
        .put("word", target.word)
        .put("centerX", target.centerX)
        .put("centerY", target.centerY)

    is WebArCommand.ClearWordTarget -> JSONObject()
        .put("type", "clear-word-target")
        .apply { word?.let { put("word", it) } }

    WebArCommand.CameraPermissionDenied -> JSONObject()
        .put("type", "camera-permission-denied")
}

private fun ArAsset.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("learningUnitId", learningUnitId)
    .put("markerId", markerId)
    .put("word", word)
    .apply {
        model3dUrl?.let { put("model3dUrl", it) }
        audioUrl?.let { put("audioUrl", it) }
        accessibilityLabel?.let { put("accessibilityLabel", it) }
    }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Keeps pause/resume intent separate from readiness reported by the current document. */
internal class WebArResumeCoordinator {
    private var restartOnResume = false

    fun onPause(wasCameraReady: Boolean) {
        restartOnResume = wasCameraReady
    }

    fun requestRestart() {
        restartOnResume = true
    }

    fun cancelRestart() {
        restartOnResume = false
    }

    fun consumeRestartOnResume(): Boolean = restartOnResume.also {
        restartOnResume = false
    }
}

/** Arbitrates the JS callback and timeout so a document generation loads at most once. */
internal class WebArReloadGate(private val generation: Long) {
    private var acquired = false

    fun tryAcquire(currentGeneration: Long, isCurrentWebView: Boolean): Boolean {
        if (acquired || generation != currentGeneration || !isCurrentWebView) return false
        acquired = true
        return true
    }
}

internal fun shouldProcessWebArBridgeEvent(
    type: String,
    messageSessionId: String?,
    activeSessionId: String?,
    acceptsSessionEvents: Boolean,
    isDocumentReloading: Boolean,
    isLifecycleResumed: Boolean
): Boolean {
    if (messageSessionId == null ||
        messageSessionId != activeSessionId ||
        isDocumentReloading
    ) {
        return false
    }
    return when (type) {
        // A current document may finish camera initialization while a transient overlay is open.
        "camera-ready" -> acceptsSessionEvents
        // Explicit recovery remains available after a runtime error invalidates normal events.
        "camera-retry-requested" -> isLifecycleResumed
        else -> acceptsSessionEvents && isLifecycleResumed
    }
}

private fun webArDocumentUrl(sessionId: String): String =
    Uri.parse(WebArConfig.WEB_AR_URL)
        .buildUpon()
        .appendQueryParameter(WEB_AR_SESSION_QUERY, sessionId)
        .build()
        .toString()

private fun String?.webArDocumentSessionId(): String? {
    val uri = this?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return null
    val configuredUri = Uri.parse(WebArConfig.WEB_AR_URL)
    if (!uri.isTrustedWebArOrigin() || uri.path != configuredUri.path) return null
    return uri.getQueryParameter(WEB_AR_SESSION_QUERY)?.takeIf(String::isNotBlank)
}

private const val WEB_AR_LOG_TAG = "WebArOcr"
private const val WEB_AR_RELOAD_FALLBACK_MS = 750L
private const val WEB_AR_SESSION_QUERY = "nativeSession"
