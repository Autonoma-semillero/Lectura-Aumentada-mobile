package co.edu.uniautonoma.inclusivereadingar.presentation.mascot

/** Minimal write surface of the Rive view model; lets the controller be tested without Rive. */
interface MascotInputs {
    fun fireTrigger(name: String)
    fun setNumber(name: String, value: Float)
}

/**
 * UI-agnostic mascot API: `play(event)` and `setExpression(expression)`.
 *
 * Commands issued before the Rive file is ready (or while the composable is gone, e.g. during
 * rotation) are not lost: the last expression/gaze are replayed on [attach] and one pending
 * action is played once the mascot is visible again.
 */
class MascotController {
    private var inputs: MascotInputs? = null
    private var expression: MascotExpression = MascotExpression.Happy
    private var lookX = 0f
    private var lookY = 0f
    private var pendingEvent: MascotEvent? = null

    @Synchronized
    fun attach(newInputs: MascotInputs) {
        inputs = newInputs
        newInputs.setNumber(MascotInputNames.EXPRESSION, expression.value.toFloat())
        newInputs.setNumber(MascotInputNames.LOOK_X, lookX)
        newInputs.setNumber(MascotInputNames.LOOK_Y, lookY)
        pendingEvent?.let { fire(newInputs, it) }
        pendingEvent = null
    }

    @Synchronized
    fun detach() {
        inputs = null
    }

    @Synchronized
    fun play(event: MascotEvent) {
        if (event == MascotEvent.Idle) {
            lookAt(0f, 0f)
            setExpression(MascotExpression.Happy)
            return
        }
        val current = inputs
        if (current == null) pendingEvent = event else fire(current, event)
    }

    @Synchronized
    fun setExpression(newExpression: MascotExpression) {
        expression = newExpression
        inputs?.setNumber(MascotInputNames.EXPRESSION, newExpression.value.toFloat())
    }

    /** Moves both pupils; [x] and [y] are in -1..1 (right/down positive). Out-of-range values are clamped. */
    @Synchronized
    fun lookAt(x: Float, y: Float) {
        lookX = x.coerceIn(-1f, 1f) * MascotInputNames.MAX_LOOK_OFFSET
        lookY = y.coerceIn(-1f, 1f) * MascotInputNames.MAX_LOOK_OFFSET
        inputs?.let {
            it.setNumber(MascotInputNames.LOOK_X, lookX)
            it.setNumber(MascotInputNames.LOOK_Y, lookY)
        }
    }

    private fun fire(target: MascotInputs, event: MascotEvent) {
        event.trigger?.let(target::fireTrigger)
    }
}
