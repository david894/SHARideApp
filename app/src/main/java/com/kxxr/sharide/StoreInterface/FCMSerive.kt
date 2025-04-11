package com.kxxr.sharide.StoreInterface

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kxxr.sharide.R

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
        // You can send this token to your backend if needed
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Message received")

        // Log notification payload (may be null in foreground)
        remoteMessage.notification?.let {
            Log.d("FCM", "Notification payload: ${it.title} - ${it.body}")
            showNotification(it.title ?: "SHARide", it.body ?: "")
        }

        // Log data payload
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"]
            val message = remoteMessage.data["message"]
            Log.d("FCM", "Data payload: $title - $message")
            showNotification(title ?: "SHARide", message ?: "")
        }
    }


    private fun showNotification(title: String, message: String) {
        val channelId = "sharide_notifications"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "SHARide Notifications", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.test_logo) // Use your own icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        notificationManager.notify(0, notificationBuilder.build())
        Log.d("FCM", "Showing notification: $title - $message")

    }
}
