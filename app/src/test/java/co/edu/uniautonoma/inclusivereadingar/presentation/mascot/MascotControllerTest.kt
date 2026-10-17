package co.edu.uniautonoma.inclusivereadingar.presentation.mascot

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MascotControllerTest {
    private class FakeInputs : MascotInputs {
        val triggers = mutableListOf<String>()
        val numbers = mutableMapOf<String, Float>()
        override fun fireTrigger(name: String) { triggers += name }
        override fun setNumber(name: String, value: Float) { numbers[name] = value }
    }

    @Test
    fun `play fires the trigger of the event when attached`() {
        val inputs = FakeInputs()
        val controller = MascotController().apply { attach(inputs) }

        MascotEvent.entries.filter { it.trigger != null }.forEach(controller::play)

        assertThat(inputs.triggers).containsExactly("wave", "celebrate", "encourage", "tryAgain", "think").inOrder()
    }

    @Test
    fun `an event sent before attach is played once when the mascot appears`() {
        val inputs = FakeInputs()
        val controller = MascotController()

        controller.play(MascotEvent.Wave)
        controller.play(MascotEvent.Celebrate) // only the latest pending action is kept
        controller.attach(inputs)
        controller.detach()
        controller.attach(inputs) // re-attach (rotation) must not replay the action

        assertThat(inputs.triggers).containsExactly("celebrate")
    }

    @Test
    fun `attach replays the last expression and gaze`() {
        val inputs = FakeInputs()
        val controller = MascotController()
        controller.setExpression(MascotExpression.Curious)
        controller.lookAt(0.5f, -0.5f)

        controller.attach(inputs)

        assertThat(inputs.numbers[MascotInputNames.EXPRESSION]).isEqualTo(2f)
        assertThat(inputs.numbers[MascotInputNames.LOOK_X]).isEqualTo(0.5f * MascotInputNames.MAX_LOOK_OFFSET)
        assertThat(inputs.numbers[MascotInputNames.LOOK_Y]).isEqualTo(-0.5f * MascotInputNames.MAX_LOOK_OFFSET)
    }

    @Test
    fun `lookAt clamps out of range values so pupils stay inside the eye`() {
        val inputs = FakeInputs()
        val controller = MascotController().apply { attach(inputs) }

        controller.lookAt(5f, -9f)

        assertThat(inputs.numbers[MascotInputNames.LOOK_X]).isEqualTo(MascotInputNames.MAX_LOOK_OFFSET)
        assertThat(inputs.numbers[MascotInputNames.LOOK_Y]).isEqualTo(-MascotInputNames.MAX_LOOK_OFFSET)
    }

    @Test
    fun `idle returns to the calm default without firing a trigger`() {
        val inputs = FakeInputs()
        val controller = MascotController().apply { attach(inputs) }
        controller.setExpression(MascotExpression.Empathic)
        controller.lookAt(1f, 1f)

        controller.play(MascotEvent.Idle)

        assertThat(inputs.triggers).isEmpty()
        assertThat(inputs.numbers[MascotInputNames.EXPRESSION]).isEqualTo(MascotExpression.Happy.value.toFloat())
        assertThat(inputs.numbers[MascotInputNames.LOOK_X]).isEqualTo(0f)
        assertThat(inputs.numbers[MascotInputNames.LOOK_Y]).isEqualTo(0f)
    }

    @Test
    fun `nothing is sent after detach`() {
        val inputs = FakeInputs()
        val controller = MascotController().apply { attach(inputs) }
        controller.detach()
        inputs.numbers.clear()

        controller.setExpression(MascotExpression.Neutral)
        controller.lookAt(1f, 1f)

        assertThat(inputs.numbers).isEmpty()
    }
}
