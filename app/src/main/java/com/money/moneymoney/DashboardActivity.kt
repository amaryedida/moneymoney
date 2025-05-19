package com.money.moneymoney

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
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

    private lateinit var buttonViewReports: Button
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var fabDriveSync: FloatingActionButton
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_dashboard)
            Log.d(TAG, "Content view set successfully")

            initializeViews()
            setupRecyclerView()
            initializeDAOs()
            setupBottomNavigation()
            setupClickListeners()
            setupDriveSyncFab()
            loadDashboardData()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            textViewIncomeINR = findViewById(R.id.textViewIncomeINR)
            textViewExpensesINR = findViewById(R.id.textViewExpensesINR)
            textViewInvestmentsINR = findViewById(R.id.textViewInvestmentsINR)
            textViewIncomeAED = findViewById(R.id.textViewIncomeAED)
            textViewExpensesAED = findViewById(R.id.textViewExpensesAED)
            textViewInvestmentsAED = findViewById(R.id.textViewInvestmentsAED)

            buttonViewReports = findViewById(R.id.buttonViewReports)
            fabAdd = findViewById(R.id.fabAdd)
            fabDriveSync = findViewById(R.id.fabDriveSync)
            bottomNavigation = findViewById(R.id.bottomNavigationView)
            Log.d(TAG, "All views initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            throw e
        }
    }

    private fun setupRecyclerView() {
        try {
            recyclerViewGoalProgress = findViewById(R.id.recyclerViewGoalProgress)
            recyclerViewGoalProgress.layoutManager = LinearLayoutManager(this)
            goalProgressAdapter = GoalProgressAdapter(emptyList())
            recyclerViewGoalProgress.adapter = goalProgressAdapter
            Log.d(TAG, "RecyclerView setup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView", e)
            throw e
        }
    }

    private fun initializeDAOs() {
        try {
            goalDao = GoalDao(this)
            investmentDao = InvestmentDao(this)
            incomeDao = IncomeDao(this)
            expenseDao = ExpenseDao(this)
            Log.d(TAG, "All DAOs initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing DAOs", e)
            Toast.makeText(this, "Error initializing database: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
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
        fabAdd.setOnClickListener {
            showAddOptionsDialog()
        }

        buttonViewReports.setOnClickListener {
            Log.d(TAG, "View Reports button clicked")
            startActivityWithErrorHandling(
                CurrencySelectionActivity::class.java,
                "View Reports",
                "Currency Selection"
            )
        }
    }

    private fun setupDriveSyncFab() {
        fabDriveSync.setOnClickListener {
            val intent = Intent(this, DriveSyncActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showAddOptionsDialog() {
        val options = arrayOf("Add Income", "Add Expense", "Add Investment", "Add Goal")
        AlertDialog.Builder(this)
            .setTitle("Add New")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivityWithErrorHandling(
                        IncomeEntryActivity::class.java,
                        "Add Income",
                        "Income Entry"
                    )
                    1 -> startActivityWithErrorHandling(
                        ExpenseEntryActivity::class.java,
                        "Add Expense",
                        "Expense Entry"
                    )
                    2 -> startActivityWithErrorHandling(
                        InvestmentEntryActivity::class.java,
                        "Add Investment",
                        "Investment Entry"
                    )
                    3 -> startActivityWithErrorHandling(
                        GoalEntryActivity::class.java,
                        "Add Goal",
                        "Goal Entry"
                    )
                }
            }
            .show()
    }

    private fun startActivityWithErrorHandling(
        activityClass: Class<*>,
        actionName: String,
        activityName: String
    ) {
        try {
            Log.d(TAG, "Starting $activityName activity")
            val intent = Intent(this, activityClass)
            startActivity(intent)
            Log.d(TAG, "Successfully started $activityName activity")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting $activityName activity", e)
            Toast.makeText(this, "Error opening $actionName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDashboardData() {
        try {
            Log.d(TAG, "Starting to load dashboard data")
            loadFinancialSummary()
            loadGoalProgress()
            Log.d(TAG, "Dashboard data loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading dashboard data", e)
            Toast.makeText(this, "Error loading dashboard data: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadFinancialSummary() {
        try {
            Log.d(TAG, "Loading financial summary")
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)

            val incomeList = incomeDao.getIncomesForMonth(currentYear, currentMonth)
            val expenseList = expenseDao.getExpensesForMonth(currentYear, currentMonth)
            val investmentList = investmentDao.getInvestmentsForMonth(currentYear, currentMonth)

            Log.d(TAG, "Retrieved data - Incomes: ${incomeList.size}, Expenses: ${expenseList.size}, Investments: ${investmentList.size}")

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
            Log.d(TAG, "Financial summary updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading financial summary", e)
            throw e
        }
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
        try {
            val activeGoals = goalDao.getAllActiveGoals()
            val goalProgressList = activeGoals.map { goal ->
                val investments = investmentDao.getInvestmentsByGoalId(goal.id)
                val totalInvested = investments.sumOf { it.value }
                val percentageProgress = if (goal.targetValue > 0) {
                    ((totalInvested / goal.targetValue) * 100).toInt().coerceIn(0, 100)
                } else 0
                val remainingAmount = goal.targetValue - totalInvested

                GoalWithProgress(
                    goal = goal,
                    amountInvested = totalInvested,
                    percentageProgress = percentageProgress,
                    remainingAmount = remainingAmount
                )
            }
            goalProgressAdapter.updateGoals(goalProgressList)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading goal progress", e)
            Toast.makeText(this, "Error loading goal progress", Toast.LENGTH_SHORT).show()
        }
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
