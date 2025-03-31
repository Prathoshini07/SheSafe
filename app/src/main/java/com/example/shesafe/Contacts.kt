package com.example.shesafe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ContactsActivity : AppCompatActivity() {

    private lateinit var guardianName1EditText: EditText
    private lateinit var guardianPhone1EditText: EditText
    private lateinit var guardianName2EditText: EditText
    private lateinit var guardianPhone2EditText: EditText
    private lateinit var saveButton: Button
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        // Initialize Firebase Auth & Database
        auth = FirebaseAuth.getInstance()

        // 🔹 Check if user is logged in BEFORE accessing Firebase
        if (auth.currentUser == null) {
            showToast("Please log in first!")
            startActivity(Intent(this, MainActivity::class.java)) // Redirect to login
            finish() // Close ContactsActivity
            return
        }

        database = FirebaseDatabase.getInstance("https://she-safe-07-default-rtdb.firebaseio.com").reference.child("contacts")

        // Find views
        guardianName1EditText = findViewById(R.id.editTextGuardian1)
        guardianPhone1EditText = findViewById(R.id.editTextPhone1)
        guardianName2EditText = findViewById(R.id.editTextGuardian2)
        guardianPhone2EditText = findViewById(R.id.editTextPhone2)
        saveButton = findViewById(R.id.buttonSave)

        // Save button - Save contacts to Firebase
        saveButton.setOnClickListener {
            saveGuardianContacts()
        }
    }

    private fun saveGuardianContacts() {
        val guardianName1 = guardianName1EditText.text.toString().trim()
        val guardianPhone1 = guardianPhone1EditText.text.toString().trim()
        val guardianName2 = guardianName2EditText.text.toString().trim()
        val guardianPhone2 = guardianPhone2EditText.text.toString().trim()

        if (guardianName1.isEmpty() || guardianPhone1.isEmpty() || guardianName2.isEmpty() || guardianPhone2.isEmpty()) {
            showToast("Please enter all details")
            return
        }

        // Get the logged-in user ID & Email
        val userId = auth.currentUser?.uid
        val userEmail = auth.currentUser?.email

        if (userId != null && userEmail != null) {
            val contacts = hashMapOf(
                "userEmail" to userEmail,  // ✅ Store user email
                "guardian1" to hashMapOf(
                    "name" to guardianName1,
                    "phone" to guardianPhone1
                ),
                "guardian2" to hashMapOf(
                    "name" to guardianName2,
                    "phone" to guardianPhone2
                )
            )

            // Save contacts under the user's ID
            database.child(userId).setValue(contacts)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        showToast("Contacts Saved Successfully")

                        // ✅ Redirect to HomePage after saving contacts
                        val intent = Intent(this, HomePage::class.java)
                        startActivity(intent)
                        finish() // Close ContactsActivity to prevent going back
                    } else {
                        showToast("Error: ${task.exception?.message}")
                    }
                }
        } else {
            showToast("User not logged in!")
        }
    }



    // ✅ Show Toast Messages
    private fun showToast(message: String)
    {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }
}
