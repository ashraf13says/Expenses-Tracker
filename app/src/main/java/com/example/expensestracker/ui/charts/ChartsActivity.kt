package com.example.expensestracker.ui.charts

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.expensestracker.R
import com.example.expensestracker.data.database.AppDatabase
import com.example.expensestracker.data.model.Expense
import com.example.expensestracker.data.repository.ExpenseRepository
import com.example.expensestracker.viewmodel.ExpenseViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.util.Log
import com.example.expensestracker.ui.expenses.RecyclerViewItem // Import RecyclerViewItem

class ChartsActivity : AppCompatActivity() {

    private lateinit var viewModel: ExpenseViewModel
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ChartsActivity", "onCreate: Setting content view.")
        setContentView(R.layout.activity_charts)
        Log.d("ChartsActivity", "onCreate: Content view set. Initializing chart views.")

        // Initialize chart views
        try {
            pieChart = findViewById(R.id.pie_chart)
            barChart = findViewById(R.id.bar_chart)
            Log.d("ChartsActivity", "onCreate: Chart views initialized successfully.")
        } catch (e: Exception) {
            Log.e("ChartsActivity", "onCreate: Error initializing chart views: ${e.message}", e)
            return
        }

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
        Log.d("ChartsActivity", "onCreate: ViewModel setup complete.")

        // Observe expenses and update charts
        lifecycleScope.launch {
            Log.d("ChartsActivity", "onCreate: Starting expense observation.")
            viewModel.expenses.collectLatest { recyclerViewItems ->
                // CRITICAL CHANGE: Filter out only ExpenseItem and extract the Expense object
                val expenses = recyclerViewItems.mapNotNull { item ->
                    if (item is RecyclerViewItem.ExpenseItem) {
                        item.expense
                    } else {
                        null // Ignore HeaderItems
                    }
                }
                Log.d("ChartsActivity", "Received and filtered expenses for charts: ${expenses.size} items.")

                if (expenses.isEmpty()) {
                    Log.d("ChartsActivity", "Filtered expenses list is empty. Charts will show 'No Data' text.")
                }
                updatePieChart(expenses)
                updateBarChart(expenses)
                Log.d("ChartsActivity", "Charts updated.")
            }
        }
    }

    private fun updatePieChart(expenses: List<Expense>) {
        Log.d("ChartsActivity", "updatePieChart: called with ${expenses.size} expenses.")
        val categorySpending = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val entries = ArrayList<PieEntry>()
        for ((category, amount) in categorySpending) {
            entries.add(PieEntry(amount.toFloat(), category))
        }

        if (entries.isEmpty()) {
            pieChart.clear()
            pieChart.setNoDataText("No expense data for Pie Chart.")
            pieChart.invalidate()
            Log.d("ChartsActivity", "updatePieChart: No entries, setting no data text.")
            return
        }

        val dataSet = PieDataSet(entries, "Expense Categories")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pieChart))
        pieChart.data = data
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setEntryLabelTextSize(10f)
        pieChart.centerText = "Spending by Category"
        pieChart.setCenterTextSize(16f)
        pieChart.animateY(1000)
        pieChart.invalidate()
        Log.d("ChartsActivity", "updatePieChart: Pie chart updated successfully.")
    }

    private fun updateBarChart(expenses: List<Expense>) {
        Log.d("ChartsActivity", "updateBarChart: called with ${expenses.size} expenses.")
        val monthlySpending = expenses.groupBy { expense ->
            val calendar = Calendar.getInstance().apply { timeInMillis = expense.date }
            SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
        }.mapValues { (_, list) -> list.sumOf { it.amount } }
            .toSortedMap()

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        var index = 0f

        for ((monthYear, amount) in monthlySpending) {
            entries.add(BarEntry(index, amount.toFloat()))
            labels.add(monthYear)
            index++
        }

        if (entries.isEmpty()) {
            barChart.clear()
            barChart.setNoDataText("No expense data for Bar Chart.")
            barChart.invalidate()
            Log.d("ChartsActivity", "updateBarChart: No entries, setting no data text.")
            return
        }

        val dataSet = BarDataSet(entries, "Monthly Spending")
        dataSet.colors = ColorTemplate.VORDIPLOM_COLORS.toList()
        dataSet.valueTextSize = 10f
        dataSet.valueTextColor = Color.BLACK

        val data = BarData(dataSet)
        barChart.data = data
        barChart.description.isEnabled = false
        barChart.setFitBars(true)
        barChart.animateY(1000)

        barChart.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
        barChart.xAxis.granularity = 1f
        barChart.xAxis.setCenterAxisLabels(true)
        barChart.xAxis.setDrawGridLines(false)
        barChart.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM

        barChart.axisRight.isEnabled = false

        barChart.invalidate()
        Log.d("ChartsActivity", "updateBarChart: Bar chart updated successfully.")
    }
}
