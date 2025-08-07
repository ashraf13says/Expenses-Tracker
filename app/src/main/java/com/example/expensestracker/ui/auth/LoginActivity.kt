package com.example.expensestracker.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.expensestracker.R
import com.example.expensestracker.ui.expenses.ExpensesActivity
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import com.google.android.material.textfield.TextInputEditText // Import TextInputEditText

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var signupButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Initialize UI elements
        emailEditText = findViewById(R.id.email_edit_text) // CORRECTED: Referencing email_edit_text
        passwordEditText = findViewById(R.id.password_edit_text)
        loginButton = findViewById(R.id.login_button)
        signupButton = findViewById(R.id.signup_button)

        // Check if user is already logged in
        if (auth.currentUser != null) {
            Log.d("LoginActivity", "User already logged in: ${auth.currentUser?.email}. Redirecting to ExpensesActivity.")
            navigateToExpenses()
            return
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("LoginActivity", "signInWithEmail:success")
                        Toast.makeText(baseContext, "Login successful.", Toast.LENGTH_SHORT).show()
                        navigateToExpenses()
                    } else {
                        Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG).show()
                    }
                }
        }

        signupButton.setOnClickListener {
            Log.d("LoginActivity", "Signup button clicked. Navigating to SignupActivity.")
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun navigateToExpenses() {
        val intent = Intent(this, ExpensesActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
