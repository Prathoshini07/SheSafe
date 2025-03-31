package com.example.shesafe

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import android.telephony.SmsManager
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase

class HomePage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        // Initialize Firebase Auth and FusedLocationProviderClient
        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val contacts = findViewById<Button>(R.id.buttonContacts)
        val sos = findViewById<Button>(R.id.buttonSOS)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val liveloc = findViewById<Button>(R.id.buttonShareLocation)
        val profile=findViewById<Button>(R.id.buttonProfile)

        // Set up click listeners
        contacts.setOnClickListener {
            val intent = Intent(this, ContactsActivity::class.java)
            startActivity(intent)
            finish()
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Notification Trigger on SOS Button Click
        sos.setOnClickListener {
            val intent = Intent(this, SOS::class.java)
            startActivity(intent)
            finish()
        }

        liveloc.setOnClickListener {
            getCurrentLocation()
        }
        profile.setOnClickListener()
        {
            val intent=Intent(this,Profile::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 2)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    val message = "Emergency! I need help. My live location: http://maps.google.com/maps?q=$lat,$lon"
                    sendSMS(message)
                } else {
                    Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to get location: $exception", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendSMS(message: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        // Check for SMS Permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 1)
            return
        }

        // Fetch guardian contacts from Firebase
        val database = FirebaseDatabase.getInstance("https://she-safe-07-default-rtdb.firebaseio.com").reference.child("contacts")

        database.child(userId).get()
            .addOnSuccessListener { snapshot ->
                val guardian1Phone = snapshot.child("guardian1").child("phone").value as? String
                val guardian2Phone = snapshot.child("guardian2").child("phone").value as? String

                val smsManager = SmsManager.getDefault()
                if (!guardian1Phone.isNullOrEmpty()) {
                    smsManager.sendTextMessage(guardian1Phone, null, message, null, null)
                }
                if (!guardian2Phone.isNullOrEmpty()) {
                    smsManager.sendTextMessage(guardian2Phone, null, message, null, null)
                }

                Toast.makeText(this, "Emergency SMS sent to guardians!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch guardian contacts", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now send SMS
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == 2) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now get location
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}