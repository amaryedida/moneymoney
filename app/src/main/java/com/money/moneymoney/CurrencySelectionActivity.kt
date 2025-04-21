package com.money.moneymoney

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.Button
import android.widget.Toast

class CurrencySelectionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CURRENCY = "selected_currency"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_currency_selection)

        val buttonAed: Button = findViewById(R.id.button_aed)
        val buttonInr: Button = findViewById(R.id.button_inr)
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)

        val buttonIncome: Button = findViewById(R.id.button_income)
        val buttonExpense: Button = findViewById(R.id.button_expense)
        val buttonInvestment: Button = findViewById(R.id.button_investment)
        val buttonGoal: Button = findViewById(R.id.button_goal)

        var selectedCurrency: String? = null

        buttonAed.setOnClickListener {
            selectedCurrency = "AED"
            enableSubButtons(buttonIncome, buttonExpense, buttonInvestment, buttonGoal)
        }

        buttonInr.setOnClickListener {
            selectedCurrency = "INR"
            enableSubButtons(buttonIncome, buttonExpense, buttonInvestment, buttonGoal)
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    navigateToDashboardAndFinish()
                    true
                }
                else -> false
            }
        }

        buttonIncome.setOnClickListener {
            selectedCurrency?.let { currency ->
                navigateToReportList(currency, "income")
            } ?: showCurrencyNotSelectedMessage()
        }

        buttonExpense.setOnClickListener {
            selectedCurrency?.let { currency ->
                navigateToReportList(currency, "expense")
            } ?: showCurrencyNotSelectedMessage()
        }

        buttonInvestment.setOnClickListener {
            selectedCurrency?.let { currency ->
                navigateToReportList(currency, "investment")
            } ?: showCurrencyNotSelectedMessage()
        }

        buttonGoal.setOnClickListener {
            selectedCurrency?.let { currency ->
                navigateToReportList(currency, "goal")
            } ?: showCurrencyNotSelectedMessage()
        }
    }

    private fun enableSubButtons(vararg buttons: Button) {
        buttons.forEach { it.isEnabled = true }
    }

    private fun navigateToReportList(currency: String, reportType: String) {
        val intent = when (reportType) {
            "income" -> Intent(this, IncomeListActivity::class.java)
            "expense" -> Intent(this, ExpenseListActivity::class.java)
            "investment" -> Intent(this, InvestmentListActivity::class.java)
            "goal" -> Intent(this, GoalListActivity::class.java)
            else -> throw IllegalArgumentException("Invalid report type: $reportType") // Or handle the error as appropriate
        }
        intent.putExtra(EXTRA_CURRENCY, currency)
        startActivity(intent)
    }

    private fun navigateToDashboardAndFinish() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showCurrencyNotSelectedMessage() {
        Toast.makeText(this, "Please select a currency first.", Toast.LENGTH_SHORT).show()
    }
}
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateToExpenseReportList(currency: String) {
        val intent = Intent(this, ExpenseReportListActivity::class.java)  // Assuming ExpenseReportListActivity will be created
        intent.putExtra(EXTRA_CURRENCY, currency)
        startActivity(intent)
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish() // Close this activity after navigating to dashboard
    }
}
