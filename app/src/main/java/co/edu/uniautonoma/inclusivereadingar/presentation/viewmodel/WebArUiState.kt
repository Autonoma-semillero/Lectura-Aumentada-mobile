package co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel

import co.edu.uniautonoma.inclusivereadingar.domain.model.ArAsset

data class WebArUiState(
    val activeMarkerId: String? = null,
    val activeWordTarget: WebArWordTarget? = null,
    val asset: ArAsset? = null,
    val isAssetLoading: Boolean = false,
    val markerWithoutAssociation: Boolean = false,
    val wordWithoutAssociation: Boolean = false,
    val requiresCameraRestart: Boolean = false,
    val statusMessage: String = "Activa la cámara para comenzar",
    val errorMessage: String? = null,
    val outboundCommand: WebArCommandEnvelope? = null
)

data class WebArWordTarget(
    val word: String,
    val confidence: Float,
    val centerX: Float,
    val centerY: Float
)

data class WebArCommandEnvelope(
    val id: Long,
    val command: WebArCommand
)

sealed interface WebArCommand {
    data class AssetReady(
        val asset: ArAsset,
        val wordTarget: WebArWordTarget? = null
    ) : WebArCommand
    data class MarkerNotFound(val markerId: String) : WebArCommand
    data class WordNotFound(val target: WebArWordTarget) : WebArCommand
    data class AssetError(val markerId: String, val message: String) : WebArCommand
    data class ClearMarker(val markerId: String) : WebArCommand
    data class ActivateWordTarget(val target: WebArWordTarget) : WebArCommand
    data class ClearWordTarget(val word: String?) : WebArCommand
    data object CameraPermissionDenied : WebArCommand
}
