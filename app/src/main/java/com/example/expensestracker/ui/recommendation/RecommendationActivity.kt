package com.example.expensestracker.ui.recommendation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.expensestracker.R
import com.example.expensestracker.data.database.AppDatabase
import com.example.expensestracker.data.repository.ExpenseRepository
import com.example.expensestracker.viewmodel.RecommendationViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.widget.TextView
import android.util.Log

class RecommendationActivity : AppCompatActivity() {

    private lateinit var viewModel: RecommendationViewModel
    private lateinit var recommendationsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendation)

        recommendationsTextView = findViewById(R.id.recommendations_text_view)

        // NEW: Get user age from Intent
        val userAge = intent.getIntExtra("user_age", 0) // Default to 0 if not found
        Log.d("RecommendationActivity", "Received user age: $userAge")


        // Setup ViewModel
        val dao = AppDatabase.getDatabase(applicationContext).expenseDao()
        val repository = ExpenseRepository(dao)
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(RecommendationViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    // MODIFIED: Pass userAge to the ViewModel constructor
                    return RecommendationViewModel(repository, userAge) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        })[RecommendationViewModel::class.java]
        Log.d("RecommendationActivity", "ViewModel setup complete.")

        // Observe recommendations
        lifecycleScope.launch {
            Log.d("RecommendationActivity", "Starting recommendation observation.")
            viewModel.recommendations.collectLatest { recommendationsList ->
                Log.d("RecommendationActivity", "Received ${recommendationsList.size} recommendations.")
                if (recommendationsList.isEmpty()) {
                    recommendationsTextView.text = "No recommendations available yet. Add more expenses!"
                } else {
                    // Join recommendations with newlines for display
                    recommendationsTextView.text = recommendationsList.joinToString("\n\n")
                }
            }
        }
    }
}
