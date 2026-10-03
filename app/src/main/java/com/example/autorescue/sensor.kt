package com.example.autorescue.sensor

data class SensorInput(
    val accelerationX: Float = 0f,
    val accelerationY: Float = 0f,
    val accelerationZ: Float = 0f,
    val accelerationMagnitude: Float = 0f,
    val rotationX: Float = 0f,
    val rotationY: Float = 0f,
    val rotationZ: Float = 0f,
    val timestampMillis: Long = System.currentTimeMillis()
)
{

    /**
     * Returns true when the measured acceleration
     * reaches the supplied impact threshold.
     *
     * This does not by itself confirm an accident.
     * The RiskEngine will combine this with vehicle
     * and driver information.
     */
    fun indicatesHighImpact(
        threshold: Float = 15f
    ): Boolean {
        return accelerationMagnitude >= threshold
    }

    /**
     * Returns a readable description of the sensor state.
     */
    fun summary(): String {
        return buildString {
            append("Acceleration: ")
            append(String.format("%.2f", accelerationMagnitude))
            append(" m/s² | ")

            append("Gyroscope: ")
            append("X=")
            append(String.format("%.2f", rotationX))
            append(", Y=")
            append(String.format("%.2f", rotationY))
            append(", Z=")
            append(String.format("%.2f", rotationZ))
        }
    }
}