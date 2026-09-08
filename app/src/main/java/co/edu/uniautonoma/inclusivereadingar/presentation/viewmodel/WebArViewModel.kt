package co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.domain.model.ArAsset
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

    private val sessionCache = mutableMapOf<String, ArAsset?>()
    private var lookupJob: Job? = null
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
        _uiState.update {
            it.copy(
                activeMarkerId = markerId,
                asset = null,
                isAssetLoading = true,
                markerWithoutAssociation = false,
                statusMessage = "Consultando contenido…",
                errorMessage = null
            )
        }

        if (sessionCache.containsKey(markerId)) {
            applyLookupResult(markerId, sessionCache[markerId])
            return
        }

        lookupJob = viewModelScope.launch {
            runCatching { arAssetRepository.findByMarker(markerId) }
                .onSuccess { asset ->
                    if (version != requestVersion || _uiState.value.activeMarkerId != markerId) return@onSuccess
                    sessionCache[markerId] = asset
                    applyLookupResult(markerId, asset)
                }
                .onFailure {
                    if (version != requestVersion || _uiState.value.activeMarkerId != markerId) return@onFailure
                    publishError(markerId, "No fue posible cargar el contenido del marcador")
                }
        }
    }

    fun onMarkerLost(rawMarkerId: String) {
        val markerId = rawMarkerId.trim()
        if (_uiState.value.activeMarkerId != markerId) return
        lookupJob?.cancel()
        requestVersion += 1
        _uiState.update {
            it.copy(
                activeMarkerId = null,
                asset = null,
                isAssetLoading = false,
                markerWithoutAssociation = false,
                statusMessage = "Marcador perdido. Enfoca una tarjeta.",
                errorMessage = null,
                outboundCommand = envelope(WebArCommand.ClearMarker(markerId))
            )
        }
    }

    fun retryActiveMarker() {
        val markerId = _uiState.value.activeMarkerId ?: return
        sessionCache.remove(markerId)
        _uiState.update { it.copy(activeMarkerId = null) }
        onMarkerFound(markerId)
    }

    fun onWebError(message: String) {
        _uiState.update {
            it.copy(
                isAssetLoading = false,
                statusMessage = "La experiencia AR encontró un problema",
                errorMessage = message.take(MAX_ERROR_LENGTH)
            )
        }
    }

    fun onCameraPermissionDenied() {
        _uiState.update {
            it.copy(
                statusMessage = "Permiso de cámara denegado",
                errorMessage = "La cámara es necesaria para reconocer las tarjetas.",
                outboundCommand = envelope(WebArCommand.CameraPermissionDenied)
            )
        }
    }

    fun onCommandDelivered(commandId: Long) {
        _uiState.update { state ->
            if (state.outboundCommand?.id == commandId) state.copy(outboundCommand = null) else state
        }
    }

    private fun applyLookupResult(markerId: String, asset: ArAsset?) {
        if (asset == null) {
            _uiState.update {
                it.copy(
                    isAssetLoading = false,
                    markerWithoutAssociation = true,
                    statusMessage = "El marcador no tiene contenido asociado",
                    outboundCommand = envelope(WebArCommand.MarkerNotFound(markerId))
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                asset = asset,
                isAssetLoading = false,
                markerWithoutAssociation = false,
                statusMessage = "Mostrando ${asset.word}",
                errorMessage = null,
                outboundCommand = envelope(WebArCommand.AssetReady(asset))
            )
        }
    }

    private fun publishError(markerId: String, message: String) {
        _uiState.update {
            it.copy(
                isAssetLoading = false,
                markerWithoutAssociation = false,
                statusMessage = "No se pudo cargar el contenido",
                errorMessage = message,
                outboundCommand = envelope(WebArCommand.AssetError(markerId, message))
            )
        }
    }

    private fun envelope(command: WebArCommand): WebArCommandEnvelope {
        commandVersion += 1
        return WebArCommandEnvelope(commandVersion, command)
    }

    private companion object {
        val MARKER_ID_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$")
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
