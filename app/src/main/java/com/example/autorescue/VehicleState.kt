package com.example.autorescue.vehicle

data class VehicleState(
    val speedKmh: Float = 0f,
    val engineRunning: Boolean = false,
    val seatbeltFastened: Boolean = true,
    val vehicleStopped: Boolean = true,
    val airbagDeployed: Boolean = false
) {

    companion object {
        val DEFAULT = VehicleState()
    }
}