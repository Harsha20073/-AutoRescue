package com.example.autorescue

object RiskEngine {

    fun assess(
        snapshot: SensorSnapshot,
        driverResponse: String
    ): IncidentAssessment {

        val impactScore =
            when {

                snapshot.accelerationMagnitude >= 30f ->
                    50

                snapshot.accelerationMagnitude >= 22f ->
                    40

                snapshot.accelerationMagnitude >= 15f ->
                    30

                snapshot.accelerationMagnitude >= 10f ->
                    15

                else ->
                    0
            }

        val rotationScore =
            when {

                snapshot.rotationMagnitude >= 7f ->
                    20

                snapshot.rotationMagnitude >= 5f ->
                    15

                snapshot.rotationMagnitude >= 3.5f ->
                    10

                else ->
                    0
            }

        val stopped =
            snapshot.speedMps <= 1.0f

        val stopScore =
            if (stopped) 15 else 0

        val gpsScore =
            if (snapshot.gpsAvailable) 5 else 0

        val responseScore =
            when (driverResponse) {

                "NO RESPONSE" ->
                    10

                "EMERGENCY CONTINUED" ->
                    8

                else ->
                    0
            }

        val score =
            (
                    impactScore +
                            rotationScore +
                            stopScore +
                            gpsScore +
                            responseScore
                    ).coerceIn(0, 100)

        val priority =
            when {

                score >= 70 ->
                    "HIGH"

                score >= 45 ->
                    "MEDIUM"

                else ->
                    "LOW"
            }

        return IncidentAssessment(

            priority = priority,

            score = score,

            impactScore = impactScore,

            rotationScore = rotationScore,

            stopScore = stopScore,

            gpsScore = gpsScore,

            responseScore = responseScore,

            impactMagnitude =
                snapshot.accelerationMagnitude,

            rotationMagnitude =
                snapshot.rotationMagnitude,

            stopped = stopped,

            driverResponse =
                driverResponse,

            gpsAvailable =
                snapshot.gpsAvailable
        )
    }
}