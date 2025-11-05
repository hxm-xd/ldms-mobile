package com.example.mad_cw.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.mad_cw.R
import com.example.mad_cw.ui.dashboard.DashboardActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class LDMSNotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if message contains data payload
        remoteMessage.data.isNotEmpty().let {
            val title = remoteMessage.data["title"] ?: "LDMS Alert"
            val body = remoteMessage.data["body"] ?: "High-risk sensor detected"
            val sensorName = remoteMessage.data["sensorName"] ?: ""

            sendNotification(title, body, sensorName)
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            sendNotification(
                it.title ?: "LDMS Alert",
                it.body ?: "High-risk sensor detected",
                ""
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to server or save locally
    }

    private fun sendNotification(title: String, messageBody: String, sensorName: String) {
        val intent = Intent(this, DashboardActivity::class.java)
        if (sensorName.isNotEmpty()) {
            intent.putExtra("sensorName", sensorName)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = "ldms_alerts"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_landslide)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "LDMS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Landslide Detection and Monitoring System alerts"
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
