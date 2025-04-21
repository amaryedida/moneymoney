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

class InvestmentListActivity : AppCompatActivity(), InvestmentListAdapter.OnItemActionListener {

    private lateinit var recyclerViewInvestments: RecyclerView
    private lateinit var investmentAdapter: InvestmentListAdapter
    private lateinit var investmentDao: InvestmentDao
    private lateinit var editTextStartDate: EditText
    private lateinit var editTextEndDate: EditText
    private lateinit var buttonFilter: Button
    private lateinit var buttonClearFilter: Button
    private var selectedCurrency: String? = null

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_investment_list) // TODO: Create activity_investment_list.xml

        recyclerViewInvestments = findViewById(R.id.recycler_view_investments) // TODO: Ensure this ID exists in the layout
        editTextStartDate = findViewById(R.id.edit_text_start_date)
        editTextEndDate = findViewById(R.id.edit_text_end_date)
        buttonFilter = findViewById(R.id.button_filter)
        buttonClearFilter = findViewById(R.id.button_clear_filter)

        selectedCurrency = intent.getStringExtra(CurrencySelectionActivity.EXTRA_CURRENCY)
        if (selectedCurrency == null) {
            Log.e("InvestmentListActivity", "Currency not provided")
            Toast.makeText(this, "Currency not selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        investmentDao = InvestmentDao(this) // TODO: Create InvestmentDao
        investmentAdapter = InvestmentListAdapter(emptyList(), this) // TODO: Create InvestmentListAdapter and pass 'this' as the listener
        recyclerViewInvestments.layoutManager = LinearLayoutManager(this)
        recyclerViewInvestments.adapter = investmentAdapter

        loadInvestments()

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
        val investments = investmentDao.getInvestmentsByCurrency(selectedCurrency!!) // TODO: Implement getInvestmentsByCurrency in InvestmentDao
        investmentAdapter.updateData(investments)
    }

    private fun filterInvestments() {
        val startDate = getDateFromEditText(editTextStartDate)
        val endDate = getDateFromEditText(editTextEndDate)

        if (startDate != null && endDate != null && startDate.after(endDate)) {
            Toast.makeText(this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show()
            return
        }

        val investments = investmentDao.getInvestmentsByCurrencyAndDateRange( // TODO: Implement getInvestmentsByCurrencyAndDateRange in InvestmentDao
            selectedCurrency!!,
            startDate?.time,
            endDate?.time
        )
        investmentAdapter.updateData(investments)
    }

    private fun clearFilter() {
        editTextStartDate.text = null
        editTextEndDate.text = null
        loadInvestments()
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
        investmentDao.close()
    }

    override fun onEditItem(investment: Investment) {
        val intent = Intent(this, InvestmentEntryActivity::class.java)
        intent.putExtra("EXTRA_INVESTMENT", investment)
        startActivity(intent)
    }

    override fun onDeleteItem(investment: Investment) {
        investmentDao.deleteInvestment(investment) // TODO: Implement deleteInvestment in InvestmentDao
        loadInvestments()
        Toast.makeText(this, "Investment deleted", Toast.LENGTH_SHORT).show()
    }
}