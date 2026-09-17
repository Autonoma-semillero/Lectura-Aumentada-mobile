package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.remote.BackendException
import co.edu.uniautonoma.inclusivereadingar.data.repository.TeacherContentDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.ArModelOption
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudioUploadInput
import co.edu.uniautonoma.inclusivereadingar.domain.model.SUPPORTED_AR_MARKERS
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException

data class EditWordCardUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val word: String = "",
    val originalWord: String = "",
    val arModels: List<ArModelOption> = emptyList(),
    val selectedArModelId: String? = null,
    val selectedMarkerId: String? = null,
    val audioUrlInput: String = "",
    val audioSource: TeacherAudioSource? = null,
    val errorMessage: String? = null,
    val saved: Boolean = false
) {
    val hasAudio: Boolean get() = audioSource != null
    val canSave: Boolean get() = word.isNotBlank()
    val hasArAssociation: Boolean
        get() = selectedArModelId != null && selectedMarkerId != null
}

class EditWordCardViewModel(
    private val cardId: String,
    private val repository: TeacherContentDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditWordCardUiState())
    val uiState: StateFlow<EditWordCardUiState> = _uiState.asStateFlow()

    init {
        loadCard()
    }

    private fun loadCard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                coroutineScope {
                    val card = async { repository.getWordCardById(cardId) }
                    val arModels = async { repository.getArModels() }
                    card.await() to arModels.await()
                }
            }
                .onSuccess { (card, arModels) ->
                    val existingAudio = card.audioUrl?.ifBlank { null }
                    val selectedModel = arModels.firstOrNull {
                        it.learningUnitId == card.learningUnitId
                    }
                    val supportedMarkerIds = SUPPORTED_AR_MARKERS.map { it.markerId }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            word = card.word,
                            originalWord = card.word,
                            arModels = arModels,
                            selectedArModelId = selectedModel?.learningUnitId,
                            selectedMarkerId = selectedModel?.markerId
                                ?.takeIf { markerId -> markerId in supportedMarkerIds },
                            audioUrlInput = existingAudio ?: "",
                            audioSource = existingAudio?.let { url -> TeacherAudioSource.Url(url) }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No fue posible cargar la tarjeta."
                        )
                    }
                }
        }
    }

    fun updateWord(value: String) {
        _uiState.update { it.copy(word = value.take(15), errorMessage = null) }
    }

    fun selectArModel(learningUnitId: String) {
        _uiState.update { state ->
            val model = state.arModels.firstOrNull { it.learningUnitId == learningUnitId }
                ?: return@update state
            val supportedMarkerIds = SUPPORTED_AR_MARKERS.map { it.markerId }
            state.copy(
                selectedArModelId = model.learningUnitId,
                selectedMarkerId = model.markerId.takeIf { it in supportedMarkerIds }
                    ?: supportedMarkerIds.firstOrNull(),
                errorMessage = null
            )
        }
    }

    fun selectMarker(markerId: String) {
        if (SUPPORTED_AR_MARKERS.none { it.markerId == markerId }) return
        _uiState.update { it.copy(selectedMarkerId = markerId, errorMessage = null) }
    }

    fun updateAudioUrl(value: String) {
        val trimmed = value.trim()
        _uiState.update {
            it.copy(
                audioUrlInput = value,
                audioSource = if (trimmed.isBlank()) null else TeacherAudioSource.Url(trimmed),
                errorMessage = null
            )
        }
    }

    fun selectAudioFile(localAudio: LocalAudioSelection) {
        _uiState.update {
            it.copy(
                audioUrlInput = "",
                audioSource = TeacherAudioSource.File(localAudio),
                errorMessage = null
            )
        }
    }

    fun selectRecordedAudio(localAudio: LocalAudioSelection) {
        _uiState.update {
            it.copy(
                audioUrlInput = "",
                audioSource = TeacherAudioSource.Recorded(localAudio),
                errorMessage = null
            )
        }
    }

    fun setErrorMessage(message: String?) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun save() {
        val snapshot = uiState.value
        if (!snapshot.canSave) return

        val selectedModel = snapshot.selectedArModelId?.let { learningUnitId ->
            snapshot.arModels.firstOrNull { it.learningUnitId == learningUnitId }
        }
        if (snapshot.selectedArModelId != null &&
            (selectedModel == null || snapshot.selectedMarkerId == null)
        ) {
            _uiState.update { it.copy(errorMessage = "Selecciona también un marcador para el modelo 3D.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                val finalAudioUrl = resolveFinalAudioUrl(snapshot.audioSource)
                if (selectedModel != null) {
                    repository.associateArContent(
                        learningUnitId = selectedModel.learningUnitId,
                        markerId = requireNotNull(snapshot.selectedMarkerId),
                        model3dUrl = selectedModel.model3dUrl
                    )
                }
                repository.updateWordCard(
                    id = cardId,
                    word = snapshot.word.trim(),
                    audioUrl = finalAudioUrl,
                    learningUnitId = selectedModel?.learningUnitId
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, saved = true) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.toEditUiMessage()
                    )
                }
            }
        }
    }

    private suspend fun resolveFinalAudioUrl(audioSource: TeacherAudioSource?): String? {
        return when (audioSource) {
            null -> null
            is TeacherAudioSource.Url -> audioSource.value.trim().ifBlank { null }
            is TeacherAudioSource.File -> {
                repository.uploadAudio(
                    AudioUploadInput(
                        filePath = audioSource.localAudio.filePath,
                        mimeType = audioSource.localAudio.mimeType,
                        originalName = audioSource.localAudio.originalName
                    )
                ).audioUrl
            }
            is TeacherAudioSource.Recorded -> {
                repository.uploadAudio(
                    AudioUploadInput(
                        filePath = audioSource.localAudio.filePath,
                        mimeType = audioSource.localAudio.mimeType,
                        originalName = audioSource.localAudio.originalName
                    )
                ).audioUrl
            }
        }
    }
}

private fun Throwable.toEditUiMessage(): String {
    if (this is SocketTimeoutException) return "Tiempo de espera agotado. Intenta nuevamente."
    if (this is BackendException && statusCode == 409 && message.lowercase().contains("marker")) {
        return "Ese marcador ya está asociado a otro modelo 3D. Selecciona el marcador original del modelo."
    }
    return message ?: "No fue posible guardar los cambios."
}

class EditWordCardViewModelFactory(
    private val cardId: String,
    private val repository: TeacherContentDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditWordCardViewModel::class.java)) {
            return EditWordCardViewModel(cardId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
