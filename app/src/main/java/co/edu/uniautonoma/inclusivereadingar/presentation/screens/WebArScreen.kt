package co.edu.uniautonoma.inclusivereadingar.presentation.screens

import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.R
import co.edu.uniautonoma.inclusivereadingar.config.WebArConfig
import co.edu.uniautonoma.inclusivereadingar.data.repository.MockLearningRepository
import co.edu.uniautonoma.inclusivereadingar.domain.repository.LearningRepository
import co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel.WebArUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel.WebArViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel.WebArViewModelFactory

@Composable
fun WebArRoute(
    repository: LearningRepository = remember { MockLearningRepository() },
    viewModel: WebArViewModel = viewModel(factory = WebArViewModelFactory(repository))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WebArScreen(
        uiState = uiState,
        onRetry = {
            viewModel.clearWebError()
            viewModel.refreshContent()
        },
        onWebError = viewModel::onWebError,
        onDismissWebError = viewModel::clearWebError,
        onSimulateRead = viewModel::registerSimulatedRead,
        onNextWord = viewModel::nextWord,
        onPreviousWord = viewModel::previousWord
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebArScreen(
    uiState: WebArUiState,
    onRetry: () -> Unit,
    onWebError: (String) -> Unit,
    onDismissWebError: () -> Unit,
    onSimulateRead: () -> Unit,
    onNextWord: () -> Unit,
    onPreviousWord: () -> Unit
) {
    val context = LocalContext.current
    val currentUnit = uiState.units.getOrNull(uiState.currentWordIndex)
    val progress = if (uiState.units.isEmpty()) 0f else (uiState.currentWordIndex + 1f) / uiState.units.size

    var isPageLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.webar_title)) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF1E5AA8), Color(0xFF45A6D8))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.hero_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.hero_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 1.dp,
                color = Color(0xFFFFF8F2)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.current_word),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = currentUnit?.word ?: stringResource(R.string.empty_word),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD86B1B)
                    )
                    Text(
                        text = stringResource(
                            R.string.word_position,
                            if (uiState.units.isEmpty()) 0 else uiState.currentWordIndex + 1,
                            uiState.units.size
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (currentUnit != null) {
                        Text(
                            text = stringResource(R.string.word_category, currentUnit.category),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(100.dp))
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            stringResource(
                                R.string.practices_count,
                                uiState.completedPractices
                            )
                        )
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onPreviousWord,
                    enabled = uiState.currentWordIndex > 0
                ) {
                    Text(text = stringResource(R.string.previous_word))
                }
                Button(
                    modifier = Modifier.weight(1.4f),
                    onClick = onSimulateRead,
                    enabled = uiState.units.isNotEmpty()
                ) {
                    Text(text = stringResource(R.string.simulate_read))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onNextWord,
                    enabled = uiState.units.isNotEmpty() && uiState.currentWordIndex < uiState.units.lastIndex
                ) {
                    Text(text = stringResource(R.string.next_word))
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.activity_panel_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = {
                                WebView(context).apply {
                                    webViewRef = this

                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    if (WebArConfig.ALLOW_MIXED_CONTENT) {
                                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                    }

                                    webChromeClient = WebChromeClient()
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val targetUrl = request?.url?.toString()
                                            val isAllowed = isAllowedLearningUrl(targetUrl)
                                            if (!isAllowed) {
                                                onWebError(context.getString(R.string.friendly_navigation_blocked))
                                            }
                                            return !isAllowed
                                        }

                                        @Deprecated("Deprecated in Java")
                                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                            val isAllowed = isAllowedLearningUrl(url)
                                            if (!isAllowed) {
                                                onWebError(context.getString(R.string.friendly_navigation_blocked))
                                            }
                                            return !isAllowed
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            isPageLoading = false
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            error: android.webkit.WebResourceError?
                                        ) {
                                            if (request?.isForMainFrame == true) {
                                                isPageLoading = false
                                                onWebError(context.getString(R.string.friendly_load_error))
                                            }
                                        }

                                        override fun onReceivedHttpError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            errorResponse: WebResourceResponse?
                                        ) {
                                            if (request?.isForMainFrame == true) {
                                                isPageLoading = false
                                                onWebError(context.getString(R.string.friendly_load_error))
                                            }
                                        }
                                    }

                                    loadUrl(WebArConfig.WEB_AR_URL)
                                }
                            }
                        )

                        if (isPageLoading || uiState.isLoading) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.width(28.dp))
                                    Text(
                                        text = stringResource(R.string.loading),
                                        modifier = Modifier.padding(top = 12.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            isPageLoading = true
                            webViewRef?.reload()
                            onRetry()
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = stringResource(R.string.retry))
                    }
                }
            }

            uiState.lastRegisteredWord?.let { word ->
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.practice_feedback, word)) }
                )
            }

            uiState.generalError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            uiState.webError?.let { webError ->
                Surface(
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.friendly_error_title),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = webError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = {
                                onDismissWebError()
                                isPageLoading = true
                                webViewRef?.reload()
                            }
                        ) {
                            Text(text = stringResource(id = R.string.dismiss))
                        }
                    }
                }
            }
        }
    }
}

private fun isAllowedLearningUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false

    if (url.startsWith("file:///android_asset/")) return true
    if (url == "about:blank") return true

    val configuredUrl = WebArConfig.WEB_AR_URL
    if (configuredUrl.startsWith("http://") || configuredUrl.startsWith("https://")) {
        val configuredUri = Uri.parse(configuredUrl)
        val requestedUri = Uri.parse(url)
        return configuredUri.scheme == requestedUri.scheme && configuredUri.host == requestedUri.host
    }

    return false
}


