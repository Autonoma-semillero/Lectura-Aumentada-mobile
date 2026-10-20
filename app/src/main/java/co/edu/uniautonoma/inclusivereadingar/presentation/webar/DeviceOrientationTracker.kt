package co.edu.uniautonoma.inclusivereadingar.presentation.webar

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Feeds the phone's physical orientation into [onOrientation] so AR content can stay anchored in
 * the room instead of being glued to the screen. Reads the gyroscope-fused rotation sensor
 * natively because `android.webkit.WebView` does not dispatch the web `deviceorientation`/
 * `deviceorientationabsolute` events at all (confirmed live: zero events received over several
 * seconds via Chrome DevTools Protocol against the real WebView, despite the Permissions Policy
 * and the `accelerometer` permission both reporting "granted") — a known WebView gap, not
 * something a web-side fix can work around.
 */
class DeviceOrientationTracker(private val sensorManager: SensorManager) {
    private var listener: SensorEventListener? = null
    private val calibrated = CalibratedOrientation()

    /** Returns false if the device exposes neither a game-rotation nor a rotation-vector sensor. */
    fun start(onOrientation: (Quat) -> Unit): Boolean {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: return false

        calibrated.reset()
        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val quaternion = FloatArray(4)
                SensorManager.getQuaternionFromVector(quaternion, event.values)
                val reading = Quat(quaternion[0], quaternion[1], quaternion[2], quaternion[3])
                onOrientation(calibrated.apply(reading, System.currentTimeMillis()))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = sensorListener
        sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_GAME)
        return true
    }

    fun stop() {
        listener?.let(sensorManager::unregisterListener)
        listener = null
    }
}
