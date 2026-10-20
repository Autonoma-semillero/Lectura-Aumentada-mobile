package co.edu.uniautonoma.inclusivereadingar.presentation.mascot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Moments of the learning flow the mascot reacts to. Screens report cues, never animations. */
enum class MascotCue {
    ScreenStarted,
    AnswerCorrect,
    AnswerIncorrect,
    LessonCompleted,
    Thinking,
    UserInteraction,
}

internal fun MascotCue.toEvent(): MascotEvent? = when (this) {
    MascotCue.ScreenStarted -> MascotEvent.Wave
    MascotCue.AnswerCorrect, MascotCue.LessonCompleted -> MascotEvent.Celebrate
    MascotCue.AnswerIncorrect -> MascotEvent.TryAgain
    MascotCue.Thinking -> MascotEvent.Think
    MascotCue.UserInteraction -> null
}

data class MascotUiState(
    val expression: MascotExpression = MascotExpression.Happy,
    val lastEvent: MascotEvent? = null,
)

/**
 * Owns the [MascotController] so the mascot state survives rotation, and turns learning cues
 * into mascot behaviour. After [idleAfterMs] without any cue the mascot gets curious for
 * [curiousForMs], then relaxes again (and repeats while the user stays idle).
 */
class MascotViewModel(
    val controller: MascotController = MascotController(),
    private val idleAfterMs: Long = DEFAULT_IDLE_AFTER_MS,
    private val curiousForMs: Long = DEFAULT_CURIOUS_FOR_MS,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MascotUiState())
    val uiState: StateFlow<MascotUiState> = _uiState.asStateFlow()

    private var idleJob: Job? = null

    init {
        scheduleIdle()
    }

    fun onCue(cue: MascotCue) {
        // Any cue means the user is active: relax a curious face before reacting.
        if (_uiState.value.expression == MascotExpression.Curious) {
            updateExpression(MascotExpression.Happy)
        }
        cue.toEvent()?.let(::play)
        scheduleIdle()
    }

    fun play(event: MascotEvent) {
        controller.play(event)
        _uiState.update { it.copy(lastEvent = event) }
        if (event == MascotEvent.Idle) updateExpression(MascotExpression.Happy)
    }

    fun setExpression(expression: MascotExpression) {
        updateExpression(expression)
        scheduleIdle()
    }

    fun lookAt(x: Float, y: Float) {
        controller.lookAt(x, y)
        scheduleIdle()
    }

    private fun updateExpression(expression: MascotExpression) {
        controller.setExpression(expression)
        _uiState.update { it.copy(expression = expression) }
    }

    private fun scheduleIdle() {
        idleJob?.cancel()
        idleJob = viewModelScope.launch {
            while (true) {
                delay(idleAfterMs)
                updateExpression(MascotExpression.Curious)
                delay(curiousForMs)
                updateExpression(MascotExpression.Happy)
            }
        }
    }

    companion object {
        const val DEFAULT_IDLE_AFTER_MS = 20_000L
        const val DEFAULT_CURIOUS_FOR_MS = 4_000L
    }
}
