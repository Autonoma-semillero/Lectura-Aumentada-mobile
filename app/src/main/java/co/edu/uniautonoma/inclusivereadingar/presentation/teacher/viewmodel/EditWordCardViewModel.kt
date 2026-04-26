package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.TeacherContentDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudioUploadInput
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
    val audioUrlInput: String = "",
    val audioSource: TeacherAudioSource? = null,
    val errorMessage: String? = null,
    val saved: Boolean = false
) {
    val hasAudio: Boolean get() = audioSource != null
    val canSave: Boolean get() = word.isNotBlank()
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
            runCatching { repository.getWordCardById(cardId) }
                .onSuccess { card ->
                    val existingAudio = card.audioUrl?.ifBlank { null }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            word = card.word,
                            originalWord = card.word,
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

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                val finalAudioUrl = resolveFinalAudioUrl(snapshot.audioSource)
                repository.updateWordCard(
                    id = cardId,
                    word = snapshot.word.trim(),
                    audioUrl = finalAudioUrl
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
