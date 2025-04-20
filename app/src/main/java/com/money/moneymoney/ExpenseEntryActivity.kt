package com.money.moneymoney

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar
import java.util.Locale

class ExpenseEntryActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ExpenseEntryActivity"
    }

    private lateinit var editTextExpenseDate: EditText
    private lateinit var editTextExpenseValue: EditText
    private lateinit var spinnerExpenseCurrency: Spinner
    private lateinit var spinnerExpenseCategory: Spinner
    private lateinit var editTextExpenseComment: EditText
    private lateinit var buttonSaveExpense: Button
    private lateinit var textViewPreviousExpenses: TextView
    private lateinit var recyclerViewPreviousExpenses: RecyclerView
    private lateinit var expenseDao: ExpenseDao
    private lateinit var previousExpensesAdapter: PreviousExpenseAdapter
    private lateinit var bottomNavigation: BottomNavigationView
    private var selectedDateInMillis: Long = Calendar.getInstance().timeInMillis

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_entry)

        editTextExpenseDate = findViewById(R.id.editTextExpenseDate)
        editTextExpenseValue = findViewById(R.id.editTextExpenseValue)
        spinnerExpenseCurrency = findViewById(R.id.spinnerExpenseCurrency)
        spinnerExpenseCategory = findViewById(R.id.spinnerExpenseCategory)
        editTextExpenseComment = findViewById(R.id.editTextExpenseComment)
        buttonSaveExpense = findViewById(R.id.buttonSaveExpense)
        textViewPreviousExpenses = findViewById(R.id.textViewPreviousExpenses)
        recyclerViewPreviousExpenses = findViewById(R.id.recyclerViewPreviousExpenses)
        bottomNavigation = findViewById(R.id.bottomNavigationView)

        expenseDao = ExpenseDao(this)

        // Set up bottom navigation
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    Log.d(TAG, "Home navigation selected, returning to DashboardActivity")
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        // Set the current item to home
        bottomNavigation.selectedItemId = R.id.menu_home

        editTextExpenseDate.setOnClickListener {
            showDatePickerDialog()
        }

        buttonSaveExpense.setOnClickListener {
            saveExpenseData()
        }

        // Set up RecyclerView for previous expenses
        recyclerViewPreviousExpenses.layoutManager = LinearLayoutManager(this)
        previousExpensesAdapter = PreviousExpenseAdapter(emptyList())
        recyclerViewPreviousExpenses.adapter = previousExpensesAdapter

        loadPreviousExpenses()
        updateDateEditText()
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, yearSelected, monthOfYear, dayOfMonthSelected ->
                calendar.set(yearSelected, monthOfYear, dayOfMonthSelected)
                selectedDateInMillis = calendar.timeInMillis
                updateDateEditText()
            },
            year,
            month,
            dayOfMonth
        )
        datePickerDialog.show()
    }

    private fun updateDateEditText() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDateInMillis
        val formattedDate = String.format(Locale.getDefault(), "%d-%02d-%02d",
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)
        )
        editTextExpenseDate.setText(formattedDate)
    }

    private fun saveExpenseData() {
        val currency = spinnerExpenseCurrency.selectedItem.toString()
        val category = spinnerExpenseCategory.selectedItem.toString()
        val valueStr = editTextExpenseValue.text.toString()
        val comment = editTextExpenseComment.text.toString()
        if (valueStr.isNotEmpty()) {
            val value = valueStr.toDouble()
            val insertedRowId = expenseDao.addExpense(currency, category, value, comment, selectedDateInMillis)

            if (insertedRowId > 0) {
                Toast.makeText(this, "Expense data saved successfully", Toast.LENGTH_SHORT).show()
                editTextExpenseValue.text.clear()
                editTextExpenseComment.text.clear()
                loadPreviousExpenses()
            } else {
                Toast.makeText(this, "Failed to save expense data", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please enter the expense value", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPreviousExpenses() {
        val lastTenExpenses = expenseDao.getLastTenExpenses()
        previousExpensesAdapter.updateData(lastTenExpenses)
    }

    override fun onDestroy() {
        super.onDestroy()
        expenseDao.close()
    }
}
