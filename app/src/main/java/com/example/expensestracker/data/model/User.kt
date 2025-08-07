package com.example.expensestracker.data.model

// Data class for a User, to be stored in Firestore
data class User(
    val name: String = "",
    val email: String = "",
    val age: Int = 0
)
