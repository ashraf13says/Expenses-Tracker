package com.example.expensestracker.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.expensestracker.R
import com.example.expensestracker.data.model.User
import com.example.expensestracker.ui.expenses.ExpensesActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistrationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var ageEditText: EditText
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        nameEditText = findViewById(R.id.name_edit_text)
        emailEditText = findViewById(R.id.email_edit_text)
        passwordEditText = findViewById(R.id.password_edit_text)
        ageEditText = findViewById(R.id.age_edit_text)
        registerButton = findViewById(R.id.register_button)

        registerButton.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val name = nameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val age = ageEditText.text.toString().trim().toIntOrNull()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || age == null) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        // Save user data (name and age) to Firestore
                        val user = User(
                            name = name,
                            email = email,
                            age = age
                        )
                        firestore.collection("users")
                            .document(firebaseUser.uid)
                            .set(user)
                            .addOnSuccessListener {
                                Log.d("RegistrationActivity", "User data saved to Firestore.")
                                // MODIFIED: After successful registration, navigate back to LoginActivity
                                Toast.makeText(this, "Registration successful! Please log in.", Toast.LENGTH_LONG).show()
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)
                                finish() // Finish this activity so they can't go back
                            }
                            .addOnFailureListener { e ->
                                Log.e("RegistrationActivity", "Error saving user data to Firestore.", e)
                                Toast.makeText(this, "Registration failed: Could not save user data.", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Log.e("RegistrationActivity", "Registration failed: ${task.exception?.message}")
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
