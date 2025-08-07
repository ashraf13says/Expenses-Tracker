package com.example.expensestracker.ui.expenses

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensestracker.R
import com.example.expensestracker.data.database.AppDatabase
import com.example.expensestracker.data.model.Expense
import com.example.expensestracker.data.repository.ExpenseRepository
import com.example.expensestracker.viewmodel.ExpenseViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.util.Log
import android.view.View
import android.widget.LinearLayout // Import for LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.example.expensestracker.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.expensestracker.ui.charts.ChartsActivity
import com.example.expensestracker.ui.recommendation.RecommendationActivity
import com.example.expensestracker.ui.settings.SettingsActivity

class ExpensesActivity : AppCompatActivity() {

    // ViewModel and other data-related components
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var expenseAdapter: ExpensesAdapter
    private lateinit var expenseRepository: ExpenseRepository

    // Firebase authentication and Firestore
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // UI elements
    private lateinit var welcomeMessageTextView: TextView
    private lateinit var fabAddExpense: FloatingActionButton

    // UI elements for the expandable menu
    private lateinit var fabMenu: FloatingActionButton
    private lateinit var menuButtonsContainer: LinearLayout
    private lateinit var btnLogout: MaterialButton
    private lateinit var btnSettings: MaterialButton
    private lateinit var btnRecommendations: MaterialButton
    private lateinit var btnCharts: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expenses)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI elements
        welcomeMessageTextView = findViewById(R.id.welcome_message_text_view)
        fabAddExpense = findViewById(R.id.fab_add_expense)
        fabMenu = findViewById(R.id.fab_menu)
        menuButtonsContainer = findViewById(R.id.menu_buttons_container)
        btnLogout = findViewById(R.id.btn_logout)
        btnSettings = findViewById(R.id.btn_settings)
        btnRecommendations = findViewById(R.id.btn_view_recommendations)
        btnCharts = findViewById(R.id.btn_view_charts)


        // Setup ViewModel and Repository
        val dao = AppDatabase.getDatabase(applicationContext).expenseDao()
        expenseRepository = ExpenseRepository(dao)
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ExpenseViewModel(expenseRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        })[ExpenseViewModel::class.java]

        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.expenses_recycler_view)
        expenseAdapter = ExpensesAdapter { expense ->
            // Handle item click for navigating to detail activity
            val intent = Intent(this, ExpenseDetailActivity::class.java).apply {
                putExtra("expense_id", expense.id)
                putExtra("expense_title", expense.title)
                putExtra("expense_amount", expense.amount)
                putExtra("expense_date", expense.date)
                putExtra("expense_category", expense.category)
                putExtra("expense_firestore_id", expense.firestoreId)
            }
            startActivity(intent)
        }
        recyclerView.adapter = expenseAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Observe expenses from the ViewModel
        lifecycleScope.launch {
            viewModel.expenses.collectLatest { list ->
                Log.d("ExpensesActivity", "Fetched expenses for list: ${list.size} items")
                expenseAdapter.submitList(list)
            }
        }

        // Set click listener for the "add expense" FAB
        fabAddExpense.setOnClickListener {
            val dialog = AddExpenseDialog { title, amount, category, date ->
                viewModel.addExpense(
                    Expense(
                        title = title,
                        amount = amount,
                        date = date.time,
                        category = category
                    )
                )
            }
            dialog.show(supportFragmentManager, "AddExpenseDialog")
        }

        // Set click listener for the main menu FAB to toggle the menu
        fabMenu.setOnClickListener {
            if (menuButtonsContainer.visibility == View.VISIBLE) {
                // If the menu is visible, hide it
                menuButtonsContainer.visibility = View.GONE
            } else {
                // If the menu is hidden, show it
                menuButtonsContainer.visibility = View.VISIBLE
            }
        }

        // Set click listeners for the menu buttons
        btnLogout.setOnClickListener {
            Log.d("ExpensesActivity", "Logout button clicked. Signing out.")
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            // Clear the back stack to prevent the user from navigating back to this activity
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btnSettings.setOnClickListener {
            Log.d("ExpensesActivity", "Settings button clicked. Launching SettingsActivity.")
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        btnRecommendations.setOnClickListener {
            Log.d("ExpensesActivity", "View Recommendations button clicked. Launching RecommendationActivity.")
            val intent = Intent(this, RecommendationActivity::class.java)
            startActivity(intent)
        }

        btnCharts.setOnClickListener {
            Log.d("ExpensesActivity", "View Charts button clicked. Launching ChartsActivity.")
            val intent = Intent(this, ChartsActivity::class.java)
            startActivity(intent)
        }


        // Trigger initial data sync and fetch user name when activity starts
        lifecycleScope.launch {
            Log.d("ExpensesActivity", "Triggering initial sync from Firestore.")
            expenseRepository.syncExpensesFromFirestore()
            fetchAndDisplayUserName()
        }
    }

    /**
     * Fetches the user's name from Firestore and updates the welcome message TextView.
     */
    private fun fetchAndDisplayUserName() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { documentSnapshot ->
                    val name = documentSnapshot.getString("name")
                    if (!name.isNullOrEmpty()) {
                        welcomeMessageTextView.text = "Welcome, $name!\nYour Expenses"
                        Log.d("ExpensesActivity", "Displayed welcome message for user: $name")
                    } else {
                        welcomeMessageTextView.text = "Welcome!\nYour Expenses"
                        Log.d("ExpensesActivity", "User name not found in Firestore, displaying generic welcome.")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ExpensesActivity", "Error fetching user name: ${e.message}", e)
                    welcomeMessageTextView.text = "Welcome!\nYour Expenses" // Fallback
                }
        } else {
            welcomeMessageTextView.text = "Welcome!\nYour Expenses" // Fallback for unauthenticated
            Log.w("ExpensesActivity", "No user logged in, cannot fetch name.")
        }
    }
}
