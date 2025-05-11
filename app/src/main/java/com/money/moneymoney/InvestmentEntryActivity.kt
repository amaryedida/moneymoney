    package com.money.moneymoney

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
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
import java.util.Date
import java.text.SimpleDateFormat

class InvestmentEntryActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "InvestmentEntryActivity"           
    }

    private lateinit var editTextInvestmentDate: EditText
    private lateinit var editTextInvestmentValue: EditText
    private lateinit var spinnerInvestmentCurrency: Spinner
    private lateinit var spinnerInvestmentCategory: Spinner
    private lateinit var spinnerInvestmentGoal: Spinner
    private lateinit var editTextInvestmentComment: EditText
    private lateinit var buttonSaveInvestment: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var textViewPreviousInvestments: TextView
    private lateinit var recyclerViewPreviousInvestments: RecyclerView
    private lateinit var investmentDao: InvestmentDao
    private lateinit var goalDao: GoalDao // To fetch goals
    private lateinit var previousInvestmentAdapter: PreviousInvestmentAdapter
    private var selectedDateInMillis: Long = Calendar.getInstance().timeInMillis
    private var goalList: MutableList<String> = mutableListOf("None") // Default "None" option
    private var goalIdMap: MutableMap<String, Long?> = mutableMapOf("None" to null)
    private var editingInvestment: InvestmentObject? = null

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")
        setContentView(R.layout.activity_investment_entry)

        editTextInvestmentDate = findViewById(R.id.editTextInvestmentDate)
        editTextInvestmentValue = findViewById(R.id.editTextInvestmentValue)
        spinnerInvestmentCurrency = findViewById(R.id.spinnerInvestmentCurrency)
        spinnerInvestmentCategory = findViewById(R.id.spinnerInvestmentCategory)
        spinnerInvestmentGoal = findViewById(R.id.spinnerInvestmentGoal)
        editTextInvestmentComment = findViewById(R.id.editTextInvestmentComment)
        buttonSaveInvestment = findViewById(R.id.buttonSaveInvestment)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        textViewPreviousInvestments = findViewById(R.id.textViewPreviousInvestments)
        recyclerViewPreviousInvestments = findViewById(R.id.recyclerViewPreviousInvestments)

        investmentDao = InvestmentDao(this)
        goalDao = GoalDao(this)

        // Set up RecyclerView
        recyclerViewPreviousInvestments.layoutManager = LinearLayoutManager(this)
        previousInvestmentAdapter = PreviousInvestmentAdapter(emptyList(), goalDao)
        recyclerViewPreviousInvestments.adapter = previousInvestmentAdapter

        loadGoals() // Load goals from the database
        loadPreviousInvestments() // Load previous investments

        editTextInvestmentDate.setOnClickListener {
            showDatePickerDialog()
        }

        buttonSaveInvestment.setOnClickListener {
            saveInvestmentData()
        }

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
        // Don't set selectedItemId to avoid automatic navigation

        updateDateEditText()

        // Check if editing an existing investment
        editingInvestment = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("EXTRA_INVESTMENT", InvestmentObject::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("EXTRA_INVESTMENT")
        }
        if (editingInvestment != null) {
            populateFields(editingInvestment!!)
            buttonSaveInvestment.text = "Update Investment"
        }
    }

    private fun loadGoals() {
        val activeGoals = goalDao.getAllActiveGoals()
        goalList.clear()
        goalList.add("None")
        goalIdMap.clear()
        goalIdMap["None"] = null

        for (goal in activeGoals) {
            goalList.add(goal.name)
            goalIdMap[goal.name] = goal.id
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, goalList)
        spinnerInvestmentGoal.adapter = adapter
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
        editTextInvestmentDate.setText(formattedDate)
    }

    private fun populateFields(investment: InvestmentObject) {
        editTextInvestmentValue.setText(String.format(Locale.getDefault(), "%.2f", investment.value))
        editTextInvestmentComment.setText(investment.comment)
        selectedDateInMillis = investment.date
        editTextInvestmentDate.setText(dateFormatter.format(Date(selectedDateInMillis)))

        // Set category in spinner
        val categoryAdapter = spinnerInvestmentCategory.adapter
        if (categoryAdapter is ArrayAdapter<*>) {
            val position = (0 until categoryAdapter.count).firstOrNull { 
                categoryAdapter.getItem(it) == investment.category 
            } ?: 0
            spinnerInvestmentCategory.setSelection(position)
        }

        // Set currency in spinner
        val currencyAdapter = spinnerInvestmentCurrency.adapter
        if (currencyAdapter is ArrayAdapter<*>) {
            val currencyPosition = (0 until currencyAdapter.count).firstOrNull {
                currencyAdapter.getItem(it) == investment.currency
            } ?: 0
            spinnerInvestmentCurrency.setSelection(currencyPosition)
        }

        // Set goal in spinner
        val goalAdapter = spinnerInvestmentGoal.adapter
        if (goalAdapter is ArrayAdapter<*>) {
            val goalPosition = (0 until goalAdapter.count).firstOrNull {
                goalAdapter.getItem(it) == investment.goalName ?: "None"
            } ?: 0
            spinnerInvestmentGoal.setSelection(goalPosition)
        }
    }

    private fun saveInvestmentData() {
        val currency = spinnerInvestmentCurrency.selectedItem.toString()
        val category = spinnerInvestmentCategory.selectedItem.toString()
        val valueStr = editTextInvestmentValue.text.toString()
        val comment = editTextInvestmentComment.text.toString()
        val selectedGoalName = spinnerInvestmentGoal.selectedItem.toString()
        val goalId = goalIdMap[selectedGoalName]

        if (valueStr.isNotEmpty()) {
            val value = valueStr.toDouble()
            if (editingInvestment == null) {
                // Add new investment
                val insertedRowId = investmentDao.addInvestment(currency, category, value, comment, selectedDateInMillis, goalId)
                if (insertedRowId > 0) {
                    Toast.makeText(this, "Investment data saved successfully", Toast.LENGTH_SHORT).show()
                    editTextInvestmentValue.text.clear()
                    editTextInvestmentComment.text.clear()
                    loadPreviousInvestments()
                } else {
                    Toast.makeText(this, "Failed to save investment data", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Update existing investment
                val updatedInvestment = editingInvestment!!.copy(
                    currency = currency,
                    category = category,
                    value = value,
                    comment = comment,
                    date = selectedDateInMillis,
                    goalId = goalId,
                    goalName = if (selectedGoalName == "None") null else selectedGoalName
                )
                investmentDao.updateInvestment(updatedInvestment)
                Toast.makeText(this, "Investment updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Please enter the investment value", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPreviousInvestments() {
        val lastTenInvestments = investmentDao.getLastTenInvestments()
        previousInvestmentAdapter.updateData(lastTenInvestments)
    }

    override fun onDestroy() {
        super.onDestroy()
        investmentDao.close()
        goalDao.close()
    }
}