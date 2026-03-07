package co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentProgress
import co.edu.uniautonoma.inclusivereadingar.domain.repository.LearningRepository
import co.edu.uniautonoma.inclusivereadingar.domain.usecase.GetDailyWordsUseCase
import co.edu.uniautonoma.inclusivereadingar.domain.usecase.RegisterProgressUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

class WebArViewModel(
    private val getDailyWordsUseCase: GetDailyWordsUseCase,
    private val registerProgressUseCase: RegisterProgressUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(WebArUiState())
    val uiState: StateFlow<WebArUiState> = _uiState.asStateFlow()

    init {
        refreshContent()
    }

    fun refreshContent() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    generalError = null,
                    webError = null
                )
            }

            runCatching { getDailyWordsUseCase() }
                .onSuccess { units ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            units = units,
                            currentWordIndex = 0,
                            completedPractices = 0,
                            lastRegisteredWord = null,
                            generalError = null
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = "No fue posible cargar el contenido de práctica."
                        )
                    }
                }
        }
    }

    fun nextWord() {
        _uiState.update { state ->
            if (state.units.isEmpty()) {
                state
            } else {
                val lastIndex = state.units.lastIndex
                val nextIndex = (state.currentWordIndex + 1).coerceAtMost(lastIndex)
                state.copy(currentWordIndex = nextIndex)
            }
        }
    }

    fun previousWord() {
        _uiState.update { state ->
            if (state.units.isEmpty()) {
                state
            } else {
                val previousIndex = (state.currentWordIndex - 1).coerceAtLeast(0)
                state.copy(currentWordIndex = previousIndex)
            }
        }
    }

    fun registerSimulatedRead() {
        val state = _uiState.value
        val selected = state.units.getOrNull(state.currentWordIndex) ?: return

        viewModelScope.launch {
            val progress = StudentProgress(
                studentId = "demo-student",
                learningUnitId = selected.id,
                markerId = selected.markerId,
                success = true,
                timestamp = Instant.now()
            )

            runCatching { registerProgressUseCase(progress) }
                .onSuccess {
                    _uiState.update { current ->
                        val size = current.units.size
                        val nextIndex = if (size == 0) 0 else (current.currentWordIndex + 1) % size
                        current.copy(
                            currentWordIndex = nextIndex,
                            completedPractices = current.completedPractices + 1,
                            lastRegisteredWord = selected.word,
                            generalError = null
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(generalError = "No fue posible registrar el avance en este momento.")
                    }
                }
        }
    }

    fun onWebError(message: String) {
        _uiState.update { it.copy(webError = message) }
    }

    fun clearWebError() {
        _uiState.update { it.copy(webError = null) }
    }
}

class WebArViewModelFactory(private val repository: LearningRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WebArViewModel::class.java)) {
            return WebArViewModel(
                getDailyWordsUseCase = GetDailyWordsUseCase(repository),
                registerProgressUseCase = RegisterProgressUseCase(repository)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
