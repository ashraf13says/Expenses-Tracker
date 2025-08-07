// src/main/java/com/example/expensestracker/data/model/Expense.kt

package com.example.expensestracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0, // Make it var to allow Room to set it
    var title: String = "", // Make it var and provide default for Firestore
    var amount: Double = 0.0, // Make it var and provide default for Firestore
    var date: Long = 0L, // Make it var and provide default for Firestore
    var category: String = "", // Make it var and provide default for Firestore
    var firestoreId: String? = null // NEW: To store the Firestore document ID
)
