package com.example.shesafe

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.shesafe.R
import com.example.shesafe.HomePage
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Message Received: ${remoteMessage.notification?.body}")

        remoteMessage.notification?.let {
            sendNotification(it.title ?: "SOS Alert", it.body ?: "Location Sent")
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val channelId = "SOS_Alerts"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Emergency Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // ✅ Open HomePage when notification is clicked
        val intent = Intent(this, HomePage::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Ensure high priority
            .setContentIntent(pendingIntent) // ✅ Set pending intent

        notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Token: $token")
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        val serverUrl = "https://yourserver.com/api/saveToken"

        val requestBody = JSONObject()
        requestBody.put("token", token)

        val request = JsonObjectRequest(
            Request.Method.POST, serverUrl, requestBody,
            { response -> Log.d("FCM", "Token saved successfully!") },
            { error -> Log.e("FCM", "Error saving token: ${error.message}") }
        )

        Volley.newRequestQueue(this).add(request)
    }
}
