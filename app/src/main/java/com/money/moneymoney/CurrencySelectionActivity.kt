package com.money.moneymoney

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.Button
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import androidx.core.content.FileProvider
import android.view.View
import android.util.Log

class CurrencySelectionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CURRENCY = "selected_currency"
    }

    private var selectedCurrency: String? = null
    private val TAG = "CurrencySelectionActivity"

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

        val buttonExportIncome: Button = findViewById(R.id.button_export_income)
        val buttonExportExpense: Button = findViewById(R.id.button_export_expense)
        val buttonExportInvestment: Button = findViewById(R.id.button_export_investment)

        buttonAed.setOnClickListener {
            selectedCurrency = "AED"
            enableSubButtons(buttonIncome, buttonExpense, buttonInvestment, buttonGoal)
            buttonAed.text = "AED  ✔"
            buttonInr.text = "INR"
            Log.d(TAG, "AED selected, selectedCurrency set to: $selectedCurrency")
        }

        buttonInr.setOnClickListener {
            selectedCurrency = "INR"
            enableSubButtons(buttonIncome, buttonExpense, buttonInvestment, buttonGoal)
            buttonInr.text = "INR  ✔"
            buttonAed.text = "AED"
            Log.d(TAG, "INR selected, selectedCurrency set to: $selectedCurrency")
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
            Log.d(TAG, "Income List clicked, selectedCurrency: $selectedCurrency")
            selectedCurrency?.let { currency ->
                navigateToReportList(currency, "income")
            } ?: showCurrencyNotSelectedMessage()
        }

        buttonExpense.setOnClickListener {
            Log.d(TAG, "Expense List clicked, selectedCurrency: $selectedCurrency")
            selectedCurrency?.let { currency ->
                navigateToReportList(currency, "expense")
            } ?: showCurrencyNotSelectedMessage()
        }

        buttonInvestment.setOnClickListener {
            Log.d(TAG, "Investment List clicked, selectedCurrency: $selectedCurrency")
            selectedCurrency?.let { currency ->
                navigateToReportList(currency, "investment")
            } ?: showCurrencyNotSelectedMessage()
        }

        buttonGoal.setOnClickListener {
            Log.d(TAG, "Goal List clicked, selectedCurrency: $selectedCurrency")
            selectedCurrency?.let { currency ->
                navigateToReportList(currency, "goal")
            } ?: showCurrencyNotSelectedMessage()
        }

        buttonExportIncome.setOnClickListener {
            exportIncomeListAsCSV()
        }
        buttonExportExpense.setOnClickListener {
            exportExpenseListAsCSV()
        }
        buttonExportInvestment.setOnClickListener {
            exportInvestmentListAsCSV()
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

    private fun exportIncomeListAsCSV() {
        try {
            val incomeDao = IncomeDao(this)
            val incomes = incomeDao.getIncomesByCurrency("INR") + incomeDao.getIncomesByCurrency("AED")
            val file = File(cacheDir, "income_list.csv")
            val writer = OutputStreamWriter(FileOutputStream(file))
            writer.write("ID,Currency,Category,Value,Comment,Date\n")
            for (income in incomes) {
                writer.write("${income.id},${income.currency},${income.category},${income.value},${income.comment ?: ""},${income.date}\n")
            }
            writer.close()
            shareCSVFile(file, "Income List exported successfully!")
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to export income list: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportExpenseListAsCSV() {
        try {
            val expenseDao = ExpenseDao(this)
            val expenses = expenseDao.getExpensesByCurrency("INR") + expenseDao.getExpensesByCurrency("AED")
            val file = File(cacheDir, "expense_list.csv")
            val writer = OutputStreamWriter(FileOutputStream(file))
            writer.write("ID,Currency,Category,Value,Comment,Date\n")
            for (expense in expenses) {
                writer.write("${expense.id},${expense.currency},${expense.category},${expense.value},${expense.comment ?: ""},${expense.date}\n")
            }
            writer.close()
            shareCSVFile(file, "Expense List exported successfully!")
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to export expense list: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportInvestmentListAsCSV() {
        try {
            val investmentDao = InvestmentDao(this)
            val investments = investmentDao.getInvestmentsByCurrency("INR") + investmentDao.getInvestmentsByCurrency("AED")
            val file = File(cacheDir, "investment_list.csv")
            val writer = OutputStreamWriter(FileOutputStream(file))
            writer.write("ID,Currency,Category,Value,Comment,Date,GoalId,GoalName\n")
            for (investment in investments) {
                writer.write("${investment.id},${investment.currency},${investment.category},${investment.value},${investment.comment ?: ""},${investment.date},${investment.goalId ?: ""},${investment.goalName ?: ""}\n")
            }
            writer.close()
            shareCSVFile(file, "Investment List exported successfully!")
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to export investment list: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareCSVFile(file: File, successMessage: String) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/csv"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Share CSV"))
        Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
    }

    private fun Button.setStyle(styleRes: Int) {
        val typedArray = context.obtainStyledAttributes(styleRes, intArrayOf(
            android.R.attr.background,
            android.R.attr.textColor
        ))
        try {
            background = typedArray.getDrawable(0)
            setTextColor(typedArray.getColor(1, currentTextColor))
        } finally {
            typedArray.recycle()
        }
    }
}
