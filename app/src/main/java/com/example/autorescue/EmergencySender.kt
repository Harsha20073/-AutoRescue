package com.example.autorescue

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

object EmergencySender {

    fun sendSms(
        context: Context,
        phoneNumber: String,
        assessment: IncidentAssessment,
        latitude: Double?,
        longitude: Double?
    ): Result<Unit> {

        val permission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            )

        if (
            permission !=
            PackageManager.PERMISSION_GRANTED
        ) {

            return Result.failure(
                SecurityException(
                    "SEND_SMS permission is not granted."
                )
            )
        }

        if (phoneNumber.isBlank()) {

            return Result.failure(
                IllegalArgumentException(
                    "Phone number is empty."
                )
            )
        }

        val locationText =
            if (
                latitude != null &&
                longitude != null
            ) {

                "Location: $latitude, $longitude"

            } else {

                "Location: unavailable"
            }

        val message =
            buildString {

                append(
                    "AutoRescue emergency alert. "
                )

                append(
                    "Crash suspicion: "
                )

                append(
                    assessment.priority
                )

                append(". ")

                append(
                    "Risk score: "
                )

                append(
                    assessment.score
                )

                append("/100. ")

                append(
                    "Driver response: "
                )

                append(
                    assessment.driverResponse
                )

                append(". ")

                append(
                    locationText
                )
            }

        return runCatching {

            SmsManager
                .getDefault()
                .sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    null,
                    null
                )
        }
    }
}