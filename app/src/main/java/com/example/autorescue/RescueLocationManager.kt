package com.example.autorescue

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class RescueLocationManager(
    context: Context
) {

    private val appContext =
        context.applicationContext

    private val client =
        LocationServices
            .getFusedLocationProviderClient(
                appContext
            )

    fun getCurrentLocation(
        onLocation:
            (Double?, Double?, Float) -> Unit
    ) {

        val fineGranted =
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted =
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {

            onLocation(
                null,
                null,
                0f
            )

            return
        }

        val priority =
            if (fineGranted) {

                Priority.PRIORITY_HIGH_ACCURACY

            } else {

                Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }

        client.getCurrentLocation(
            priority,
            null
        )
            .addOnSuccessListener { location ->

                if (location != null) {

                    onLocation(
                        location.latitude,
                        location.longitude,
                        location.speed
                    )

                } else {

                    onLocation(
                        null,
                        null,
                        0f
                    )
                }
            }
            .addOnFailureListener {

                onLocation(
                    null,
                    null,
                    0f
                )
            }
    }
}