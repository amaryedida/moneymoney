package com.money.moneymoney

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseListActivity : AppCompatActivity(), ExpenseListAdapter.OnItemActionListener {

    private lateinit var recyclerViewExpenses: RecyclerView
    private lateinit var expenseAdapter: ExpenseListAdapter
    private lateinit var expenseDao: ExpenseDao
    private lateinit var editTextStartDate: EditText
    private lateinit var editTextEndDate: EditText
    private lateinit var buttonFilter: Button
    private lateinit var buttonClearFilter: Button
    private var selectedCurrency: String? = null

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_list)

        recyclerViewExpenses = findViewById(R.id.recycler_view_expenses)
        editTextStartDate = findViewById(R.id.edit_text_start_date)
        editTextEndDate = findViewById(R.id.edit_text_end_date)
        buttonFilter = findViewById(R.id.button_filter)
        buttonClearFilter = findViewById(R.id.button_clear_filter)

        selectedCurrency = intent.getStringExtra("EXTRA_CURRENCY")
        if (selectedCurrency == null) {
            Log.e("ExpenseListActivity", "Currency not provided")
            Toast.makeText(this, "Currency not selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        expenseDao = ExpenseDao(this)
        expenseAdapter = ExpenseListAdapter(emptyList(), this) // Pass 'this' as the listener
        recyclerViewExpenses.layoutManager = LinearLayoutManager(this)
        recyclerViewExpenses.adapter = expenseAdapter

        loadExpenses()

        editTextStartDate.setOnClickListener {
            showDatePickerDialog(editTextStartDate)
        }

        editTextEndDate.setOnClickListener {
            showDatePickerDialog(editTextEndDate)
        }

        buttonFilter.setOnClickListener {
            filterExpenses()
        }

        buttonClearFilter.setOnClickListener {
            clearFilters()
        }

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    navigateToDashboard()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadExpenses() {
        val expenses = expenseDao.getExpensesByCurrency(selectedCurrency!!)
        expenseAdapter.updateData(expenses)
    }

    private fun filterExpenses() {
        val startDate = getDateFromEditText(editTextStartDate)
        val endDate = getDateFromEditText(editTextEndDate)

        if (startDate != null && endDate != null && startDate.after(endDate)) {
            Toast.makeText(this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show()
            return
        }

        val expenses = expenseDao.getExpensesByCurrencyAndDateRange(
            selectedCurrency!!,
            startDate?.time,
            endDate?.time
        )
        expenseAdapter.updateData(expenses)
    }

    private fun clearFilters() {
        editTextStartDate.text = null
        editTextEndDate.text = null
        loadExpenses()
    }

    private fun getDateFromEditText(editText: EditText): Date? {
        val dateString = editText.text.toString()
        return if (dateString.isNotEmpty()) {
            try {
                dateFormatter.parse(dateString)
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
                null
            }
        } else {
            null
        }
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val selectedDate = calendar.time
                editText.setText(dateFormatter.format(selectedDate))
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        expenseDao.close()
    }

    override fun onEditItem(expense: ExpenseObject) {
        val intent = Intent(this, ExpenseEntryActivity::class.java)
        intent.putExtra("EXTRA_EXPENSE", expense)
        startActivityForResult(intent, EDIT_EXPENSE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_EXPENSE_REQUEST_CODE && resultCode == RESULT_OK) {
            loadExpenses() // Refresh the list after successful edit
        }
    }

    override fun onDeleteItem(expense: ExpenseObject) {
        expenseDao.deleteExpense(expense)
        loadExpenses()
        Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val EDIT_EXPENSE_REQUEST_CODE = 1
    }
}
