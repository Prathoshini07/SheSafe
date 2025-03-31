package com.example.shesafe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Profile : AppCompatActivity() {

    private lateinit var editFullName: EditText
    private lateinit var editOldPassword: EditText
    private lateinit var editNewPassword: EditText
    private lateinit var buttonSaveProfile: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase Authentication & Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://she-safe-07-default-rtdb.firebaseio.com")
            .reference.child("users")

        // Find views
        editFullName = findViewById(R.id.editFullName)
        editOldPassword = findViewById(R.id.editOldPassword)
        editNewPassword = findViewById(R.id.editNewPassword)
        buttonSaveProfile = findViewById(R.id.buttonSaveProfile)

        // Load existing user data
        loadUserProfile()

        // Save changes button click listener
        buttonSaveProfile.setOnClickListener {
            val newName = editFullName.text.toString().trim()
            val oldPassword = editOldPassword.text.toString().trim()
            val newPassword = editNewPassword.text.toString().trim()

            var isUpdating = false

            if (newName.isNotEmpty()) {
                isUpdating = true
                updateFullName(newName)
            }
            if (oldPassword.isNotEmpty() && newPassword.isNotEmpty()) {
                isUpdating = true
                updatePassword(oldPassword, newPassword)
            }

            if (isUpdating) {
                navigateToHome()
            } else {
                showToast("Please enter details to update")
            }
        }
    }

    // Load user profile from Firebase
    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.child(userId).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val name = snapshot.child("fullName").value.toString()
                    editFullName.setText(name)
                }
            }.addOnFailureListener {
                showToast("Failed to load profile")
            }
        }
    }

    // Update full name in Firebase Database
    private fun updateFullName(newName: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.child(userId).child("fullName").setValue(newName)
                .addOnSuccessListener { showToast("Name updated successfully") }
                .addOnFailureListener { showToast("Failed to update name") }
        }
    }

    // Update password with authentication
    private fun updatePassword(oldPassword: String, newPassword: String) {
        val user = auth.currentUser
        if (user != null && user.email != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)

            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(newPassword)
                        .addOnSuccessListener {
                            showToast("Password updated successfully")
                            navigateToHome() // Navigate to home after successful update
                        }
                        .addOnFailureListener { showToast("Failed to update password: ${it.message}") }
                }
                .addOnFailureListener {
                    showToast("Old password is incorrect")
                }
        }
    }

    // Navigate to Home Activity
    private fun navigateToHome() {
        val intent = Intent(this, HomePage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish() // Finish profile activity to prevent going back
    }

    // Show Toast message
    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }
}
