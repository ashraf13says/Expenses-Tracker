package com.example.expensestracker.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.expensestracker.R
import com.example.expensestracker.ui.auth.AuthManager // Corrected import statement

class MainActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var welcomeTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize AuthManager and views
        authManager = AuthManager(this)
        welcomeTextView = findViewById(R.id.welcomeTextView)

        // Get the logged-in user's name from the AuthManager
        val loggedInUserName = authManager.getLoggedInUserName()

        // Set the personalized welcome message
        if (loggedInUserName == "Guest") {
            welcomeTextView.text = "Welcome!"
        } else {
            // Display the personalized welcome message
            welcomeTextView.text = "Welcome, $loggedInUserName!"
        }
    }

    override fun onStart() {
        super.onStart()

        // Check if the user is logged in. If not, redirect to the login screen.
        // This is where you would redirect to your LoginActivity.
        if (!authManager.isLoggedIn()) {
            // Example of how you would start a login activity
            // val intent = Intent(this, LoginActivity::class.java)
            // startActivity(intent)
            // finish()
        }
    }
}
