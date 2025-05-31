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
import android.app.DatePickerDialog
import android.app.Dialog
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class CurrencySelectionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CURRENCY = "selected_currency"
    }

    private var selectedCurrency: String? = null
    private val TAG = "CurrencySelectionActivity"
    private var fromDate: Calendar = Calendar.getInstance()
    private var toDate: Calendar = Calendar.getInstance()
    private var currentExportType: String = ""

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
            Log.d(TAG, "Navigating to income list with currency: $selectedCurrency")
        }

        buttonInr.setOnClickListener {
            selectedCurrency = "INR"
            enableSubButtons(buttonIncome, buttonExpense, buttonInvestment, buttonGoal)
            buttonInr.text = "INR  ✔"
            buttonAed.text = "AED"
            Log.d(TAG, "INR selected, selectedCurrency set to: $selectedCurrency")
            Log.d(TAG, "Navigating to income list with currency: $selectedCurrency")
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
            showDateRangeDialog("income")
        }
        buttonExportExpense.setOnClickListener {
            showDateRangeDialog("expense")
        }
        buttonExportInvestment.setOnClickListener {
            showDateRangeDialog("investment")
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

    private fun showDateRangeDialog(exportType: String) {
        currentExportType = exportType
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.export_date_range)
        dialog.setTitle("Select Date Range")

        val fromDateText = dialog.findViewById<TextView>(R.id.fromDateText)
        val toDateText = dialog.findViewById<TextView>(R.id.toDateText)
        val exportButton = dialog.findViewById<Button>(R.id.exportButton)

        // Initially disable export button
        exportButton.isEnabled = false
        fromDateText.text = "Select From Date"
        toDateText.text = "Select To Date"

        fromDateText.setOnClickListener {
            showDatePicker(fromDate) { calendar ->
                fromDate = calendar
                updateDateText(fromDateText, calendar)
                validateAndEnableExport(exportButton, fromDateText, toDateText)
            }
        }

        toDateText.setOnClickListener {
            showDatePicker(toDate) { calendar ->
                toDate = calendar
                updateDateText(toDateText, calendar)
                validateAndEnableExport(exportButton, fromDateText, toDateText)
            }
        }

        exportButton.setOnClickListener {
            if (validateDateRange(fromDate, toDate)) {
                when (exportType) {
                    "income" -> exportIncomeListAsCSV()
                    "expense" -> exportExpenseListAsCSV()
                    "investment" -> exportInvestmentListAsCSV()
                }
                dialog.dismiss()
            } else {
                Toast.makeText(this, "To date must be after from date", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun validateAndEnableExport(exportButton: Button, fromDateText: TextView, toDateText: TextView) {
        val isFromDateSelected = fromDateText.text != "Select From Date"
        val isToDateSelected = toDateText.text != "Select To Date"
        exportButton.isEnabled = isFromDateSelected && isToDateSelected
    }

    private fun validateDateRange(fromDate: Calendar, toDate: Calendar): Boolean {
        return !toDate.before(fromDate)
    }

    private fun showDatePicker(calendar: Calendar, onDateSelected: (Calendar) -> Unit) {
        DatePickerDialog(
            this,
            { _: DatePicker, year: Int, month: Int, day: Int ->
                calendar.set(year, month, day)
                onDateSelected(calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateText(textView: TextView, calendar: Calendar) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        textView.text = dateFormat.format(calendar.time)
    }

    private fun isDateInRange(date: String, fromDate: Calendar, toDate: Calendar): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val transactionDate = dateFormat.parse(date)
        return transactionDate != null && 
               !transactionDate.before(fromDate.time) && 
               !transactionDate.after(toDate.time)
    }

    private fun exportIncomeListAsCSV() {
        try {
            val incomeDao = IncomeDao(this)
            val incomes = incomeDao.getIncomesByCurrency("INR") + incomeDao.getIncomesByCurrency("AED")
            val filteredIncomes = incomes.filter { isDateInRange(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date)), fromDate, toDate) }
            
            val file = File(cacheDir, "income_list.csv")
            val writer = OutputStreamWriter(FileOutputStream(file))
            writer.write("ID,Currency,Category,Value,Comment,Date\n")
            for (income in filteredIncomes) {
                writer.write("${income.id.toString()},${income.currency},${income.category},\"${income.value.toString()}\",${income.comment ?: ""},${income.date}\n")
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
            val filteredExpenses = expenses.filter { isDateInRange(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date)), fromDate, toDate) }
            
            val file = File(cacheDir, "expense_list.csv")
            val writer = OutputStreamWriter(FileOutputStream(file))
            writer.write("ID,Currency,Category,Value,Comment,Date\n")
            for (expense in filteredExpenses) {
                writer.write("${expense.id.toString()},${expense.currency},${expense.category},\"${expense.value.toString()}\",${expense.comment ?: ""},${expense.date}\n")
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
            val filteredInvestments = investments.filter { isDateInRange(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date)), fromDate, toDate) }
            
            val file = File(cacheDir, "investment_list.csv")
            val writer = OutputStreamWriter(FileOutputStream(file))
            writer.write("ID,Currency,Category,Value,Comment,Date,GoalId,GoalName\n")
            for (investment in filteredInvestments) {
                writer.write("${investment.id.toString()},${investment.currency},${investment.category},\"${investment.value.toString()}\",${investment.comment ?: ""},${investment.date},${investment.goalId?.toString() ?: ""},${investment.goalName ?: ""}\n")
            }
            writer.close()
            shareCSVFile(file, "Investment List exported successfully!")
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to export investment list: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareCSVFile(file: File, successMessage: String) {
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/csv"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Share CSV"))
        Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
    }
}
