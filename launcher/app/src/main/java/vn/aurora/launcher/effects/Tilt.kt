package vn.aurora.launcher.effects

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * How far the phone is tilted away from the way the user is holding it, each axis in -1..1.
 *
 * The neutral pose slowly follows the current pose, so the parallax always recentres
 * whether the phone is held upright or lying on a table. Only listens while resumed.
 * Read it inside draw / graphicsLayer lambdas so sensor updates don't recompose.
 * When [enabled] is false the sensor is not used and the tilt stays at zero.
 */
@Composable
fun rememberTilt(enabled: Boolean = true): State<Offset> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tilt = remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(context, lifecycleOwner, enabled) {
        if (!enabled) {
            tilt.value = Offset.Zero
            return@DisposableEffect onDispose { }
        }
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var neutral: Offset? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val raw = Offset(
                    (-event.values[0] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f),
                    (event.values[1] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f),
                )
                val base = neutral ?: raw
                val newNeutral = base + (raw - base) * 0.01f
                neutral = newNeutral
                val target = (raw - newNeutral) * 3f
                val clamped = Offset(target.x.coerceIn(-1f, 1f), target.y.coerceIn(-1f, 1f))
                // Low-pass filter to remove hand jitter.
                tilt.value = tilt.value + (clamped - tilt.value) * 0.15f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (sensor != null) {
                    neutral = null
                    sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
                }
                Lifecycle.Event.ON_PAUSE -> sensorManager?.unregisterListener(listener)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager?.unregisterListener(listener)
        }
    }
    return tilt
}
