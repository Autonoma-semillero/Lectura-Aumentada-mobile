package co.edu.uniautonoma.inclusivereadingar.presentation.mascot

import co.edu.uniautonoma.inclusivereadingar.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MascotViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private class FakeInputs : MascotInputs {
        val triggers = mutableListOf<String>()
        val numbers = mutableMapOf<String, Float>()
        override fun fireTrigger(name: String) { triggers += name }
        override fun setNumber(name: String, value: Float) { numbers[name] = value }
    }

    private fun createViewModel(inputs: FakeInputs): MascotViewModel {
        val controller = MascotController().apply { attach(inputs) }
        return MascotViewModel(controller, idleAfterMs = 1_000, curiousForMs = 200)
    }

    @Test
    fun `learning cues map to the expected mascot actions`() {
        val inputs = FakeInputs()
        val viewModel = createViewModel(inputs)

        viewModel.onCue(MascotCue.ScreenStarted)
        viewModel.onCue(MascotCue.AnswerCorrect)
        viewModel.onCue(MascotCue.AnswerIncorrect)
        viewModel.onCue(MascotCue.LessonCompleted)
        viewModel.onCue(MascotCue.Thinking)
        viewModel.onCue(MascotCue.UserInteraction)

        assertThat(inputs.triggers)
            .containsExactly("wave", "celebrate", "tryAgain", "celebrate", "think").inOrder()
        assertThat(viewModel.uiState.value.lastEvent).isEqualTo(MascotEvent.Think)
    }

    @Test
    fun `mascot gets curious after inactivity and relaxes afterwards`() {
        val inputs = FakeInputs()
        val viewModel = createViewModel(inputs)
        dispatcher.scheduler.runCurrent()

        dispatcher.scheduler.advanceTimeBy(999)
        dispatcher.scheduler.runCurrent()
        assertThat(viewModel.uiState.value.expression).isEqualTo(MascotExpression.Happy)

        dispatcher.scheduler.advanceTimeBy(2)
        dispatcher.scheduler.runCurrent()
        assertThat(viewModel.uiState.value.expression).isEqualTo(MascotExpression.Curious)
        assertThat(inputs.numbers[MascotInputNames.EXPRESSION]).isEqualTo(MascotExpression.Curious.value.toFloat())

        dispatcher.scheduler.advanceTimeBy(200)
        dispatcher.scheduler.runCurrent()
        assertThat(viewModel.uiState.value.expression).isEqualTo(MascotExpression.Happy)
    }

    @Test
    fun `a cue restarts the inactivity timer and relaxes a curious face`() {
        val inputs = FakeInputs()
        val viewModel = createViewModel(inputs)
        dispatcher.scheduler.runCurrent()
        dispatcher.scheduler.advanceTimeBy(1_050)
        dispatcher.scheduler.runCurrent()
        assertThat(viewModel.uiState.value.expression).isEqualTo(MascotExpression.Curious)

        viewModel.onCue(MascotCue.UserInteraction)
        assertThat(viewModel.uiState.value.expression).isEqualTo(MascotExpression.Happy)

        dispatcher.scheduler.advanceTimeBy(900)
        dispatcher.scheduler.runCurrent()
        assertThat(viewModel.uiState.value.expression).isEqualTo(MascotExpression.Happy)
    }

    @Test
    fun `setExpression is stored and sent to the mascot`() {
        val inputs = FakeInputs()
        val viewModel = createViewModel(inputs)

        viewModel.setExpression(MascotExpression.Encouraging)

        assertThat(viewModel.uiState.value.expression).isEqualTo(MascotExpression.Encouraging)
        assertThat(inputs.numbers[MascotInputNames.EXPRESSION]).isEqualTo(3f)
    }
}
