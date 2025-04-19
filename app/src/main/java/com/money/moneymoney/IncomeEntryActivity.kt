package com.money.moneymoney

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
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

    private lateinit var editTextDate: EditText
    private lateinit var editTextValue: EditText
    private lateinit var spinnerCurrency: Spinner
    private lateinit var spinnerCategory: Spinner
    private lateinit var editTextComment: EditText
    private lateinit var buttonSaveIncome: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var textViewPreviousIncomes: TextView
    private lateinit var recyclerViewPreviousIncomes: RecyclerView
    private lateinit var incomeDao: IncomeDao
    private lateinit var previousIncomeAdapter: PreviousIncomeAdapter
    private var selectedDateInMillis: Long = Calendar.getInstance().timeInMillis

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_income_entry)

        // Initialize UI elements
        editTextDate = findViewById(R.id.editTextDate)
        editTextValue = findViewById(R.id.editTextValue)
        spinnerCurrency = findViewById(R.id.spinnerCurrency)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        editTextComment = findViewById(R.id.editTextComment)
        buttonSaveIncome = findViewById(R.id.buttonSaveIncome)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        textViewPreviousIncomes = findViewById(R.id.textViewPreviousIncomes)
        recyclerViewPreviousIncomes = findViewById(R.id.recyclerViewPreviousIncomes)

        // Initialize IncomeDao
        incomeDao = IncomeDao(this)

        // Set up Date Picker
        editTextDate.setOnClickListener {
            showDatePickerDialog()
        }

        // Set up Save Button Click Listener
        buttonSaveIncome.setOnClickListener {
            saveIncomeData()
        }

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.navigation_home

        // Set up RecyclerView for previous incomes
        recyclerViewPreviousIncomes.layoutManager = LinearLayoutManager(this)
        previousIncomeAdapter = PreviousIncomeAdapter(emptyList())
        recyclerViewPreviousIncomes.adapter = previousIncomeAdapter

        // Load initial previous incomes
        loadPreviousIncomes()

        // Initialize Date Field with Current Date
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
        editTextDate.setText(formattedDate)
    }

    private fun saveIncomeData() {
        val currency = spinnerCurrency.selectedItem.toString()
        val category = spinnerCategory.selectedItem.toString()
        val valueStr = editTextValue.text.toString()
        val comment = editTextComment.text.toString()

        if (valueStr.isNotEmpty()) {
            val value = valueStr.toDouble()
            val insertedRowId = incomeDao.addIncome(currency, category, value, comment, selectedDateInMillis)

            if (insertedRowId > 0) {
                Toast.makeText(this, "Income data saved successfully", Toast.LENGTH_SHORT).show()
                // Clear input fields after saving
                editTextValue.text.clear()
                editTextComment.text.clear()
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
