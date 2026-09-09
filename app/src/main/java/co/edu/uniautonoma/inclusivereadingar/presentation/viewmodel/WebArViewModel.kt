package co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.domain.model.ArAsset
import co.edu.uniautonoma.inclusivereadingar.domain.ocr.OcrWordNormalizer
import co.edu.uniautonoma.inclusivereadingar.domain.repository.ArAssetRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WebArViewModel(
    private val arAssetRepository: ArAssetRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WebArUiState())
    val uiState: StateFlow<WebArUiState> = _uiState.asStateFlow()

    private val markerCache = mutableMapOf<String, ArAsset?>()
    private val wordCache = mutableMapOf<String, ArAsset?>()
    private var lookupJob: Job? = null
    private var pendingWordPosition: WebArWordTarget? = null
    private var requestVersion = 0L
    private var commandVersion = 0L

    fun onMarkerFound(rawMarkerId: String) {
        val markerId = rawMarkerId.trim()
        if (!markerId.matches(MARKER_ID_PATTERN)) {
            publishError(markerId.ifBlank { "unknown" }, "El marcador detectado no es válido")
            return
        }

        val current = _uiState.value
        if (current.activeMarkerId == markerId &&
            (current.isAssetLoading || current.asset != null || current.markerWithoutAssociation)
        ) {
            return
        }

        lookupJob?.cancel()
        requestVersion += 1
        val version = requestVersion
        val previousWord = current.activeWordTarget?.word
        pendingWordPosition = null
        _uiState.update {
            it.copy(
                activeMarkerId = markerId,
                activeWordTarget = null,
                asset = null,
                isAssetLoading = true,
                markerWithoutAssociation = false,
                wordWithoutAssociation = false,
                statusMessage = "Consultando contenido…",
                errorMessage = null
            )
        }
        if (previousWord != null) publishCommand(WebArCommand.ClearWordTarget(previousWord))

        if (markerCache.containsKey(markerId)) {
            applyMarkerLookupResult(markerId, markerCache[markerId])
            return
        }

        lookupJob = viewModelScope.launch {
            runCatching { arAssetRepository.findByMarker(markerId) }
                .onSuccess { asset ->
                    if (version != requestVersion || _uiState.value.activeMarkerId != markerId) {
                        return@onSuccess
                    }
                    markerCache[markerId] = asset
                    applyMarkerLookupResult(markerId, asset)
                }
                .onFailure {
                    if (version != requestVersion || _uiState.value.activeMarkerId != markerId) {
                        return@onFailure
                    }
                    publishError(markerId, "No fue posible cargar el contenido del marcador")
                }
        }
    }

    fun onMarkerLost(rawMarkerId: String) {
        val markerId = rawMarkerId.trim()
        if (_uiState.value.activeMarkerId != markerId) return
        lookupJob?.cancel()
        pendingWordPosition = null
        requestVersion += 1
        _uiState.update {
            it.copy(
                activeMarkerId = null,
                asset = null,
                isAssetLoading = false,
                markerWithoutAssociation = false,
                statusMessage = "Marcador perdido. Enfoca una palabra o tarjeta.",
                errorMessage = null
            )
        }
        publishCommand(WebArCommand.ClearMarker(markerId))
    }

    fun onOcrWordDetected(
        rawWord: String,
        confidence: Float,
        centerX: Float,
        centerY: Float
    ) {
        val word = OcrWordNormalizer.normalize(rawWord) ?: return
        if (!confidence.isFinite() || confidence < MIN_OCR_CONFIDENCE) return
        if (!centerX.isFinite() || !centerY.isFinite()) return

        val current = _uiState.value
        // Marker tracking is more precise, so it wins while a physical marker is visible.
        if (current.activeMarkerId != null) return
        if (current.activeWordTarget?.word == word &&
            (current.isAssetLoading || current.asset != null || current.wordWithoutAssociation)
        ) {
            return
        }

        val target = WebArWordTarget(
            word = word,
            confidence = confidence.coerceIn(0f, 1f),
            centerX = centerX.coerceIn(0f, 1f),
            centerY = centerY.coerceIn(0f, 1f)
        )

        lookupJob?.cancel()
        requestVersion += 1
        val version = requestVersion
        val previousWord = current.activeWordTarget?.word
        pendingWordPosition = null
        _uiState.update {
            it.copy(
                activeMarkerId = null,
                activeWordTarget = target,
                asset = null,
                isAssetLoading = true,
                markerWithoutAssociation = false,
                wordWithoutAssociation = false,
                statusMessage = "Palabra «$word» detectada. Consultando contenido…",
                errorMessage = null
            )
        }
        if (previousWord != null && previousWord != word) {
            publishCommand(WebArCommand.ClearWordTarget(previousWord))
        }
        publishCommand(WebArCommand.ActivateWordTarget(target))

        if (wordCache.containsKey(word)) {
            applyWordLookupResult(target, wordCache[word])
            return
        }

        lookupJob = viewModelScope.launch {
            runCatching { arAssetRepository.findByWord(word) }
                .onSuccess { asset ->
                    if (version != requestVersion || _uiState.value.activeWordTarget?.word != word) {
                        return@onSuccess
                    }
                    wordCache[word] = asset
                    applyWordLookupResult(target, asset)
                }
                .onFailure {
                    if (version != requestVersion || _uiState.value.activeWordTarget?.word != word) {
                        return@onFailure
                    }
                    publishError(word, "No fue posible cargar el contenido de la palabra")
                }
        }
    }

    fun onOcrWordLost(rawWord: String) {
        val word = OcrWordNormalizer.normalize(rawWord) ?: return
        if (_uiState.value.activeWordTarget?.word != word) return
        lookupJob?.cancel()
        pendingWordPosition = null
        requestVersion += 1
        _uiState.update {
            it.copy(
                activeWordTarget = null,
                asset = null,
                isAssetLoading = false,
                wordWithoutAssociation = false,
                statusMessage = "Palabra perdida. Enfoca una palabra o tarjeta.",
                errorMessage = null
            )
        }
        publishCommand(WebArCommand.ClearWordTarget(word))
    }

    fun onOcrWordPositionUpdated(
        rawWord: String,
        confidence: Float,
        centerX: Float,
        centerY: Float
    ) {
        val word = OcrWordNormalizer.normalize(rawWord) ?: return
        if (!confidence.isFinite() || confidence < MIN_OCR_CONFIDENCE) return
        if (!centerX.isFinite() || !centerY.isFinite()) return

        val current = _uiState.value
        val activeTarget = current.activeWordTarget ?: return
        if (current.activeMarkerId != null || activeTarget.word != word) return
        val updatedTarget = activeTarget.copy(
            confidence = confidence.coerceIn(0f, 1f),
            centerX = centerX.coerceIn(0f, 1f),
            centerY = centerY.coerceIn(0f, 1f)
        )
        _uiState.update { it.copy(activeWordTarget = updatedTarget) }
        // Preserve asset-ready while the lookup/model is pending, then apply the newest position.
        if (current.isAssetLoading || current.asset == null || current.wordWithoutAssociation) {
            pendingWordPosition = updatedTarget
            return
        }
        publishCommand(WebArCommand.ActivateWordTarget(updatedTarget))
    }

    fun retryActiveTarget() {
        val state = _uiState.value
        val markerId = state.activeMarkerId
        if (markerId != null) {
            markerCache.remove(markerId)
            _uiState.update { it.copy(activeMarkerId = null) }
            onMarkerFound(markerId)
            return
        }

        val target = state.activeWordTarget ?: return
        wordCache.remove(target.word)
        _uiState.update { it.copy(activeWordTarget = null) }
        onOcrWordDetected(target.word, target.confidence, target.centerX, target.centerY)
    }

    fun retryActiveMarker() = retryActiveTarget()

    fun onCameraReady() {
        val current = _uiState.value
        if (current.activeMarkerId != null || current.activeWordTarget != null) return
        _uiState.update {
            it.copy(
                statusMessage = "Enfoca una palabra o tarjeta",
                errorMessage = null
            )
        }
    }

    fun onExperienceStopped() {
        lookupJob?.cancel()
        lookupJob = null
        pendingWordPosition = null
        requestVersion += 1
        _uiState.value = WebArUiState()
    }

    fun onModelReady(rawAssetId: String) {
        val current = _uiState.value
        val asset = current.asset ?: return
        if (!current.isAssetLoading || asset.id != rawAssetId.trim()) return
        _uiState.update {
            it.copy(
                isAssetLoading = false,
                statusMessage = "Mostrando ${asset.word}",
                errorMessage = null
            )
        }
        val latestPosition = pendingWordPosition
            ?.takeIf { it.word == _uiState.value.activeWordTarget?.word }
        pendingWordPosition = null
        if (latestPosition != null) {
            publishCommand(WebArCommand.ActivateWordTarget(latestPosition))
        }
    }

    fun onModelError(rawAssetId: String, rawMessage: String) {
        val current = _uiState.value
        val asset = current.asset ?: return
        if (!current.isAssetLoading || asset.id != rawAssetId.trim()) return
        val message = rawMessage.trim()
            .ifBlank { "No fue posible renderizar el modelo 3D" }
            .take(MAX_ERROR_LENGTH)
        pendingWordPosition = null
        _uiState.update {
            it.copy(
                isAssetLoading = false,
                statusMessage = "No pudimos mostrar ${asset.word}",
                errorMessage = message
            )
        }
    }

    fun onWebError(message: String) {
        lookupJob?.cancel()
        lookupJob = null
        pendingWordPosition = null
        requestVersion += 1
        _uiState.value = WebArUiState(
            requiresCameraRestart = true,
            statusMessage = "La experiencia AR encontró un problema",
            errorMessage = message.trim()
                .ifBlank { "No fue posible iniciar la experiencia AR" }
                .take(MAX_ERROR_LENGTH)
        )
    }

    fun onCameraPermissionDenied() {
        _uiState.update {
            it.copy(
                statusMessage = "Permiso de cámara denegado",
                errorMessage = "La cámara es necesaria para reconocer palabras y tarjetas."
            )
        }
        publishCommand(WebArCommand.CameraPermissionDenied)
    }

    fun onCommandDelivered(commandId: Long) {
        _uiState.update { state ->
            if (state.outboundCommand?.id == commandId) {
                state.copy(outboundCommand = null)
            } else {
                state
            }
        }
    }

    private fun applyMarkerLookupResult(markerId: String, asset: ArAsset?) {
        if (asset == null) {
            _uiState.update {
                it.copy(
                    isAssetLoading = false,
                    markerWithoutAssociation = true,
                    statusMessage = "El marcador no tiene contenido asociado"
                )
            }
            publishCommand(WebArCommand.MarkerNotFound(markerId))
            return
        }

        _uiState.update {
            it.copy(
                asset = asset,
                isAssetLoading = true,
                markerWithoutAssociation = false,
                statusMessage = "Cargando modelo de ${asset.word}…",
                errorMessage = null
            )
        }
        publishCommand(WebArCommand.AssetReady(asset))
    }

    private fun applyWordLookupResult(target: WebArWordTarget, asset: ArAsset?) {
        val latestTarget = _uiState.value.activeWordTarget
            ?.takeIf { it.word == target.word }
            ?: target
        if (asset == null) {
            pendingWordPosition = null
            _uiState.update {
                it.copy(
                    isAssetLoading = false,
                    wordWithoutAssociation = true,
                    statusMessage = "No hay contenido asociado con «${target.word}»"
                )
            }
            publishCommand(WebArCommand.WordNotFound(latestTarget))
            return
        }

        pendingWordPosition = null
        _uiState.update {
            it.copy(
                asset = asset,
                isAssetLoading = true,
                wordWithoutAssociation = false,
                statusMessage = "Cargando modelo de ${asset.word}…",
                errorMessage = null
            )
        }
        // Target metadata lets WebAR activate and render the OCR result atomically.
        publishCommand(WebArCommand.AssetReady(asset, latestTarget))
    }

    private fun publishError(targetId: String, message: String) {
        _uiState.update {
            it.copy(
                isAssetLoading = false,
                markerWithoutAssociation = false,
                wordWithoutAssociation = false,
                statusMessage = "No se pudo cargar el contenido",
                errorMessage = message
            )
        }
        publishCommand(WebArCommand.AssetError(targetId, message))
    }

    private fun publishCommand(command: WebArCommand) {
        _uiState.update { it.copy(outboundCommand = envelope(command)) }
    }

    private fun envelope(command: WebArCommand): WebArCommandEnvelope {
        commandVersion += 1
        return WebArCommandEnvelope(commandVersion, command)
    }

    private companion object {
        val MARKER_ID_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$")
        const val MIN_OCR_CONFIDENCE = 0.60f
        const val MAX_ERROR_LENGTH = 240
    }
}

class WebArViewModelFactory(
    private val arAssetRepository: ArAssetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WebArViewModel::class.java)) {
            return WebArViewModel(arAssetRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
