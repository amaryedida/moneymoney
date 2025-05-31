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
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

class CurrencySelectionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CURRENCY = "selected_currency"
        private const val TAG = "CurrencySelectionActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private var selectedCurrency: String? = null
    private var fromDate: Calendar = Calendar.getInstance()
    private var toDate: Calendar = Calendar.getInstance()
    private var currentExportType: String = ""
    private var pendingExportAction: (() -> Unit)? = null

    private fun checkAndRequestPermissions(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            
            val permissionsToRequest = permissions.filter {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()
            
            if (permissionsToRequest.isNotEmpty()) {
                Log.d(TAG, "Permissions to request: ${permissionsToRequest.joinToString()}")

                // Check if we should show a rationale (user previously denied without "don't ask again")
                val showRationale = permissionsToRequest.any {
                    shouldShowRequestPermissionRationale(it)
                }

                if (showRationale) {
                    // Show explanation before requesting again
                    Toast.makeText(this, "Storage permissions are needed to export data.", Toast.LENGTH_LONG).show()
                }

                pendingExportAction = onGranted
                requestPermissions(permissionsToRequest, PERMISSION_REQUEST_CODE)
            } else {
                Log.d(TAG, "Storage permissions already granted")
                onGranted()
            }
        } else {
            Log.d(TAG, "Android version < M, no need for runtime permissions")
            onGranted()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d(TAG, "Storage permissions granted")
                Toast.makeText(this, "Storage permissions granted", Toast.LENGTH_SHORT).show()
                pendingExportAction?.invoke()
            } else {
                Log.d(TAG, "Storage permissions denied")
                // Just show a toast on denial, do not navigate to settings here
                Toast.makeText(this, "Storage permission denied. Cannot export data.", Toast.LENGTH_LONG).show()
            }
            pendingExportAction = null
        }
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
            Log.d(TAG, "Export Income button clicked")
            checkAndRequestPermissions { showDateRangeDialog("income") }
        }
        buttonExportExpense.setOnClickListener {
            Log.d(TAG, "Export Expense button clicked")
            checkAndRequestPermissions { showDateRangeDialog("expense") }
        }
        buttonExportInvestment.setOnClickListener {
            Log.d(TAG, "Export Investment button clicked")
            checkAndRequestPermissions { showDateRangeDialog("investment") }
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
        Log.d(TAG, "Showing date range dialog for export type: $exportType")
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
            Log.d(TAG, "From date text clicked")
            showDatePicker(fromDate) { calendar ->
                fromDate = calendar
                updateDateText(fromDateText, calendar)
                validateAndEnableExport(exportButton, fromDateText, toDateText)
            }
        }

        toDateText.setOnClickListener {
            Log.d(TAG, "To date text clicked")
            showDatePicker(toDate) { calendar ->
                toDate = calendar
                updateDateText(toDateText, calendar)
                validateAndEnableExport(exportButton, fromDateText, toDateText)
            }
        }

        exportButton.setOnClickListener {
            Log.d(TAG, "Export button clicked in dialog")
            if (validateDateRange(fromDate, toDate)) {
                Log.d(TAG, "Date range validated, proceeding with export")
                when (exportType) {
                    "income" -> exportIncomeListAsCSV()
                    "expense" -> exportExpenseListAsCSV()
                    "investment" -> exportInvestmentListAsCSV()
                }
                dialog.dismiss()
            } else {
                Log.d(TAG, "Invalid date range")
                Toast.makeText(this, "To date must be after from date", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
        Log.d(TAG, "Date range dialog shown")
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
            Log.d(TAG, "Starting income export...")
            val incomeDao = IncomeDao(this)
            val incomes = incomeDao.getIncomesByCurrency("INR") + incomeDao.getIncomesByCurrency("AED")
            Log.d(TAG, "Retrieved ${incomes.size} incomes")
            
            val filteredIncomes = incomes.filter { isDateInRange(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date)), fromDate, toDate) }
            Log.d(TAG, "Filtered to ${filteredIncomes.size} incomes")
            
            val file = File(cacheDir, "income_list.csv")
            Log.d(TAG, "Creating file at: ${file.absolutePath}")
            
            val writer = OutputStreamWriter(FileOutputStream(file))
            writer.write("ID,Currency,Category,Value,Comment,Date\n")
            for (income in filteredIncomes) {
                writer.write("${income.id.toString()},${income.currency},${income.category},\"${income.value.toString()}\",${income.comment ?: ""},${income.date}\n")
            }
            writer.close()
            Log.d(TAG, "File written successfully")
            
            shareCSVFile(file, "Income List exported successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export income list", e)
            Toast.makeText(this, "Failed to export income list: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportExpenseListAsCSV() {
        try {
            Log.d(TAG, "Starting expense export...")
            val expenseDao = ExpenseDao(this)
            val expenses = expenseDao.getExpensesByCurrency("INR") + expenseDao.getExpensesByCurrency("AED")
            Log.d(TAG, "Retrieved ${expenses.size} expenses")
            
            val filteredExpenses = expenses.filter { isDateInRange(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date)), fromDate, toDate) }
            Log.d(TAG, "Filtered to ${filteredExpenses.size} expenses")
            
            val file = File(cacheDir, "expense_list.csv")
            Log.d(TAG, "Creating file at: ${file.absolutePath}")
            
            val writer = OutputStreamWriter(FileOutputStream(file))
            writer.write("ID,Currency,Category,Value,Comment,Date\n")
            for (expense in filteredExpenses) {
                writer.write("${expense.id.toString()},${expense.currency},${expense.category},\"${expense.value.toString()}\",${expense.comment ?: ""},${expense.date}\n")
            }
            writer.close()
            Log.d(TAG, "File written successfully")
            
            shareCSVFile(file, "Expense List exported successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export expense list", e)
            Toast.makeText(this, "Failed to export expense list: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportInvestmentListAsCSV() {
        try {
            Log.d(TAG, "Starting investment export...")
            val investmentDao = InvestmentDao(this)
            val investments = investmentDao.getInvestmentsByCurrency("INR") + investmentDao.getInvestmentsByCurrency("AED")
            Log.d(TAG, "Retrieved ${investments.size} investments")
            
            val filteredInvestments = investments.filter { isDateInRange(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date)), fromDate, toDate) }
            Log.d(TAG, "Filtered to ${filteredInvestments.size} investments")
            
            val file = File(cacheDir, "investment_list.csv")
            Log.d(TAG, "Creating file at: ${file.absolutePath}")
            
            val writer = OutputStreamWriter(FileOutputStream(file))
            writer.write("ID,Currency,Category,Value,Comment,Date,GoalId,GoalName\n")
            for (investment in filteredInvestments) {
                writer.write("${investment.id.toString()},${investment.currency},${investment.category},\"${investment.value.toString()}\",${investment.comment ?: ""},${investment.date},${investment.goalId?.toString() ?: ""},${investment.goalName ?: ""}\n")
            }
            writer.close()
            Log.d(TAG, "File written successfully")
            
            shareCSVFile(file, "Investment List exported successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export investment list", e)
            Toast.makeText(this, "Failed to export investment list: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareCSVFile(file: File, successMessage: String) {
        try {
            Log.d(TAG, "Starting file share...")
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            Log.d(TAG, "File URI created: $uri")
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            Log.d(TAG, "Starting share chooser...")
            startActivity(Intent.createChooser(intent, "Share CSV"))
            Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share file", e)
            Toast.makeText(this, "Failed to share file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
