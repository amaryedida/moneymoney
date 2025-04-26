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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager

class ExpenseEntryActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ExpenseEntryActivity"
        const val EXTRA_EXPENSE = "com.money.moneymoney.EXTRA_EXPENSE"
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
    private var editingExpense: ExpenseObject? = null

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

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

        // Initialize DAO
        expenseDao = ExpenseDao(this)

        // Check if we're editing an existing expense
        editingExpense = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_EXPENSE, ExpenseObject::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_EXPENSE)
        }

        if (editingExpense != null) {
            populateFields(editingExpense!!)
        }

        // Setup date button
        editTextExpenseDate.setText(dateFormatter.format(Date(selectedDateInMillis)))
        editTextExpenseDate.setOnClickListener {
            showDatePicker()
        }

        // Setup save button
        buttonSaveExpense.setOnClickListener {
            saveExpense()
        }

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

        // Set up RecyclerView for previous expenses
        recyclerViewPreviousExpenses.layoutManager = LinearLayoutManager(this)
        previousExpensesAdapter = PreviousExpenseAdapter(emptyList())
        recyclerViewPreviousExpenses.adapter = previousExpensesAdapter

        loadPreviousExpenses()
        updateDateEditText()
    }

    private fun setupCategorySpinner() {
        val categories = arrayOf(
            "Food",
            "Transportation",
            "Entertainment",
            "Shopping",
            "Bills",
            "Healthcare",
            "Education",
            "Other"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerExpenseCategory.adapter = adapter
    }

    private fun populateFields(expense: ExpenseObject) {
        editTextExpenseValue.setText(String.format(Locale.getDefault(), "%.2f", expense.value))
        editTextExpenseComment.setText(expense.comment)
        selectedDateInMillis = expense.date
        editTextExpenseDate.setText(dateFormatter.format(Date(selectedDateInMillis)))

        // Set category in spinner
        val categoryAdapter = spinnerExpenseCategory.adapter as? ArrayAdapter<String>
        if (categoryAdapter != null) {
            val position = (0 until categoryAdapter.count).firstOrNull { 
                categoryAdapter.getItem(it) == expense.category 
            } ?: 0
            spinnerExpenseCategory.setSelection(position)
        }

        // Set currency in spinner
        val currencyAdapter = spinnerExpenseCurrency.adapter as? ArrayAdapter<String>
        if (currencyAdapter != null) {
            val currencyPosition = (0 until currencyAdapter.count).firstOrNull {
                currencyAdapter.getItem(it) == expense.currency
            } ?: 0
            spinnerExpenseCurrency.setSelection(currencyPosition)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDateInMillis
        
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDateInMillis = calendar.timeInMillis
                editTextExpenseDate.setText(dateFormatter.format(Date(selectedDateInMillis)))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveExpense() {
        val valueStr = editTextExpenseValue.text.toString()
        if (valueStr.isEmpty()) {
            Toast.makeText(this, "Please enter a value", Toast.LENGTH_SHORT).show()
            return
        }

        val value = valueStr.toDoubleOrNull()
        if (value == null) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            return
        }

        val category = spinnerExpenseCategory.selectedItem.toString()
        val currency = spinnerExpenseCurrency.selectedItem.toString()
        val comment = editTextExpenseComment.text.toString()

        val expense = ExpenseObject(
            id = editingExpense?.id ?: 0,
            currency = currency,
            category = category,
            value = value,
            comment = if (comment.isEmpty()) null else comment,
            date = selectedDateInMillis
        )

        if (editingExpense == null) {
            expenseDao.addExpense(expense)
        } else {
            expenseDao.updateExpense(expense)
        }

        finish()
    }

    private fun loadPreviousExpenses() {
        val lastTenExpenses = expenseDao.getLastTenExpenses()
        previousExpensesAdapter.updateData(lastTenExpenses)
    }

    private fun updateDateEditText() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDateInMillis
        val formattedDate = String.format(Locale.getDefault(), "%d-%02d-%02d",
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)
        )
        editTextExpenseDate.setText(formattedDate)
    }

    override fun onDestroy() {
        super.onDestroy()
        expenseDao.close()
    }
}
