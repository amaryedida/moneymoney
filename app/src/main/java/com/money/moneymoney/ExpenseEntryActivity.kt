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
        Log.d(TAG, "ExpenseEntryActivity onCreate started")
        setContentView(R.layout.activity_expense_entry)
        Log.d(TAG, "Layout set to activity_expense_entry")

        try {
            editTextExpenseDate = findViewById(R.id.editTextExpenseDate)
            editTextExpenseValue = findViewById(R.id.editTextExpenseValue)
            spinnerExpenseCurrency = findViewById(R.id.spinnerExpenseCurrency)
            spinnerExpenseCategory = findViewById(R.id.spinnerExpenseCategory)
            editTextExpenseComment = findViewById(R.id.editTextExpenseComment)
            buttonSaveExpense = findViewById(R.id.buttonSaveExpense)
            textViewPreviousExpenses = findViewById(R.id.textViewPreviousExpenses)
            recyclerViewPreviousExpenses = findViewById(R.id.recyclerViewPreviousExpenses)
            bottomNavigation = findViewById(R.id.bottomNavigationView)
            Log.d(TAG, "All views found and initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error finding views", e)
            Toast.makeText(this, "Error initializing views: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            // Initialize DAO
            expenseDao = ExpenseDao(this)
            Log.d(TAG, "ExpenseDao initialized")

            // Setup spinners
            setupCategorySpinner()
            setupCurrencySpinner()
            Log.d(TAG, "Spinners setup complete")

            // Check if we're editing an existing expense
            editingExpense = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_EXPENSE, ExpenseObject::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_EXPENSE)
            }
            Log.d(TAG, "Editing expense check complete: ${editingExpense != null}")

            if (editingExpense != null) {
                populateFields(editingExpense!!)
                Log.d(TAG, "Fields populated with existing expense data")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in initialization", e)
            Toast.makeText(this, "Error in initialization: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Setup date button
        editTextExpenseDate.setText(dateFormatter.format(Date(selectedDateInMillis)))
        editTextExpenseDate.setOnClickListener {
            showDatePicker()
        }
        Log.d(TAG, "Date picker setup complete")

        // Setup save button
        buttonSaveExpense.setOnClickListener {
            saveExpense()
        }
        Log.d(TAG, "Save button setup complete")

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
        Log.d(TAG, "Bottom navigation setup complete")

        // Set up RecyclerView for previous expenses
        recyclerViewPreviousExpenses.layoutManager = LinearLayoutManager(this)
        previousExpensesAdapter = PreviousExpenseAdapter(emptyList())
        recyclerViewPreviousExpenses.adapter = previousExpensesAdapter
        Log.d(TAG, "RecyclerView setup complete")

        loadPreviousExpenses()
        updateDateEditText()
        Log.d(TAG, "ExpenseEntryActivity onCreate completed successfully")
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

    private fun setupCurrencySpinner() {
        val currencies = arrayOf("INR", "AED")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerExpenseCurrency.adapter = adapter
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
