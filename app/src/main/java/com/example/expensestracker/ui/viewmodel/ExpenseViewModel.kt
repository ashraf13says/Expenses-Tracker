package com.example.expensestracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asFlow
import com.example.expensestracker.data.model.Expense
import com.example.expensestracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log
import com.example.expensestracker.ui.expenses.RecyclerViewItem // Import the new sealed class
import com.example.expensestracker.ui.expenses.RecyclerViewItem.HeaderItem
import com.example.expensestracker.ui.expenses.RecyclerViewItem.ExpenseItem

class ExpenseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(getCurrentDate())
    val selectedDate: StateFlow<String> = _selectedDate

    // CRITICAL CHANGE: expenses now emits List<RecyclerViewItem> for grouping
    val expenses: StateFlow<List<RecyclerViewItem>> = repository.allExpenses.asFlow()
        .map { expensesList ->
            // Group expenses by category
            val groupedExpenses = expensesList
                .sortedWith(compareBy({ it.category }, { it.date })) // Sort by category then date
                .groupBy { it.category }

            val items = mutableListOf<RecyclerViewItem>()
            groupedExpenses.forEach { (category, expensesInCategory) ->
                // Add a header for each category
                val totalAmount = expensesInCategory.sumOf { it.amount }
                items.add(HeaderItem(category, totalAmount))
                // Add all expenses for that category
                expensesInCategory.forEach { expense ->
                    items.add(ExpenseItem(expense))
                }
            }
            Log.d("ExpenseViewModel", "Grouped expenses into ${items.size} RecyclerViewItems.")
            items.toList() // Convert mutable list to immutable list
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
        // Note: We are currently showing ALL expenses. If you want to re-introduce
        // date filtering, you would modify the 'map' block above to filter by _selectedDate
        // before grouping.
    }

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            repository.insert(expense)
            Log.d("ExpenseViewModel", "Attempted to add expense: ${expense.title}, Amount: ${expense.amount}, Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(expense.date))}, Category: ${expense.category}")
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.delete(expense)
        }
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun convertTimestampToDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}
