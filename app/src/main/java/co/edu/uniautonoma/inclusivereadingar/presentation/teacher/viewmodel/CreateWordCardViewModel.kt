package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.remote.BackendException
import co.edu.uniautonoma.inclusivereadingar.data.repository.TeacherContentDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.AppUser
import co.edu.uniautonoma.inclusivereadingar.domain.model.AudioUploadInput
import co.edu.uniautonoma.inclusivereadingar.domain.model.Category
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException

data class LocalAudioSelection(
    val filePath: String,
    val mimeType: String?,
    val originalName: String?
)

sealed interface TeacherAudioSource {
    data class Url(val value: String) : TeacherAudioSource
    data class File(val localAudio: LocalAudioSelection) : TeacherAudioSource
    data class Recorded(val localAudio: LocalAudioSelection) : TeacherAudioSource
}

data class CreateWordCardUiState(
    val isLoadingStudents: Boolean = true,
    val isLoadingCategories: Boolean = true,
    val isSaving: Boolean = false,
    val students: List<AppUser> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedStudentId: String? = null,
    val word: String = "",
    val selectedCategoryId: String? = null,
    val audioUrlInput: String = "",
    val audioSource: TeacherAudioSource? = null,
    val errorMessage: String? = null,
    val saved: Boolean = false
) {
    val canSave: Boolean
        get() = selectedStudentId != null && word.isNotBlank() && selectedCategoryId != null

    val hasAudio: Boolean
        get() = audioSource != null
}

class CreateWordCardViewModel(
    private val repository: TeacherContentDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateWordCardUiState())
    val uiState: StateFlow<CreateWordCardUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun selectStudent(id: String) {
        _uiState.update { it.copy(selectedStudentId = id, errorMessage = null) }
    }

    fun updateWord(value: String) {
        _uiState.update { it.copy(word = value.take(15), errorMessage = null) }
    }

    fun selectCategory(id: String) {
        _uiState.update { it.copy(selectedCategoryId = id, errorMessage = null) }
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
        if (!snapshot.canSave) {
            _uiState.update { it.copy(errorMessage = "Completa estudiante, palabra y temática.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                val finalAudioUrl = resolveFinalAudioUrl(snapshot.audioSource)
                repository.createWordCard(
                    studentId = requireNotNull(snapshot.selectedStudentId),
                    word = snapshot.word.trim(),
                    categoryId = requireNotNull(snapshot.selectedCategoryId),
                    audioUrl = finalAudioUrl
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, saved = true) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.toAudioUiMessage()
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
                val upload = repository.uploadAudio(
                    AudioUploadInput(
                        filePath = audioSource.localAudio.filePath,
                        mimeType = audioSource.localAudio.mimeType,
                        originalName = audioSource.localAudio.originalName
                    )
                )
                upload.audioUrl
            }
            is TeacherAudioSource.Recorded -> {
                val upload = repository.uploadAudio(
                    AudioUploadInput(
                        filePath = audioSource.localAudio.filePath,
                        mimeType = audioSource.localAudio.mimeType,
                        originalName = audioSource.localAudio.originalName
                    )
                )
                upload.audioUrl
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            runCatching {
                awaitAll(
                    async { repository.getStudents() },
                    async { repository.getCategories() }
                )
            }.onSuccess { results ->
                @Suppress("UNCHECKED_CAST")
                val students = results[0] as List<AppUser>
                @Suppress("UNCHECKED_CAST")
                val categories = results[1] as List<Category>
                _uiState.update {
                    it.copy(
                        isLoadingStudents = false,
                        isLoadingCategories = false,
                        students = students,
                        categories = categories.sortedBy { category -> category.sortOrder }
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingStudents = false,
                        isLoadingCategories = false,
                        errorMessage = error.toUiMessage("No fue posible cargar la información inicial.")
                    )
                }
            }
        }
    }
}

class CreateWordCardViewModelFactory(
    private val repository: TeacherContentDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateWordCardViewModel::class.java)) {
            return CreateWordCardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private fun Throwable.toAudioUiMessage(): String {
    if (this is SocketTimeoutException) {
        return "Tiempo de espera agotado al subir el audio. Intenta nuevamente."
    }
    if (this is BackendException) {
        val normalized = message.lowercase()
        return when {
            statusCode == 413 || normalized.contains("exceeds 10mb") || normalized.contains("file too large") ->
                "El audio supera el límite de 10MB."
            statusCode == 400 && normalized.contains("unsupported audio format") ->
                "Formato no permitido. Usa m4a, mp3 o wav."
            statusCode == 401 || normalized.contains("bearer token") ->
                "Tu sesión expiró. Inicia sesión nuevamente."
            else -> message
        }
    }
    return toUiMessage("No fue posible guardar la tarjeta.")
}
