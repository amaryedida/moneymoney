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
import androidx.activity.result.contract.ActivityResultContracts

class InvestmentListActivity : AppCompatActivity(), InvestmentListAdapter.OnItemActionListener {
    companion object {
        private const val TAG = "InvestmentListActivity"
    }

    private lateinit var recyclerViewInvestments: RecyclerView
    private lateinit var investmentAdapter: InvestmentListAdapter
    private lateinit var investmentDao: InvestmentDao
    private lateinit var editTextStartDate: EditText
    private lateinit var editTextEndDate: EditText
    private lateinit var buttonFilter: Button
    private lateinit var buttonClearFilter: Button
    private var selectedCurrency: String? = null

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Define the ActivityResultLauncher for editing investments
    private val editInvestmentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // If the edit was successful, reload the investments
            Log.d(TAG, "Investment edit successful, reloading investments")
            loadInvestments()
        } else {
            Log.d(TAG, "Investment edit cancelled or failed")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_investment_list)

        initializeViews()
        setupCurrency()
        setupInvestmentList()
        setupListeners()
        setupBottomNavigation()
    }

    private fun initializeViews() {
        recyclerViewInvestments = findViewById(R.id.recycler_view_investments)
        editTextStartDate = findViewById(R.id.edit_text_start_date)
        editTextEndDate = findViewById(R.id.edit_text_end_date)
        buttonFilter = findViewById(R.id.button_filter)
        buttonClearFilter = findViewById(R.id.button_clear_filter)
    }

    private fun setupCurrency() {
        selectedCurrency = intent.getStringExtra(CurrencySelectionActivity.EXTRA_CURRENCY)
        Log.d(TAG, "Received currency in setupCurrency: $selectedCurrency")
        if (selectedCurrency == null) {
            Log.e(TAG, "Currency not provided")
            Toast.makeText(this, "Currency not selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    private fun setupInvestmentList() {
        investmentDao = InvestmentDao(this)
        investmentAdapter = InvestmentListAdapter(emptyList(), this)
        recyclerViewInvestments.apply {
            layoutManager = LinearLayoutManager(this@InvestmentListActivity)
            adapter = investmentAdapter
        }
        loadInvestments()
    }

    private fun setupListeners() {
        editTextStartDate.setOnClickListener {
            showDatePickerDialog(editTextStartDate)
        }

        editTextEndDate.setOnClickListener {
            showDatePickerDialog(editTextEndDate)
        }

        buttonFilter.setOnClickListener {
            filterInvestments()
        }

        buttonClearFilter.setOnClickListener {
            clearFilter()
        }

        // Add long press on RecyclerView to add new investment
        recyclerViewInvestments.setOnLongClickListener {
            addNewInvestment()
            true
        }
    }

    private fun setupBottomNavigation() {
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

    private fun loadInvestments() {
        try {
            val investments = investmentDao.getInvestmentsByCurrency(selectedCurrency!!)
            Log.d(TAG, "Loaded ${investments.size} investments for currency: $selectedCurrency")
            investmentAdapter.updateData(investments)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading investments", e)
            Toast.makeText(this, "Error loading investments: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun filterInvestments() {
        val startDate = getDateFromEditText(editTextStartDate)
        val endDate = getDateFromEditText(editTextEndDate)

        if (startDate != null && endDate != null && startDate.after(endDate)) {
            Toast.makeText(this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val investments = investmentDao.getInvestmentsByCurrencyAndDateRange(
                selectedCurrency!!,
                startDate?.time,
                endDate?.time
            )
            investmentAdapter.updateData(investments)
        } catch (e: Exception) {
            Log.e(TAG, "Error filtering investments", e)
            Toast.makeText(this, "Error filtering investments: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearFilter() {
        editTextStartDate.text.clear()
        editTextEndDate.text.clear()
        loadInvestments()
    }

    private fun getDateFromEditText(editText: EditText): Date? {
        val dateString = editText.text.toString()
        return if (dateString.isNotEmpty()) {
            try {
                dateFormatter.parse(dateString)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing date", e)
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

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val selectedDate = calendar.time
                editText.setText(dateFormatter.format(selectedDate))
            },
            year,
            month,
            day
        ).show()
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            investmentDao.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing database", e)
        }
    }

    override fun onEditItem(investment: InvestmentObject) {
        try {
            val intent = Intent(this, InvestmentEntryActivity::class.java)
            intent.putExtra("EXTRA_INVESTMENT", investment)
            // Use the launcher to start the activity for result
            editInvestmentLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting InvestmentEntryActivity for edit", e)
            Toast.makeText(this, "Error opening investment for editing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDeleteItem(investment: InvestmentObject) {
        try {
            val result = investmentDao.deleteInvestment(investment)
            if (result > 0) {
                loadInvestments()
                Toast.makeText(this, "Investment deleted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to delete investment", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting investment", e)
            Toast.makeText(this, "Error deleting investment: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addNewInvestment() {
        try {
            val intent = Intent(this, InvestmentEntryActivity::class.java)
            intent.putExtra(CurrencySelectionActivity.EXTRA_CURRENCY, selectedCurrency)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting InvestmentEntryActivity", e)
            Toast.makeText(this, "Error adding investment: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}