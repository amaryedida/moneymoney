package com.money.moneymoney

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

        initializeViews()
        setupRecyclerView()
        initializeDAOs()
        setupBottomNavigation()
        setupClickListeners()
        loadDashboardData()
    }

    private fun initializeViews() {
        textViewIncomeINR = findViewById(R.id.textViewIncomeINR)
        textViewExpensesINR = findViewById(R.id.textViewExpensesINR)
        textViewInvestmentsINR = findViewById(R.id.textViewInvestmentsINR)
        textViewIncomeAED = findViewById(R.id.textViewIncomeAED)
        textViewExpensesAED = findViewById(R.id.textViewExpensesAED)
        textViewInvestmentsAED = findViewById(R.id.textViewInvestmentsAED)

        buttonAddIncome = findViewById(R.id.buttonAddIncome)
        buttonAddExpense = findViewById(R.id.buttonAddExpense)
        buttonAddInvestment = findViewById(R.id.buttonAddInvestment)
        buttonAddGoal = findViewById(R.id.buttonAddGoal)
        buttonViewReports = findViewById(R.id.buttonViewReports)
        bottomNavigation = findViewById(R.id.bottomNavigationView)
    }

    private fun setupRecyclerView() {
        recyclerViewGoalProgress = findViewById(R.id.recyclerViewGoalProgress)
        recyclerViewGoalProgress.layoutManager = LinearLayoutManager(this)
        goalProgressAdapter = GoalProgressAdapter(emptyList())
        recyclerViewGoalProgress.adapter = goalProgressAdapter
    }

    private fun initializeDAOs() {
        goalDao = GoalDao(this)
        investmentDao = InvestmentDao(this)
        incomeDao = IncomeDao(this)
        expenseDao = ExpenseDao(this)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_home -> true // Already on home screen
                else -> false
            }
        }
        bottomNavigation.selectedItemId = R.id.menu_home
    }

    private fun setupClickListeners() {
        buttonAddIncome.setOnClickListener {
            startActivityWithErrorHandling(
                IncomeEntryActivity::class.java,
                "Add Income",
                "Income Entry"
            )
        }

        buttonAddExpense.setOnClickListener {
            startActivityWithErrorHandling(
                ExpenseEntryActivity::class.java,
                "Add Expense",
                "Expense Entry"
            )
        }

        buttonAddInvestment.setOnClickListener {
            startActivityWithErrorHandling(
                InvestmentEntryActivity::class.java,
                "Add Investment",
                "Investment Entry"
            )
        }

        buttonAddGoal.setOnClickListener {
            startActivityWithErrorHandling(
                GoalEntryActivity::class.java,
                "Add Goal",
                "Goal Entry"
            )
        }

        buttonViewReports.setOnClickListener {
            startActivityWithErrorHandling(
                CurrencySelectionActivity::class.java,
                "View Reports",
                "Currency Selection"
            )
        }
    }

    private fun startActivityWithErrorHandling(
        activityClass: Class<*>,
        action: String,
        screenName: String
    ) {
        Log.d(TAG, "$action button clicked")
        try {
            val intent = Intent(this, activityClass)
            startActivity(intent)
            Log.d(TAG, "Successfully started ${activityClass.simpleName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting $screenName", e)
            val errorMessage = when (e) {
                is SecurityException -> "Permission denied to start $screenName"
                is android.content.ActivityNotFoundException -> "$screenName screen not found"
                else -> "Error opening $screenName: ${e.message}"
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDashboardData() {
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

        for (income in incomeList) {
            when (income.currency) {
                "INR" -> totalIncomeINR += income.value
                "AED" -> totalIncomeAED += income.value
            }
        }

        for (expense in expenseList) {
            when (expense.currency) {
                "INR" -> totalExpensesINR += expense.value
                "AED" -> totalExpensesAED += expense.value
            }
        }

        for (investment in investmentList) {
            when (investment.currency) {
                "INR" -> totalInvestmentsINR += investment.value
                "AED" -> totalInvestmentsAED += investment.value
            }
        }

        updateFinancialSummaryViews(
            totalIncomeINR, totalExpensesINR, totalInvestmentsINR,
            totalIncomeAED, totalExpensesAED, totalInvestmentsAED
        )
    }

    private fun updateFinancialSummaryViews(
        totalIncomeINR: Double, totalExpensesINR: Double, totalInvestmentsINR: Double,
        totalIncomeAED: Double, totalExpensesAED: Double, totalInvestmentsAED: Double
    ) {
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
            val amountInvested = investmentsForGoal.sumOf { it.value }
            val percentageProgress = if (goal.targetValue > 0) {
                (amountInvested / goal.targetValue * 100).toInt()
            } else {
                0
            }
            val remainingAmount = goal.targetValue - amountInvested

            goalProgressList.add(
                GoalWithProgress(
                    goal = goal,
                    amountInvested = amountInvested,
                    percentageProgress = percentageProgress,
                    remainingAmount = remainingAmount
                )
            )
        }

        goalProgressAdapter.updateGoals(goalProgressList)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            goalDao.close()
            investmentDao.close()
            incomeDao.close()
            expenseDao.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing database connections", e)
        }
    }
}

data class GoalWithProgress(
    val goal: Goal,
    val amountInvested: Double,
    val percentageProgress: Int,
    val remainingAmount: Double
)
