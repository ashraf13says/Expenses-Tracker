package com.example.expensestracker.ui.expenses

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.expensestracker.R
import com.example.expensestracker.data.database.AppDatabase
import com.example.expensestracker.data.model.Expense
import com.example.expensestracker.data.repository.ExpenseRepository
import com.example.expensestracker.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.util.Log
import android.view.LayoutInflater // ADDED: Missing import for LayoutInflater
import android.content.DialogInterface // ADDED: Missing import for DialogInterface

class ExpenseDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: ExpenseViewModel
    private var currentExpense: Expense? = null

    private lateinit var detailTitle: TextView
    private lateinit var detailAmount: TextView
    private lateinit var detailCategory: TextView
    private lateinit var detailDate: TextView
    private lateinit var btnEditExpense: Button
    private lateinit var btnDeleteExpense: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_detail)

        detailTitle = findViewById(R.id.detail_title)
        detailAmount = findViewById(R.id.detail_amount)
        detailCategory = findViewById(R.id.detail_category)
        detailDate = findViewById(R.id.detail_date)
        btnEditExpense = findViewById(R.id.btn_edit_expense)
        btnDeleteExpense = findViewById(R.id.btn_delete_expense)

        // Setup ViewModel
        val dao = AppDatabase.getDatabase(applicationContext).expenseDao()
        val repository = ExpenseRepository(dao)
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ExpenseViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        })[ExpenseViewModel::class.java]

        // Get expense data from Intent
        val expenseId = intent.getIntExtra("expense_id", 0)
        val expenseTitle = intent.getStringExtra("expense_title") ?: ""
        val expenseAmount = intent.getDoubleExtra("expense_amount", 0.0)
        val expenseDate = intent.getLongExtra("expense_date", 0L)
        val expenseCategory = intent.getStringExtra("expense_category") ?: ""
        val expenseFirestoreId = intent.getStringExtra("expense_firestore_id")

        currentExpense = Expense(
            id = expenseId,
            title = expenseTitle,
            amount = expenseAmount,
            date = expenseDate,
            category = expenseCategory,
            firestoreId = expenseFirestoreId
        )

        displayExpenseDetails(currentExpense!!)

        btnEditExpense.setOnClickListener {
            showEditExpenseDialog(currentExpense!!)
        }

        btnDeleteExpense.setOnClickListener {
            confirmDeleteExpense(currentExpense!!)
        }
    }

    private fun displayExpenseDetails(expense: Expense) {
        detailTitle.text = expense.title
        detailAmount.text = String.format("$%.2f", expense.amount)
        detailCategory.text = "Category: ${expense.category}"
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        detailDate.text = "Date: ${dateFormat.format(Date(expense.date))}"
    }

    private fun showEditExpenseDialog(expense: Expense) {
        val builder = AlertDialog.Builder(this)
        // FIXED: Explicitly inflate the view and pass it to setView
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null)

        val titleInput: EditText = dialogView.findViewById(R.id.edit_text_title)
        val amountInput: EditText = dialogView.findViewById(R.id.edit_text_amount)
        val dateInput: EditText = dialogView.findViewById(R.id.edit_text_date)
        val categorySpinner: Spinner = dialogView.findViewById(R.id.spinner_category)

        // Populate fields with current expense data
        titleInput.setText(expense.title)
        amountInput.setText(expense.amount.toString())
        val currentCalendar = Calendar.getInstance().apply { timeInMillis = expense.date }
        dateInput.setText("${currentCalendar.get(Calendar.DAY_OF_MONTH)}/${currentCalendar.get(Calendar.MONTH) + 1}/${currentCalendar.get(Calendar.YEAR)}")
        var selectedDateForEdit: Date = Date(expense.date)

        // Setup spinner with current category selected
        val categories = arrayOf("Food", "Transport", "Utilities", "Entertainment", "Shopping", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
        val categoryIndex = categories.indexOf(expense.category)
        if (categoryIndex != -1) {
            categorySpinner.setSelection(categoryIndex)
        }

        dateInput.setOnClickListener {
            val calendar = Calendar.getInstance().apply { timeInMillis = expense.date }
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDateForEdit = calendar.time
                    dateInput.setText("${dayOfMonth}/${month + 1}/${year}")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        builder.setView(dialogView) // FIXED: Pass the inflated View object
            .setTitle("Edit Expense")
            .setPositiveButton("Save") { dialog: DialogInterface, _: Int -> // FIXED: Explicitly type parameters
                val newTitle = titleInput.text.toString()
                val newAmount = amountInput.text.toString().toDoubleOrNull() ?: 0.0
                val newCategory = categorySpinner.selectedItem.toString()

                val updatedExpense = expense.copy(
                    title = newTitle,
                    amount = newAmount,
                    date = selectedDateForEdit.time,
                    category = newCategory
                )

                lifecycleScope.launch {
                    viewModel.addExpense(updatedExpense) // Room's insert with REPLACE strategy handles update
                    Toast.makeText(this@ExpenseDetailActivity, "Expense updated!", Toast.LENGTH_SHORT).show()
                    finish() // Close activity after update
                }
            }
            .setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.dismiss() } // FIXED: Explicitly type parameters

        builder.create().show()
    }

    private fun confirmDeleteExpense(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete '${expense.title}'?")
            .setPositiveButton("Delete") { dialog: DialogInterface, _: Int -> // FIXED: Explicitly type parameters
                lifecycleScope.launch {
                    viewModel.deleteExpense(expense)
                    Toast.makeText(this@ExpenseDetailActivity, "Expense deleted!", Toast.LENGTH_SHORT).show()
                    finish() // Close activity after deletion
                }
            }
            .setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.dismiss() } // FIXED: Explicitly type parameters
            .create()
            .show()
    }
}
