package com.framewise.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.framewise.domain.model.HorizonState
import com.framewise.domain.repository.SensorRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Horizon roll angle from `TYPE_ROTATION_VECTOR` (a fused sensor — no need
 * to hand-roll a complementary filter over raw accelerometer/gyroscope).
 * Listener is only active while there's at least one collector, via
 * `SharingStarted.WhileSubscribed`, so it doesn't drain battery with the
 * camera screen backgrounded.
 *
 * NOTE: the sign of "tilted left" vs "tilted right" depends on how a real
 * device reports roll for `TYPE_ROTATION_VECTOR` held in portrait; this
 * couldn't be verified against real hardware in the environment this was
 * written in. If the on-screen horizon indicator tilts the wrong way on a
 * real device, flip the sign at [rollDegrees].
 */
@Singleton
class HorizonSensorController @Inject constructor(
    @ApplicationContext context: Context,
) : SensorRepository {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val horizonState: StateFlow<HorizonState> = callbackFlow {
        if (rotationVectorSensor == null) {
            trySend(HorizonState.fromAngle(0f))
            awaitClose { }
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientation = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                trySend(HorizonState.fromAngle(rollDegrees(orientation[2])))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
        .map { smoothed(it) }
        .stateIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), HorizonState())

    private var lastAngle = 0f

    /** Light exponential smoothing so the indicator doesn't jitter frame to frame. */
    private fun smoothed(raw: HorizonState): HorizonState {
        val smoothedAngle = lastAngle + SMOOTHING_FACTOR * (raw.angleDegrees - lastAngle)
        lastAngle = smoothedAngle
        return HorizonState.fromAngle((smoothedAngle * 10f).roundToInt() / 10f)
    }

    private fun rollDegrees(rollRadians: Float): Float = Math.toDegrees(rollRadians.toDouble()).toFloat()

    private companion object {
        const val SMOOTHING_FACTOR = 0.3f
    }
}
