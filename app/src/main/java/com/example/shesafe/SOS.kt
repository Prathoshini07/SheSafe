package com.example.shesafe

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telephony.TelephonyManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SOS : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var sosButton: ImageButton
    private lateinit var phoneStateReceiver: PhoneStateReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://she-safe-07-default-rtdb.firebaseio.com").reference.child("contacts")
        sosButton = findViewById(R.id.imageButtonSOS)
        val homepage=findViewById<Button>(R.id.buttonHome)

        // Request READ_PHONE_STATE permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 2)
        } else {
            // Register BroadcastReceiver if permission is already granted
            phoneStateReceiver = PhoneStateReceiver()
            val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            registerReceiver(phoneStateReceiver, filter)
        }

        // Request CALL_PHONE permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1)
        }

        // Set click listener for SOS button
        sosButton.setOnClickListener {
            callGuardian()
        }
        homepage.setOnClickListener()
        {
            val intent=Intent(this,HomePage::class.java)
            startActivity(intent)
        }
    }

    private fun callGuardian() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showToast("User not logged in!")
            return
        }

        // Fetch guardian's phone number from Firebase
        database.child(userId).child("guardian1").child("phone").get()
            .addOnSuccessListener { snapshot ->
                val guardianPhone = snapshot.value as? String
                if (!guardianPhone.isNullOrEmpty()) {
                    makePhoneCall(guardianPhone)
                } else {
                    showToast("No guardian phone number found!")
                }
            }
            .addOnFailureListener {
                showToast("Failed to get guardian contact.")
            }
    }

    private fun makePhoneCall(phoneNumber: String) {
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:$phoneNumber")

        // Check if CALL_PHONE permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(callIntent)
        } else {
            // Request CALL_PHONE permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1)
        }
    }

    // Handle permission request result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // CALL_PHONE permission granted
        } else if (requestCode == 1) {
            showToast("Call permission denied!")
        } else if (requestCode == 2 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // READ_PHONE_STATE permission granted, register BroadcastReceiver
            phoneStateReceiver = PhoneStateReceiver()
            val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            registerReceiver(phoneStateReceiver, filter)
        } else if (requestCode == 2) {
            showToast("Phone state permission denied!")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // Unregister BroadcastReceiver when activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(phoneStateReceiver)
        } catch (e: Exception) {
            // Ignore if receiver is not registered
        }
    }

    // Inner class for BroadcastReceiver
    inner class PhoneStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                    // Call the function to return to home screen here
                    returnToHomeScreen()
                }
            }
        }
    }

    // Function to navigate to HomepageActivity
    private fun returnToHomeScreen() {
        val intent = Intent(this, HomePage::class.java)
        startActivity(intent)
        finish() // Optional: Close the current activity if you don't want it in the back stack
    }
}