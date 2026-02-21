package com.example.whereuat.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

class MotionMonitor(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun movingFlow(): Flow<Boolean> = callbackFlow {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val magnitude = abs(event.values[0]) + abs(event.values[1]) + abs(event.values[2])
                trySend(magnitude > 20f)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (accel != null) {
            sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_NORMAL)
        }

        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
