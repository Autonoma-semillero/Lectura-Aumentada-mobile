package co.edu.uniautonoma.inclusivereadingar.presentation.mascot

/**
 * One-shot actions the mascot can perform. Each maps to a trigger of the `MascotVM`
 * view model inside `res/raw/mascota.riv` (see docs/mascota-rive.md).
 *
 * [Idle] has no trigger: the base idle loop always runs, so it only means
 * "return to the calm default" (happy face, eyes centered).
 */
enum class MascotEvent(val trigger: String?) {
    Idle(null),
    Wave("wave"),
    Celebrate("celebrate"),
    Encourage("encourage"),
    TryAgain("tryAgain"),
    Think("think"),
}

/** Persistent facial expression; the value is the `expression` number input of the .riv. */
enum class MascotExpression(val value: Int) {
    Neutral(0),
    Happy(1),
    Curious(2),
    Encouraging(3),
    Empathic(4),
}

/** Input names exposed by the .riv view model. Keep in sync with the Rive file. */
object MascotInputNames {
    const val EXPRESSION = "expression"
    const val LOOK_X = "lookX"
    const val LOOK_Y = "lookY"

    /** Max gaze offset in artboard pixels; beyond this the pupils would leave the eye. */
    const val MAX_LOOK_OFFSET = 14f
}
