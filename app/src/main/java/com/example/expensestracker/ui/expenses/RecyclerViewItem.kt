package com.example.expensestracker.ui.expenses

import com.example.expensestracker.data.model.Expense

// This sealed class represents the different types of items our RecyclerView can display.
// It can either be a category header or an actual expense item.
sealed class RecyclerViewItem {
    // Represents a category header (e.g., "Food", "Transport")
    data class HeaderItem(val categoryName: String, val totalAmount: Double) : RecyclerViewItem()
    // Represents an actual expense entry
    data class ExpenseItem(val expense: Expense) : RecyclerViewItem()
}
