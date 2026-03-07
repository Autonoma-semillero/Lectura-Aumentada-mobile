package co.edu.uniautonoma.inclusivereadingar.presentation.viewmodel

import co.edu.uniautonoma.inclusivereadingar.domain.model.LearningUnit

data class WebArUiState(
    val isLoading: Boolean = true,
    val units: List<LearningUnit> = emptyList(),
    val currentWordIndex: Int = 0,
    val completedPractices: Int = 0,
    val webError: String? = null,
    val generalError: String? = null,
    val lastRegisteredWord: String? = null
)
