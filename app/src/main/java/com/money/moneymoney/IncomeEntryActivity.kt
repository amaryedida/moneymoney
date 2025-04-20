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

class IncomeEntryActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "IncomeEntryActivity"
    }

    private lateinit var editTextIncomeDate: EditText
    private lateinit var editTextIncomeValue: EditText
    private lateinit var spinnerIncomeCurrency: Spinner
    private lateinit var spinnerIncomeCategory: Spinner
    private lateinit var editTextIncomeComment: EditText
    private lateinit var buttonSaveIncome: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var textViewPreviousIncome: TextView
    private lateinit var recyclerViewPreviousIncome: RecyclerView
    private lateinit var incomeDao: IncomeDao
    private lateinit var previousIncomeAdapter: PreviousIncomeAdapter
    private var selectedDateInMillis: Long = Calendar.getInstance().timeInMillis

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")
        setContentView(R.layout.activity_income_entry)

        // Initialize UI elements
        editTextIncomeDate = findViewById(R.id.editTextIncomeDate)
        editTextIncomeValue = findViewById(R.id.editTextIncomeValue)
        spinnerIncomeCurrency = findViewById(R.id.spinnerIncomeCurrency)
        spinnerIncomeCategory = findViewById(R.id.spinnerIncomeCategory)
        editTextIncomeComment = findViewById(R.id.editTextIncomeComment)
        buttonSaveIncome = findViewById(R.id.buttonSaveIncome)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        textViewPreviousIncome = findViewById(R.id.textViewPreviousIncome)
        recyclerViewPreviousIncome = findViewById(R.id.recyclerViewPreviousIncome)

        // Initialize IncomeDao
        incomeDao = IncomeDao(this)

        // Set up Date Picker
        editTextIncomeDate.setOnClickListener {
            showDatePickerDialog()
        }

        // Set up Save Button Click Listener
        buttonSaveIncome.setOnClickListener {
            saveIncomeData()
        }

        // Set up bottom navigation
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menu_home -> {
                    Log.d(TAG, "Home navigation selected, returning to DashboardActivity")
                    startActivity(Intent(this, DashboardActivity::class.java))
                    true
                }
                else -> false
            }
        }
        
        // Set up RecyclerView for previous incomes
        recyclerViewPreviousIncome.layoutManager = LinearLayoutManager(this)
        previousIncomeAdapter = PreviousIncomeAdapter(emptyList())
        recyclerViewPreviousIncome.adapter = previousIncomeAdapter

        // Load initial previous incomes
        loadPreviousIncomes()

        // Initialize Date Field with Current Date
        updateDateEditText()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart() called")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() called")
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
        editTextIncomeDate.setText(formattedDate)
    }

    private fun saveIncomeData() {
        val currency = spinnerIncomeCurrency.selectedItem.toString()
        val category = spinnerIncomeCategory.selectedItem.toString()
        val valueStr = editTextIncomeValue.text.toString()
        val comment = editTextIncomeComment.text.toString()

        if (valueStr.isNotEmpty()) {
            val value = valueStr.toDouble()
            val insertedRowId = incomeDao.addIncome(currency, category, value, comment, selectedDateInMillis)

            if (insertedRowId > 0) {
                Toast.makeText(this, "Income data saved successfully", Toast.LENGTH_SHORT).show()
                // Clear input fields after saving
                editTextIncomeValue.text.clear()
                editTextIncomeComment.text.clear()
                // Keep the selected date
                loadPreviousIncomes() // Reload the list after saving
            } else {
                Toast.makeText(this, "Failed to save income data", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please enter the income value", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPreviousIncomes() {
        val lastTenIncomes = incomeDao.getLastTenIncomes()
        previousIncomeAdapter.updateData(lastTenIncomes)
    }

    override fun onDestroy() {
        super.onDestroy()
        incomeDao.close() // Close the database connection when the activity is destroyed
    }
}
