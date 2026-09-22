package co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.inclusivereadingar.data.remote.BackendException
import co.edu.uniautonoma.inclusivereadingar.data.repository.StudentTodayDataSource
import co.edu.uniautonoma.inclusivereadingar.domain.model.DailyPlanSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

/**
 * UI state for the single "today" screen shown to a student, merging the active study plan
 * identity with today's due activities. Kept as a sealed hierarchy (instead of a flags-based data
 * class) so a real fetch failure can never be silently rendered as the "up to date" empty state —
 * see the spec's `/doman/sessions?plan_id=` authorization risk scenario.
 */
sealed interface StudentTodayUiState {
    data object Loading : StudentTodayUiState

    data object NoActivePlan : StudentTodayUiState

    data class UpToDate(
        val planName: String?,
        val levelName: String?
    ) : StudentTodayUiState

    data class DueToday(
        val planName: String?,
        val levelName: String?,
        val activities: List<DailyPlanSummary>
    ) : StudentTodayUiState

    data class Error(val message: String) : StudentTodayUiState
}

class StudentTodayViewModel(
    private val dataSource: StudentTodayDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow<StudentTodayUiState>(StudentTodayUiState.Loading)
    val uiState: StateFlow<StudentTodayUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = StudentTodayUiState.Loading
            runCatching {
                // supervisorScope: a failure in one call must not cancel the sibling call before
                // its own await() delivers the exception to this runCatching (plain async/await
                // under a shared non-supervisor scope propagates child failures eagerly via
                // structured-concurrency cancellation, bypassing this try/catch).
                supervisorScope {
                    val planDeferred = async { dataSource.getActivePlanForCurrentStudent() }
                    val activitiesDeferred = async { dataSource.getTodayActivitiesForCurrentStudent() }
                    planDeferred.await() to activitiesDeferred.await()
                }
            }.onSuccess { (plan, activitiesResult) ->
                _uiState.value = when {
                    plan == null -> StudentTodayUiState.NoActivePlan

                    activitiesResult.countsLoadFailed -> StudentTodayUiState.Error(
                        "No fue posible cargar tu actividad de hoy. Intenta de nuevo."
                    )

                    activitiesResult.activities.isEmpty() ||
                        activitiesResult.activities.all { it.pendingSessionsCount == 0 } ->
                        StudentTodayUiState.UpToDate(planName = plan.planName, levelName = plan.levelName)

                    else -> StudentTodayUiState.DueToday(
                        planName = plan.planName,
                        levelName = plan.levelName,
                        activities = activitiesResult.activities
                    )
                }
            }.onFailure { error ->
                _uiState.value = StudentTodayUiState.Error(
                    error.toUiMessage("No fue posible cargar tu actividad de hoy.")
                )
            }
        }
    }
}

class StudentTodayViewModelFactory(
    private val dataSource: StudentTodayDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentTodayViewModel::class.java)) {
            return StudentTodayViewModel(dataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private fun Throwable.toUiMessage(fallback: String): String = when (this) {
    is BackendException -> message
    else -> message?.takeIf { it.isNotBlank() } ?: fallback
}
