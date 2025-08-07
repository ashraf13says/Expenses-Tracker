package com.example.expensestracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asFlow
import com.example.expensestracker.data.model.Expense
import com.example.expensestracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat

class RecommendationViewModel(
    private val repository: ExpenseRepository,
    private val userAge: Int // NEW: User's age passed to ViewModel
) : ViewModel() {

    // StateFlow to hold the list of recommendations
    val recommendations: StateFlow<List<String>> = repository.allExpenses.asFlow()
        .map { expensesList ->
            Log.d("RecommendationVM", "Analyzing ${expensesList.size} expenses for recommendations for age $userAge.")
            generateRecommendations(expensesList, userAge) // Pass age to generator
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Keep active as long as UI is subscribed
            initialValue = emptyList() // Start with an empty list of recommendations
        )

    // --- Mock AI Logic for Generating Recommendations (MODIFIED for Age) ---
    private fun generateRecommendations(expenses: List<Expense>, age: Int): List<String> {
        val recommendationsList = mutableListOf<String>()

        if (expenses.isEmpty()) {
            recommendationsList.add("Start by adding your first expense to get personalized recommendations!")
            return recommendationsList
        }

        val totalSpending = expenses.sumOf { it.amount }
        val categorySpending = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toSortedMap() // Sort categories alphabetically for consistent order

        // --- Age-Based Recommendations ---
        when {
            age < 25 -> {
                recommendationsList.add("As a young adult, focus on building an emergency fund (3-6 months of expenses) and start saving for retirement early, even if it's a small amount. Every bit counts!")
                recommendationsList.add("Try to keep your 'wants' spending in check and prioritize 'needs'. Budgeting apps can be very helpful.")
            }
            age >= 25 && age <= 40 -> {
                recommendationsList.add("In your early career, consider increasing your retirement contributions. Also, think about long-term goals like a down payment for a house or education savings.")
                recommendationsList.add("Review your debt. Prioritize paying off high-interest debts like credit cards.")
            }
            age > 40 && age <= 60 -> {
                recommendationsList.add("You're likely in your peak earning years! Maximize your retirement savings (e.g., 401k, IRA contributions). Explore diversifying your investment portfolio.")
                recommendationsList.add("Consider setting up college funds for dependents if applicable, and review your insurance coverage (life, health).")
            }
            age > 60 -> {
                recommendationsList.add("Nearing or in retirement, focus on preserving your capital and ensuring a stable income stream. Review your retirement accounts and healthcare costs.")
                recommendationsList.add("It's a good time to review your estate plan and ensure your financial affairs are in order.")
            }
            else -> {
                recommendationsList.add("Based on your age, here are some general financial tips:")
            }
        }

        // --- Expense-Based Recommendations (with specific Food mention) ---
        val thresholdPercentage = 0.30 // 30% of total spending
        var highSpendingCategory: String? = null
        var highSpendingAmount = 0.0

        for ((category, amount) in categorySpending) {
            if (totalSpending > 0 && (amount / totalSpending) > thresholdPercentage) {
                highSpendingCategory = category
                highSpendingAmount = amount
                break
            }
        }

        if (highSpendingCategory != null) {
            if (highSpendingCategory.equals("Food", ignoreCase = true)) {
                recommendationsList.add(
                    String.format(
                        "You spent $%.2f on 'Food', which is a significant portion of your total expenses. Look for ways to save, like cooking at home more often or planning meals.",
                        highSpendingAmount
                    )
                )
            } else {
                recommendationsList.add(
                    String.format(
                        "You spent $%.2f on '%s', which is a significant portion of your total expenses. Consider reviewing your spending in this area.",
                        highSpendingAmount, highSpendingCategory
                    )
                )
            }
        }

        // Identify Top 2 Spending Categories
        val sortedCategories = categorySpending.entries.sortedByDescending { it.value }

        if (sortedCategories.isNotEmpty()) {
            val topCategory = sortedCategories.first()
            if (!topCategory.key.equals(highSpendingCategory, ignoreCase = true)) { // Avoid duplicate if top is already highlighted
                recommendationsList.add(
                    String.format(
                        "Your top spending category is '%s' with a total of $%.2f. This is a good area to focus on for savings.",
                        topCategory.key, topCategory.value
                    )
                )
            }

            if (sortedCategories.size > 1) {
                val secondTopCategory = sortedCategories[1]
                if (!secondTopCategory.key.equals(highSpendingCategory, ignoreCase = true) &&
                    !secondTopCategory.key.equals(topCategory.key, ignoreCase = true)) { // Avoid duplicates
                    recommendationsList.add(
                        String.format(
                            "Your second highest spending category is '%s' ($%.2f). Small changes here can also make a difference.",
                            secondTopCategory.key, secondTopCategory.value
                        )
                    )
                }
            }
        }

        // General Encouragement/Tip for overall spending
        if (totalSpending > 500.0) { // Arbitrary threshold for "high" spending
            recommendationsList.add("Keep tracking your expenses! Understanding where your money goes is the first step to better financial health.")
        } else {
            recommendationsList.add("Great job managing your expenses! Consistent tracking helps you stay on top of your finances.")
        }

        // Suggest exploring charts
        recommendationsList.add("Don't forget to check the 'Charts' section for a visual breakdown of your spending!")

        return recommendationsList
    }
}
