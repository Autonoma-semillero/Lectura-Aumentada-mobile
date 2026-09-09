package co.edu.uniautonoma.inclusivereadingar.presentation.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
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
import co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel.WebArCommand
import co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel.WebArUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel.WebArViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel.WebArViewModelFactory
import org.json.JSONObject

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
        onWebError = viewModel::onWebError,
        onCameraPermissionDenied = viewModel::onCameraPermissionDenied,
        onRetry = viewModel::retryActiveMarker,
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
    onWebError: (String) -> Unit,
    onCameraPermissionDenied: () -> Unit,
    onRetry: () -> Unit,
    onCommandDelivered: (Long) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val request = pendingPermissionRequest
        pendingPermissionRequest = null
        if (granted) {
            request?.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
        } else {
            request?.deny()
            onCameraPermissionDenied()
        }
    }

    val stopExperience = remember {
        {
            webViewRef?.evaluateJavascript(
                "window.WebAR && window.WebAR.stop({showActivation:true});",
                null
            )
        }
    }

    BackHandler {
        stopExperience()
        onBackClick()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> webViewRef?.onPause()
                Lifecycle.Event.ON_STOP -> stopExperience()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            pendingPermissionRequest?.deny()
            pendingPermissionRequest = null
            webViewRef?.evaluateJavascript("window.WebAR && window.WebAR.stop();", null)
            webViewRef?.removeJavascriptInterface(WebArConfig.BRIDGE_NAME)
            webViewRef?.stopLoading()
            webViewRef?.loadUrl("about:blank")
            webViewRef?.destroy()
            webViewRef = null
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
                            if (request?.isForMainFrame == true) {
                                onWebError("No fue posible iniciar la experiencia AR")
                            }
                        }
                    }

                    val handleBridgeMessage: (String) -> Unit = { rawMessage ->
                        runCatching { JSONObject(rawMessage) }
                            .onSuccess { message ->
                                when (message.optString("type")) {
                                    "marker-found" -> onMarkerFound(message.optString("markerId"))
                                    "marker-lost" -> onMarkerLost(message.optString("markerId"))
                                    "runtime-error" -> onWebError(
                                        message.optString("message", "Error en la experiencia AR")
                                    )
                                }
                            }
                            .onFailure { onWebError("La experiencia AR envió un evento inválido") }
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

                    loadUrl(WebArConfig.WEB_AR_URL)
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
                if (uiState.errorMessage != null && uiState.activeMarkerId != null) {
                    Button(onClick = onRetry) { Text("Reintentar") }
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

    is WebArCommand.MarkerNotFound -> JSONObject()
        .put("type", "marker-not-found")
        .put("markerId", markerId)

    is WebArCommand.AssetError -> JSONObject()
        .put("type", "asset-error")
        .put("markerId", markerId)
        .put("message", message)

    is WebArCommand.ClearMarker -> JSONObject()
        .put("type", "clear-marker")
        .put("markerId", markerId)

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
