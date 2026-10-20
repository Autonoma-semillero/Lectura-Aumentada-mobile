package co.edu.uniautonoma.inclusivereadingar.presentation.webar

/**
 * Quaternion as (w, x, y, z) — matching the output order of
 * [android.hardware.SensorManager.getQuaternionFromVector], which Android's own
 * documentation and header comments list w-first (unlike three.js's x,y,z,w).
 */
data class Quat(val w: Float, val x: Float, val y: Float, val z: Float) {
    fun conjugate() = Quat(w, -x, -y, -z)

    operator fun times(o: Quat) = Quat(
        w = w * o.w - x * o.x - y * o.y - z * o.z,
        x = w * o.x + x * o.w + y * o.z - z * o.y,
        y = w * o.y - x * o.z + y * o.w + z * o.x,
        z = w * o.z + x * o.y - y * o.x + z * o.w,
    )
}

/**
 * Captures the device's orientation shortly after tracking starts as "the camera is looking
 * straight at the content that was just placed in front of it" and reports every reading after
 * that relative to that reference, so a card anchored at mount time doesn't jump out of view just
 * because the phone happened to be physically pointed elsewhere when tracking started.
 *
 * The reference isn't locked to the very first reading: whoever just tapped the camera permission
 * prompt may still be moving the phone into position, so every reading within [warmupMs] of the
 * first one keeps re-locking the reference, and the effective "zero" ends up being wherever the
 * phone actually settles.
 */
class CalibratedOrientation(private val warmupMs: Long = 800L) {
    private var referenceInverse: Quat? = null
    private var startedAtMs: Long? = null

    fun apply(current: Quat, nowMs: Long): Quat {
        val start = startedAtMs ?: nowMs.also { startedAtMs = it }
        if (nowMs - start < warmupMs) {
            referenceInverse = current.conjugate()
        }
        val reference = referenceInverse ?: current.conjugate().also { referenceInverse = it }
        return reference * current
    }

    fun reset() {
        referenceInverse = null
        startedAtMs = null
    }
}
