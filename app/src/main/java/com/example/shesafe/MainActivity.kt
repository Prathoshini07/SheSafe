package com.example.shesafe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        // ✅ Check if user is already logged in
        if (auth.currentUser != null) {
            showToast("Welcome back!")
            startActivity(Intent(this, HomePage::class.java))
            finish() // Close MainActivity so user doesn't come back here
        }

        setContentView(R.layout.activity_main)

        // Initialize Firebase Authentication & Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://she-safe-07-default-rtdb.firebaseio.com").reference.child("users")

        // Find views
        emailEditText = findViewById(R.id.editTextTextEmailAddress)
        loginButton = findViewById(R.id.sendOtpButton)
        registerButton = findViewById(R.id.button8)

        // "Login" Button - Check if Email Exists
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                checkIfEmailRegistered(email)
            } else {
                showToast("Enter a valid email address")
            }
        }

        // "Register" Button - Redirect to Register Activity
        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkIfEmailRegistered(email: String) {
        database.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        showToast("Email exists in database")

                        val password = findViewById<EditText>(R.id.editTextTextPassword2).text.toString().trim()

                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    showToast("Login Successful")
                                    startActivity(Intent(this@MainActivity, HomePage::class.java))
                                    finish() // Close login activity
                                } else {
                                    showToast("Login failed: ${task.exception?.message}")
                                }
                            }
                    } else {
                        showToast("No such registered email!")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Database Error: ${error.message}")
                }
            })
    }

    // ✅ Show Toast messages safely on the main thread
    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }
}
