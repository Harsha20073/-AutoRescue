package com.example.autorescue

data class SensorSnapshot(
    val accelerationMagnitude: Float = 0f,
    val rotationMagnitude: Float = 0f,

    val accelerationX: Float = 0f,
    val accelerationY: Float = 0f,
    val accelerationZ: Float = 0f,

    val gpsAvailable: Boolean = false,

    val latitude: Double? = null,
    val longitude: Double? = null,

    val speedMps: Float = 0f
)

data class IncidentAssessment(
    val priority: String,

    val score: Int,

    val impactScore: Int,

    val rotationScore: Int,

    val stopScore: Int,

    val gpsScore: Int,

    val responseScore: Int,

    val impactMagnitude: Float,

    val rotationMagnitude: Float,

    val stopped: Boolean,

    val driverResponse: String,

    val gpsAvailable: Boolean
)

data class IncidentHistoryEntry(
    val timestamp: Long,

    val priority: String,

    val score: Int,

    val impactMagnitude: Float,

    val rotationMagnitude: Float,

    val driverResponse: String,

    val gpsAvailable: Boolean,

    val latitude: Double?,

    val longitude: Double?
)