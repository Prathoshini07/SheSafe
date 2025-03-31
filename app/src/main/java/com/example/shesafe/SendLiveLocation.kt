package com.example.shesafe

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.shesafe.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class SendLiveLocation : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val guardianPhoneNumber = "+918903787836"  // Change to your guardian’s number

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_live_location)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val sendLocationButton: Button = findViewById(R.id.imageButton2)

        sendLocationButton.setOnClickListener {
            requestPermissionsAndSendLocation()
        }
    }

    private fun requestPermissionsAndSendLocation() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), 1001)
        } else {
            getLastLocation()
        }
    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val latitude = it.latitude
                val longitude = it.longitude
                val locationUrl = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

                // Send SMS
                sendSMS(guardianPhoneNumber, "🚨 I'm in danger! My live location: $locationUrl")

                // Send FCM Notification
                sendFCMNotification(locationUrl)

                // Confirmation
                Toast.makeText(this, "Live Location Sent!", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission required", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(this, "SMS Sent!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SMS", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun sendFCMNotification(locationUrl: String) {
        val fcmToken = "GUARDIAN_FCM_TOKEN"  // Fetch from Firebase or secure storage
        val serverKey = "YOUR_FIREBASE_SERVER_KEY"

        val requestBody = """
            {
                "to": "$fcmToken",
                "notification": {
                    "title": "🚨 SOS Alert!",
                    "body": "Your contact needs help! Location: $locationUrl"
                }
            }
        """.trimIndent()

        val url = "https://fcm.googleapis.com/fcm/send"

        Thread {
            try {
                val urlObj = java.net.URL(url)
                val connection = urlObj.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "key=$serverKey")
                connection.doOutput = true

                val outputStream = connection.outputStream
                outputStream.write(requestBody.toByteArray())
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                println("FCM Response Code: $responseCode")

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val permissionsGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (permissionsGranted) {
                getLastLocation()
            } else {
                Toast.makeText(this, "Permissions required for sending location", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
