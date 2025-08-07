package com.example.expensestracker.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.expensestracker.R
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.android.material.textfield.TextInputEditText // Import TextInputEditText

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var nameEditText: TextInputEditText
    private lateinit var ageEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var createAccountButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        nameEditText = findViewById(R.id.name_edit_text)
        ageEditText = findViewById(R.id.age_edit_text)
        emailEditText = findViewById(R.id.email_edit_text_signup)
        passwordEditText = findViewById(R.id.password_edit_text_signup)
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text_signup)
        createAccountButton = findViewById(R.id.create_account_button)

        createAccountButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val age = ageEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (name.isEmpty() || age.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Password and email validation logic from your original code.
            if (!isValidEmail(email) || !isValidPassword(password)) {
                Toast.makeText(this, "Email or password format is invalid.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("SignupActivity", "createUserWithEmail:success")
                        val user = auth.currentUser
                        user?.let {
                            val userData = hashMapOf(
                                "name" to name,
                                "age" to age
                            )
                            firestore.collection("users").document(it.uid)
                                .set(userData, SetOptions.merge())
                                .addOnSuccessListener {
                                    Log.d("SignupActivity", "User data saved to Firestore.")
                                    Toast.makeText(baseContext, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                    navigateToLogin() // Navigate after Firestore write is complete
                                }
                                .addOnFailureListener { e ->
                                    Log.w("SignupActivity", "Error writing document", e)
                                    Toast.makeText(baseContext, "Failed to save user data. Please try again.", Toast.LENGTH_LONG).show()
                                    navigateToLogin()
                                }
                        } ?: run {
                            Log.e("SignupActivity", "Firebase user is null after successful creation.")
                            Toast.makeText(baseContext, "Account created, but user data could not be retrieved.", Toast.LENGTH_LONG).show()
                            navigateToLogin()
                        }
                    } else {
                        Log.w("SignupActivity", "createUserWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Account creation failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.endsWith("@gmail.com")
    }

    private fun isValidPassword(password: String): Boolean {
        if (password.length < 5) return false
        if (!password.matches(Regex(".*[A-Z].*"))) return false
        if (!password.matches(Regex(".*[a-z].*"))) return false
        if (!password.matches(Regex(".*[0-9].*"))) return false
        if (!password.matches(Regex(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~].*"))) return false
        return true
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
