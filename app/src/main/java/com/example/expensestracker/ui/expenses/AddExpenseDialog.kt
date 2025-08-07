package com.example.expensestracker.ui.expenses

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.expensestracker.R
import java.util.Calendar
import java.util.Date
import android.widget.Spinner
import android.widget.ArrayAdapter


class AddExpenseDialog(
    private val onAddExpense: (title: String, amount: Double, category: String, date: Date) -> Unit
) : DialogFragment() {

    private lateinit var titleInput: EditText
    private lateinit var amountInput: EditText
    private lateinit var dateInput: EditText
    private lateinit var categorySpinner: Spinner

    private var selectedDate: Date = Date()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_expense, null)

        titleInput = view.findViewById(R.id.edit_text_title)
        amountInput = view.findViewById(R.id.edit_text_amount)
        dateInput = view.findViewById(R.id.edit_text_date)
        categorySpinner = view.findViewById(R.id.spinner_category)

        // Setup date picker
        dateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.time
                    dateInput.setText("${dayOfMonth}/${month + 1}/${year}")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        // Setup spinner
        val categories = arrayOf("Food", "Transport", "Utilities", "Entertainment", "Shopping", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        // Build dialog
        builder.setView(view)
            .setTitle("Add Expense")
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString()
                val amount = amountInput.text.toString().toDoubleOrNull() ?: 0.0
                val category = categorySpinner.selectedItem.toString()
                onAddExpense(title, amount, category, selectedDate)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        return builder.create()
    }
}
