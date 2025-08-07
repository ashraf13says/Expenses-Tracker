package com.example.expensestracker.ui.expenses

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expensestracker.R
import com.example.expensestracker.data.model.Expense
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import android.util.Log

// Define view types for our different RecyclerView items
private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_EXPENSE = 1

// Add a lambda function for item clicks
class ExpensesAdapter(private val onItemClick: (Expense) -> Unit) :
    ListAdapter<RecyclerViewItem, RecyclerView.ViewHolder>(ExpenseDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is RecyclerViewItem.HeaderItem -> VIEW_TYPE_HEADER
            is RecyclerViewItem.ExpenseItem -> VIEW_TYPE_EXPENSE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_EXPENSE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_expense, parent, false)
                ExpenseViewHolder(view, onItemClick) // Pass the click listener to ExpenseViewHolder
            }
            else -> throw IllegalArgumentException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_HEADER -> {
                val headerItem = getItem(position) as RecyclerViewItem.HeaderItem
                (holder as HeaderViewHolder).bind(headerItem)
            }
            VIEW_TYPE_EXPENSE -> {
                val expenseItem = getItem(position) as RecyclerViewItem.ExpenseItem
                (holder as ExpenseViewHolder).bind(expenseItem.expense)
            }
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryNameTextView: TextView = itemView.findViewById(R.id.text_view_category_name)
        private val categoryTotalTextView: TextView = itemView.findViewById(R.id.text_view_category_total)

        fun bind(header: RecyclerViewItem.HeaderItem) {
            categoryNameTextView.text = header.categoryName
            categoryTotalTextView.text = String.format("Total: $%.2f", header.totalAmount)
            Log.d("HeaderViewHolder", "Binding header: ${header.categoryName}, Total: ${header.totalAmount}")
        }
    }

    // --- MODIFIED: ExpenseViewHolder to handle clicks ---
    class ExpenseViewHolder(itemView: View, private val onItemClick: (Expense) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val categoryTextView: TextView = itemView.findViewById(R.id.category_text_view)
        private val amountTextView: TextView = itemView.findViewById(R.id.amount_text_view)
        private val dateTextView: TextView = itemView.findViewById(R.id.date_text_view)
        private val titleTextView: TextView = itemView.findViewById(R.id.title_text_view)

        fun bind(expense: Expense) {
            categoryTextView.text = expense.category
            amountTextView.text = String.format("$%.2f", expense.amount)
            titleTextView.text = expense.title

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateTextView.text = dateFormat.format(Date(expense.date))
            Log.d("ExpenseViewHolder", "Binding expense: ${expense.title}, Amount: ${expense.amount}")

            // Set click listener for the entire item view
            itemView.setOnClickListener {
                onItemClick(expense)
            }
        }
    }

    class ExpenseDiffCallback : DiffUtil.ItemCallback<RecyclerViewItem>() {
        override fun areItemsTheSame(oldItem: RecyclerViewItem, newItem: RecyclerViewItem): Boolean {
            return when {
                oldItem is RecyclerViewItem.HeaderItem && newItem is RecyclerViewItem.HeaderItem ->
                    oldItem.categoryName == newItem.categoryName
                oldItem is RecyclerViewItem.ExpenseItem && newItem is RecyclerViewItem.ExpenseItem ->
                    oldItem.expense.id == newItem.expense.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: RecyclerViewItem, newItem: RecyclerViewItem): Boolean {
            return oldItem == newItem
        }
    }
}
