package com.example.expensestracker.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.expensestracker.data.database.ExpenseDao
import com.example.expensestracker.data.model.Expense
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    val allExpenses: LiveData<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun syncExpensesFromFirestore() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("ExpenseRepository", "User not logged in, cannot sync from Firestore.")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val userExpensesCollection = firestore.collection("users").document(userId).collection("expenses")
                val snapshot = userExpensesCollection.get().await()
                val firestoreExpenses = snapshot.documents.mapNotNull { document ->
                    // When converting from Firestore, ensure the firestoreId is set from the document ID
                    document.toObject(Expense::class.java)?.apply {
                        this.firestoreId = document.id // Set the firestoreId from the document's ID
                    }
                }

                // Clear existing Room data and insert fresh data from Firestore
                expenseDao.deleteAll()
                firestoreExpenses.forEach { expenseDao.insert(it) } // Insert with the firestoreId
                Log.d("ExpenseRepository", "Successfully synced ${firestoreExpenses.size} expenses from Firestore to Room.")

            } catch (e: Exception) {
                Log.e("ExpenseRepository", "Error syncing expenses from Firestore: ${e.message}", e)
            }
        }
    }

    // --- MODIFIED: Insert function to save to both Room and Firestore and capture firestoreId ---
    suspend fun insert(expense: Expense) {
        withContext(Dispatchers.IO) {
            try {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val firestoreExpensesCollection = firestore.collection("users").document(userId).collection("expenses")
                    val newFirestoreDocRef = firestoreExpensesCollection.document() // Get a new document reference (generates ID)
                    val firestoreDocId = newFirestoreDocRef.id // Get the generated ID

                    // Create a copy of the expense with the Firestore ID
                    val expenseWithFirestoreId = expense.copy(firestoreId = firestoreDocId)

                    // 1. Save to Firestore first (or concurrently)
                    newFirestoreDocRef.set(expenseWithFirestoreId).await() // Use await() for sequential execution
                    Log.d("ExpenseRepository", "Expense added to Firestore with ID: $firestoreDocId")

                    // 2. Save to Room Database, or update if it already exists (e.g., from initial sync)
                    // If the expense already has a Room ID (e.g., from a previous sync), update it.
                    // Otherwise, insert it.
                    val existingExpense = expenseDao.getExpenseByFirestoreId(firestoreDocId) // NEW DAO METHOD NEEDED
                    if (existingExpense != null) {
                        // If it exists, update its Room ID and other fields
                        expenseWithFirestoreId.id = existingExpense.id // Keep the existing Room ID
                        expenseDao.update(expenseWithFirestoreId)
                        Log.d("ExpenseRepository", "Updated existing expense in Room with Firestore ID: ${expenseWithFirestoreId.title}")
                    } else {
                        // If it's a new expense, insert it into Room
                        val newRoomId = expenseDao.insert(expenseWithFirestoreId)
                        // Update the expense object with the Room ID for future operations if needed
                        expenseWithFirestoreId.id = newRoomId.toInt()
                        Log.d("ExpenseRepository", "Inserted new expense into Room with ID: $newRoomId")
                    }

                } else {
                    // If user not logged in, only save to Room
                    expenseDao.insert(expense)
                    Log.w("ExpenseRepository", "User not logged in, expense only saved locally.")
                }
            } catch (e: Exception) {
                Log.e("ExpenseRepository", "Error inserting expense: ${e.message}", e)
            }
        }
    }

    // --- MODIFIED: Delete function to delete from both Room and Firestore using firestoreId ---
    suspend fun delete(expense: Expense) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Delete from Room Database
                expenseDao.delete(expense)
                Log.d("ExpenseRepository", "Successfully deleted expense from Room: ${expense.title}")

                // 2. Delete from Firebase Firestore using firestoreId
                val userId = auth.currentUser?.uid
                val firestoreId = expense.firestoreId
                if (userId != null && firestoreId != null) {
                    firestore.collection("users").document(userId).collection("expenses")
                        .document(firestoreId) // Use the stored firestoreId for direct deletion
                        .delete()
                        .addOnSuccessListener {
                            Log.d("ExpenseRepository", "Expense deleted from Firestore with ID: $firestoreId")
                        }
                        .addOnFailureListener { e ->
                            Log.e("ExpenseRepository", "Error deleting expense from Firestore: ${e.message}", e)
                        }
                } else if (userId != null && firestoreId == null) {
                    Log.w("ExpenseRepository", "Expense has no firestoreId, skipping Firestore deletion. Expense: ${expense.title}")
                } else {
                    Log.w("ExpenseRepository", "User not logged in, expense only deleted locally.")
                }
            } catch (e: Exception) {
                Log.e("ExpenseRepository", "Error deleting expense: ${e.message}", e)
            }
        }
    }

    suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            expenseDao.deleteAll()
        }
    }
}
