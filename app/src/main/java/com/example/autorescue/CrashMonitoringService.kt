package com.example.autorescue

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class CrashMonitoringService : Service() {

    private lateinit var sensorManager:
            PhoneSensorManager

    override fun onCreate() {

        super.onCreate()

        createNotificationChannel()

        sensorManager =
            PhoneSensorManager(this) {
                // Background sensor collection.
                //
                // The foreground UI remains the
                // main demonstration layer.
            }

        startForeground(
            NOTIFICATION_ID,
            buildNotification(
                "AutoRescue is monitoring phone motion"
            )
        )

        sensorManager.start()
    }

    override fun onDestroy() {

        sensorManager.stop()

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }

    private fun createNotificationChannel() {

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "AutoRescue monitoring",
                NotificationManager.IMPORTANCE_LOW
            )

        getSystemService(
            NotificationManager::class.java
        )
            .createNotificationChannel(
                channel
            )
    }

    private fun buildNotification(
        text: String
    ): Notification {

        return NotificationCompat
            .Builder(
                this,
                CHANNEL_ID
            )
            .setContentTitle(
                "AutoRescue"
            )
            .setContentText(
                text
            )
            .setSmallIcon(
                android.R.drawable
                    .ic_dialog_alert
            )
            .setOngoing(true)
            .build()
    }

    companion object {

        private const val CHANNEL_ID =
            "autorescue_monitoring"

        private const val NOTIFICATION_ID =
            1001
    }
}