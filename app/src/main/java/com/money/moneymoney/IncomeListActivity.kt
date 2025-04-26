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

class IncomeListActivity : AppCompatActivity(), IncomeListAdapter.OnItemActionListener {

    private lateinit var recyclerViewIncomes: RecyclerView
    private lateinit var incomeAdapter: IncomeListAdapter
    private lateinit var incomeDao: IncomeDao
    private lateinit var editTextStartDate: EditText
    private lateinit var editTextEndDate: EditText
    private lateinit var buttonFilter: Button
    private lateinit var buttonClearFilter: Button
    private var selectedCurrency: String? = null

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_income_list)

        recyclerViewIncomes = findViewById(R.id.recycler_view_incomes)
        editTextStartDate = findViewById(R.id.edit_text_start_date)
        editTextEndDate = findViewById(R.id.edit_text_end_date)
        buttonFilter = findViewById(R.id.button_filter)
        buttonClearFilter = findViewById(R.id.button_clear_filter)

        selectedCurrency = intent.getStringExtra("EXTRA_CURRENCY")
        if (selectedCurrency == null) {
            Log.e("IncomeListActivity", "Currency not provided")
            Toast.makeText(this, "Currency not selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        incomeDao = IncomeDao(this)
        incomeAdapter = IncomeListAdapter(emptyList(), this)
        recyclerViewIncomes.layoutManager = LinearLayoutManager(this)
        recyclerViewIncomes.adapter = incomeAdapter

        loadIncomes()

        editTextStartDate.setOnClickListener {
            showDatePickerDialog(editTextStartDate)
        }

        editTextEndDate.setOnClickListener {
            showDatePickerDialog(editTextEndDate)
        }

        buttonFilter.setOnClickListener {
            filterIncomes()
        }

        buttonClearFilter.setOnClickListener {
            clearFilter()
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

    private fun loadIncomes() {
        val incomes = incomeDao.getIncomesByCurrency(selectedCurrency!!)
        incomeAdapter.updateData(incomes)
    }

    private fun filterIncomes() {
        val startDate = getDateFromEditText(editTextStartDate)
        val endDate = getDateFromEditText(editTextEndDate)

        if (startDate != null && endDate != null && startDate.after(endDate)) {
            Toast.makeText(this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show()
            return
        }

        val incomes = incomeDao.getIncomesByCurrencyAndDateRange(
            selectedCurrency!!,
            startDate?.time,
            endDate?.time
        )
        incomeAdapter.updateData(incomes)
    }

    private fun clearFilter() {
        editTextStartDate.text = null
        editTextEndDate.text = null
        loadIncomes()
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
        incomeDao.close()
    }

    override fun onEditItem(income: IncomeObject) {
        val intent = Intent(this, IncomeEntryActivity::class.java)
        intent.putExtra("EXTRA_INCOME", income)
        startActivity(intent)
    }

    override fun onDeleteItem(income: IncomeObject) {
        incomeDao.deleteIncome(income)
        loadIncomes()
        Toast.makeText(this, "Income deleted", Toast.LENGTH_SHORT).show()
    }
}