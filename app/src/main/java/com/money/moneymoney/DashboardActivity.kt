package com.money.moneymoney

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar

class DashboardActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DashboardActivity"
    }

    private lateinit var textViewIncomeINR: TextView
    private lateinit var textViewExpensesINR: TextView
    private lateinit var textViewInvestmentsINR: TextView
    private lateinit var textViewIncomeAED: TextView
    private lateinit var textViewExpensesAED: TextView
    private lateinit var textViewInvestmentsAED: TextView

    private lateinit var recyclerViewGoalProgress: RecyclerView
    private lateinit var goalProgressAdapter: GoalProgressAdapter
    private lateinit var goalDao: GoalDao
    private lateinit var investmentDao: InvestmentDao
    private lateinit var incomeDao: IncomeDao
    private lateinit var expenseDao: ExpenseDao

    private lateinit var buttonAddIncome: Button
    private lateinit var buttonAddExpense: Button
    private lateinit var buttonAddInvestment: Button
    private lateinit var buttonAddGoal: Button
    private lateinit var buttonViewReports: Button
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize TextViews for Financial Summary
        textViewIncomeINR = findViewById(R.id.textViewIncomeINR)
        textViewExpensesINR = findViewById(R.id.textViewExpensesINR)
        textViewInvestmentsINR = findViewById(R.id.textViewInvestmentsINR)
        textViewIncomeAED = findViewById(R.id.textViewIncomeAED)
        textViewExpensesAED = findViewById(R.id.textViewExpensesAED)
        textViewInvestmentsAED = findViewById(R.id.textViewInvestmentsAED)

        // Initialize RecyclerView for Goal Progress
        recyclerViewGoalProgress = findViewById(R.id.recyclerViewGoalProgress)
        recyclerViewGoalProgress.layoutManager = LinearLayoutManager(this)
        goalProgressAdapter = GoalProgressAdapter(emptyList()) // Initialize with an empty list
        recyclerViewGoalProgress.adapter = goalProgressAdapter

        // Initialize DAOs
        goalDao = GoalDao(this)
        investmentDao = InvestmentDao(this)
        incomeDao = IncomeDao(this)
        expenseDao = ExpenseDao(this)

        // Initialize Quick Action Buttons
        buttonAddIncome = findViewById(R.id.buttonAddIncome)
        buttonAddExpense = findViewById(R.id.buttonAddExpense)
        buttonAddInvestment = findViewById(R.id.buttonAddInvestment)
        buttonAddGoal = findViewById(R.id.buttonAddGoal)
        buttonViewReports = findViewById(R.id.buttonViewReports)
        
        // Initialize Bottom Navigation
        bottomNavigation = findViewById(R.id.bottomNavigationView)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    // Already on home screen
                    true
                }
                else -> false
            }
        }
        
        // Set the current item to home
        bottomNavigation.selectedItemId = R.id.menu_home

        // Set OnClickListeners for Quick Action Buttons
        buttonAddIncome.setOnClickListener {
            Log.d(TAG, "Add Income button clicked")
            try {
                val intent = Intent(this, IncomeEntryActivity::class.java)
                Log.d(TAG, "Starting IncomeEntryActivity")
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting IncomeEntryActivity", e)
                val errorMessage = when (e) {
                    is SecurityException -> "Permission denied to start Income Entry"
                    is android.content.ActivityNotFoundException -> "Income Entry screen not found"
                    else -> "Error opening Income Entry: ${e.message}"
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
        buttonAddExpense.setOnClickListener {
            Log.d(TAG, "Add Expense button clicked")
            try {
                val intent = Intent(this, ExpenseEntryActivity::class.java)
                startActivity(intent)
                Log.d(TAG, "Successfully started ExpenseEntryActivity")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting ExpenseEntryActivity", e)
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        buttonAddInvestment.setOnClickListener {
            Log.d(TAG, "Add Investment button clicked")
            try {
                val intent = Intent(this, InvestmentEntryActivity::class.java)
                Log.d(TAG, "Starting InvestmentEntryActivity")
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting InvestmentEntryActivity", e)
                val errorMessage = when (e) {
                    is SecurityException -> "Permission denied to start Investment Entry"
                    is android.content.ActivityNotFoundException -> "Investment Entry screen not found"
                    else -> "Error opening Investment Entry: ${e.message}"
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
        buttonAddGoal.setOnClickListener {
            Log.d(TAG, "Add Goal button clicked")
            try {
                val intent = Intent(this, GoalEntryActivity::class.java)
                startActivity(intent)
                Log.d(TAG, "Successfully started GoalEntryActivity")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting GoalEntryActivity", e)
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        buttonViewReports.setOnClickListener {
            Log.d(TAG, "View Reports button clicked")
            try {
                val intent = Intent(this, CurrencySelectionActivity::class.java)
                startActivity(intent)
                Log.d(TAG, "Successfully started CurrencySelectionActivity")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting CurrencySelectionActivity", e)
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Load data for the dashboard
        loadFinancialSummary()
        loadGoalProgress()
    }

    private fun loadFinancialSummary() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        val incomeList = incomeDao.getIncomesForMonth(currentYear, currentMonth)
        val expenseList = expenseDao.getExpensesForMonth(currentYear, currentMonth)
        val investmentList = investmentDao.getInvestmentsForMonth(currentYear, currentMonth)

        var totalIncomeINR = 0.00
        var totalExpensesINR = 0.00
        var totalInvestmentsINR = 0.00
        var totalIncomeAED = 0.00
        var totalExpensesAED = 0.00
        var totalInvestmentsAED = 0.00

        for (income in incomeList.asSequence()) {
            when (income.currency) {
                "INR" -> totalIncomeINR += income.value
                "AED" -> totalIncomeAED += income.value
            }
        }

        for (expense in expenseList.asSequence()) {
            when (expense.currency) {
                "INR" -> totalExpensesINR += expense.value
                "AED" -> totalExpensesAED += expense.value
            }
        }

        for (investment in investmentList.asSequence()) {
            when (investment.currency) {
                "INR" -> totalInvestmentsINR += investment.value
                "AED" -> totalInvestmentsAED += investment.value
            }
        }

        textViewIncomeINR.text = "Income: ₹${String.format("%.2f", totalIncomeINR)}"
        textViewExpensesINR.text = "Expenses: ₹${String.format("%.2f", totalExpensesINR)}"
        textViewInvestmentsINR.text = "Investments: ₹${String.format("%.2f", totalInvestmentsINR)}"

        textViewIncomeAED.text = "Income: د.إ ${String.format("%.2f", totalIncomeAED)}"
        textViewExpensesAED.text = "Expenses: د.إ ${String.format("%.2f", totalExpensesAED)}"
        textViewInvestmentsAED.text = "Investments: د.إ ${String.format("%.2f", totalInvestmentsAED)}"
    }

    private fun loadGoalProgress() {
        val activeGoals = goalDao.getAllActiveGoals()
        val goalProgressList = mutableListOf<GoalWithProgress>()

        for (goal in activeGoals) {
            val investmentsForGoal = investmentDao.getInvestmentsByGoalId(goal.id)
            val amountInvested = investmentsForGoal.sumOf { it.value  }
            val percentageProgress = if (goal.targetValue > 0) (amountInvested / goal.targetValue.toDouble() * 100).toInt() else 0
            val remainingAmount = goal.targetValue - amountInvested

            goalProgressList.add(GoalWithProgress(goal = goal, amountInvested = amountInvested, percentageProgress = percentageProgress, remainingAmount = remainingAmount))
        }

        goalProgressAdapter.updateGoals(goalProgressList)
    }

    override fun onDestroy() {
        super.onDestroy()
        goalDao.close()
        investmentDao.close()
        incomeDao.close()
        expenseDao.close()
    }
}

data class GoalWithProgress(
    val goal: Goal,
    val amountInvested: Double,
    val percentageProgress: Int,
    val remainingAmount: Double
)
