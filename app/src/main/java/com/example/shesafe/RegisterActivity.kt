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

class RegisterActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var loginButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase Authentication & Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://she-safe-07-default-rtdb.firebaseio.com").reference.child("users")

        // Find views
        nameEditText = findViewById(R.id.editTextText)
        emailEditText = findViewById(R.id.editTextTextEmailAddress2)
        registerButton = findViewById(R.id.button3)
        loginButton = findViewById(R.id.button7)

        // "Register" Button - Save User Details to Firebase
        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim().lowercase()

            if (name.isNotEmpty() && email.isNotEmpty()) {
                registerUser(name, email)
            } else {
                showToast("Please enter all details")
            }
        }

        // "Login" Button - Redirect to Main Activity
        loginButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun registerUser(name: String, email: String) {
        val pwd = findViewById<EditText>(R.id.editTextTextPassword)
        val password=pwd.text.toString().trim()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid // Get Firebase Authentication User ID

                    if (userId != null) {
                        val user = hashMapOf(
                            "fullName" to name,
                            "email" to email
                        )

                        // 🔹 Store user details in Firebase Realtime Database
                        database.child(userId).setValue(user)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    showToast("Registration Successful")
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                } else {
                                    showToast("Database Error: ${dbTask.exception?.message}")
                                }
                            }
                    }
                } else {
                    showToast("Authentication Failed: ${task.exception?.message}")
                }
            }
    }


    // ✅ Ensures Toast messages always run on the main thread
    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }
}