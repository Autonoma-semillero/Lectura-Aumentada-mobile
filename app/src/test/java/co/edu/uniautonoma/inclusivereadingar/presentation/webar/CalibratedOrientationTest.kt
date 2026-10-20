package co.edu.uniautonoma.inclusivereadingar.presentation.webar

import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import org.junit.Test

private const val EPSILON = 1e-5f

private fun assertNearIdentityDirection(q: Quat) {
    assertThat(q.x).isWithin(EPSILON).of(0f)
    assertThat(q.y).isWithin(EPSILON).of(0f)
    assertThat(q.z).isWithin(EPSILON).of(0f)
}

class CalibratedOrientationTest {
    @Test
    fun aSingleReadingOutsideWarmupCalibratesToIdentitySoContentInFrontOfTheCameraStaysInView() {
        val calibrated = CalibratedOrientation(warmupMs = 0)
        val first = Quat(w = 0.7f, x = 0.1f, y = 0.2f, z = 0.3f)

        val result = calibrated.apply(first, nowMs = 1000L)

        assertNearIdentityDirection(result)
    }

    @Test
    fun laterReadingsAreRelativeToTheCalibratedStartingOrientation() {
        val calibrated = CalibratedOrientation(warmupMs = 0)
        val start = Quat(w = 0.9f, x = 0.1f, y = 0f, z = 0f)
        val turned = Quat(w = 0.8f, x = 0.2f, y = 0.1f, z = 0f)

        val atStart = calibrated.apply(start, nowMs = 1000L)
        val afterTurn = calibrated.apply(turned, nowMs = 1050L)

        assertNearIdentityDirection(atStart)
        assertThat(afterTurn).isNotEqualTo(atStart)
    }

    @Test
    fun readingsDuringTheWarmupWindowKeepRecalibratingInsteadOfLockingTheFirstTransientPose() {
        val calibrated = CalibratedOrientation(warmupMs = 800)
        // Simulates a hand still moving the phone right after tapping "allow camera".
        val stillMoving = Quat(w = 0.9f, x = 0.3f, y = 0.1f, z = 0f)
        val settled = Quat(w = 0.6f, x = -0.2f, y = 0.5f, z = 0.1f)

        val duringWarmup = calibrated.apply(stillMoving, nowMs = 1000L)
        val stillWithinWarmup = calibrated.apply(settled, nowMs = 1700L)

        // Every reading inside the warmup window reports as "zero" because the reference keeps
        // re-locking to whatever just arrived.
        assertNearIdentityDirection(duringWarmup)
        assertNearIdentityDirection(stillWithinWarmup)
    }

    @Test
    fun theReferenceLocksToWhereverThePhoneSettledOnceTheWarmupWindowElapses() {
        val calibrated = CalibratedOrientation(warmupMs = 800)
        val stillMoving = Quat(w = 0.9f, x = 0.3f, y = 0.1f, z = 0f)
        val settled = Quat(w = 0.6f, x = -0.2f, y = 0.5f, z = 0.1f)
        val turnedAfterSettling = Quat(w = 0.5f, x = -0.1f, y = 0.6f, z = 0.2f)

        calibrated.apply(stillMoving, nowMs = 1000L)
        val atEndOfWarmup = calibrated.apply(settled, nowMs = 1799L)
        val afterWarmup = calibrated.apply(settled, nowMs = 1800L)
        val afterTurning = calibrated.apply(turnedAfterSettling, nowMs = 1900L)

        assertNearIdentityDirection(atEndOfWarmup)
        // The reference is now locked to `settled`, so reporting the exact same pose again still
        // reads as identity...
        assertNearIdentityDirection(afterWarmup)
        // ...but a genuinely different pose after that no longer gets re-absorbed into "zero".
        val driftedFromIdentity = abs(afterTurning.x) > EPSILON || abs(afterTurning.y) > EPSILON || abs(afterTurning.z) > EPSILON
        assertThat(driftedFromIdentity).isTrue()
    }

    @Test
    fun resetForgetsTheCalibratedReferenceSoTheNextReadingBecomesTheNewZero() {
        val calibrated = CalibratedOrientation(warmupMs = 0)
        calibrated.apply(Quat(w = 0.7f, x = 0.1f, y = 0.2f, z = 0.3f), nowMs = 1000L)

        calibrated.reset()
        val result = calibrated.apply(Quat(w = 0.5f, x = 0.4f, y = -0.3f, z = 0.2f), nowMs = 5000L)

        assertNearIdentityDirection(result)
    }

    @Test
    fun conjugateNegatesTheVectorPartAndKeepsTheScalarPart() {
        val q = Quat(w = 0.5f, x = 0.1f, y = -0.2f, z = 0.3f)

        val conjugate = q.conjugate()

        assertThat(conjugate).isEqualTo(Quat(w = 0.5f, x = -0.1f, y = 0.2f, z = -0.3f))
    }
}
