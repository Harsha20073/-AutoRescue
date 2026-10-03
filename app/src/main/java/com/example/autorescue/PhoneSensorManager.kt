package com.example.autorescue

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class PhoneSensorManager(
    context: Context,
    private val onSnapshot: (SensorSnapshot) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(
            Context.SENSOR_SERVICE
        ) as SensorManager

    private val accelerometer =
        sensorManager.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER
        )

    private val gyroscope =
        sensorManager.getDefaultSensor(
            Sensor.TYPE_GYROSCOPE
        )

    private var ax = 0f
    private var ay = 0f
    private var az = 0f

    private var rx = 0f
    private var ry = 0f
    private var rz = 0f

    fun start() {

        accelerometer?.let { sensor ->

            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        gyroscope?.let { sensor ->

            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    fun stop() {

        sensorManager.unregisterListener(this)
    }

    fun hasAccelerometer(): Boolean {
        return accelerometer != null
    }

    fun hasGyroscope(): Boolean {
        return gyroscope != null
    }

    override fun onSensorChanged(
        event: SensorEvent
    ) {

        when (event.sensor.type) {

            Sensor.TYPE_ACCELEROMETER -> {

                ax = event.values[0]
                ay = event.values[1]
                az = event.values[2]
            }

            Sensor.TYPE_GYROSCOPE -> {

                rx = event.values[0]
                ry = event.values[1]
                rz = event.values[2]
            }
        }

        val accelerationMagnitude =
            sqrt(
                ax * ax +
                        ay * ay +
                        az * az
            )

        val rotationMagnitude =
            sqrt(
                rx * rx +
                        ry * ry +
                        rz * rz
            )

        onSnapshot(
            SensorSnapshot(

                accelerationMagnitude =
                    accelerationMagnitude,

                rotationMagnitude =
                    rotationMagnitude,

                accelerationX = ax,

                accelerationY = ay,

                accelerationZ = az
            )
        )
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
        // No action required.
    }
}