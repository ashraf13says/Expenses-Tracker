package com.example.expensestracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.expensestracker.data.model.Expense

// IMPORTANT: Increased version to 2 because we changed the Expense schema (added firestoreId)
@Database(entities = [Expense::class], version = 2, exportSchema = false)
@TypeConverters(DateConverter::class) // Ensure DateConverter is registered
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_database"
                )
                    // CRITICAL CHANGE: Add migration strategy for schema changes
                    // This will recreate the database if schema changes, losing old data.
                    // For production, you'd define a proper Migration object.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
