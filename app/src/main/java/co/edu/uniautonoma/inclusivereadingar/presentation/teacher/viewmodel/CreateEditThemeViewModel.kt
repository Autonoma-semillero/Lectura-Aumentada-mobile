package co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.repository.TeacherContentDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateEditThemeUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val name: String = "",
    val icon: String = "restaurant",
    val sortOrder: Int = 0,
    val description: String = "",
    val errorMessage: String? = null,
    val saved: Boolean = false
)

class CreateEditThemeViewModel(
    private val themeId: String?,
    private val repository: TeacherContentDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateEditThemeUiState(isLoading = themeId != null))
    val uiState: StateFlow<CreateEditThemeUiState> = _uiState.asStateFlow()

    init {
        if (themeId != null) {
            loadTheme(themeId)
        }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null) }
    }

    fun updateIcon(value: String) {
        _uiState.update { it.copy(icon = value, errorMessage = null) }
    }

    fun updateSortOrder(value: Int) {
        _uiState.update { it.copy(sortOrder = value.coerceAtLeast(0), errorMessage = null) }
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value, errorMessage = null) }
    }

    fun save() {
        val snapshot = uiState.value
        if (snapshot.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa un nombre para la temática.") }
            return
        }

        val slug = snapshot.name
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                if (themeId == null) {
                    repository.createCategory(
                        name = snapshot.name.trim(),
                        slug = slug,
                        description = snapshot.description.trim().ifBlank { null },
                        icon = snapshot.icon,
                        sortOrder = snapshot.sortOrder
                    )
                } else {
                    repository.updateCategory(
                        id = themeId,
                        name = snapshot.name.trim(),
                        slug = slug,
                        description = snapshot.description.trim().ifBlank { null },
                        icon = snapshot.icon,
                        sortOrder = snapshot.sortOrder
                    )
                }
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, saved = true) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.toUiMessage("No fue posible guardar la temática.")
                    )
                }
            }
        }
    }

    private fun loadTheme(targetThemeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.getCategories().firstOrNull { it.id == targetThemeId }
            }.onSuccess { category ->
                if (category == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No se encontró la temática solicitada."
                        )
                    }
                    return@onSuccess
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        name = category.name,
                        icon = category.icon ?: "restaurant",
                        sortOrder = category.sortOrder,
                        description = category.description.orEmpty(),
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.toUiMessage("No fue posible cargar la temática.")
                    )
                }
            }
        }
    }
}

class CreateEditThemeViewModelFactory(
    private val themeId: String?,
    private val repository: TeacherContentDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateEditThemeViewModel::class.java)) {
            return CreateEditThemeViewModel(themeId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
