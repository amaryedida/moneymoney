package com.money.moneymoney

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar
import java.util.Locale
import android.widget.ArrayAdapter

class GoalEntryActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GoalEntryActivity"
        const val EXTRA_GOAL = "EXTRA_GOAL"
    }

    private lateinit var btnAddGoal: Button
    private lateinit var recyclerViewGoals: RecyclerView
    private lateinit var goalAdapter: GoalAdapter
    private lateinit var layoutAddGoalForm: LinearLayout
    private lateinit var editTextGoalName: EditText
    private lateinit var editTextTargetAmount: EditText
    private lateinit var editTextGoalCreationDate: EditText
    private lateinit var spinnerGoalCurrency: Spinner
    private lateinit var buttonSaveGoal: Button
    private lateinit var goalDao: GoalDao
    private lateinit var bottomNavigationView: BottomNavigationView
    private var selectedCreationDateInMillis: Long? = null
    private var currentGoal: GoalObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "GoalEntryActivity onCreate started")
        setContentView(R.layout.activity_goal_entry)
        Log.d(TAG, "Layout set to activity_goal_entry")

        try {
            recyclerViewGoals = findViewById(R.id.recyclerViewGoals)
            layoutAddGoalForm = findViewById(R.id.layoutAddGoalForm)
            editTextGoalName = findViewById(R.id.editTextGoalName)
            editTextTargetAmount = findViewById(R.id.editTextTargetAmount)
            editTextGoalCreationDate = findViewById(R.id.editTextGoalCreationDate)
            spinnerGoalCurrency = findViewById(R.id.spinnerGoalCurrency)
            buttonSaveGoal = findViewById(R.id.buttonSaveGoal)
            bottomNavigationView = findViewById(R.id.bottomNavigationView)
            Log.d(TAG, "All views found and initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error finding views", e)
            Toast.makeText(this, "Error initializing views: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            // Setup currency spinner
            setupCurrencySpinner()
            Log.d(TAG, "Currency spinner setup complete")

            goalDao = GoalDao(this)
            Log.d(TAG, "GoalDao initialized")

            goalAdapter = GoalAdapter(mutableListOf())
            recyclerViewGoals.layoutManager = LinearLayoutManager(this)
            recyclerViewGoals.adapter = goalAdapter
            Log.d(TAG, "RecyclerView setup complete")

            loadActiveGoals()
            Log.d(TAG, "Active goals loaded")

            // Check if we're editing an existing goal
            currentGoal = intent.getSerializableExtra(EXTRA_GOAL) as? GoalObject
            if (currentGoal != null) {
                populateGoalData()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in initialization", e)
            Toast.makeText(this, "Error in initialization: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Always show the form
        layoutAddGoalForm.visibility = View.VISIBLE

        editTextGoalCreationDate.setOnClickListener {
            Log.d(TAG, "Goal creation date field clicked")
            showDatePickerDialog()
        }

        // Set button text based on whether we're creating or updating
        buttonSaveGoal.text = if (currentGoal == null) "Save Goal" else "Update Goal"

        buttonSaveGoal.setOnClickListener {
            Log.d(TAG, "Save/Update Goal button clicked")
            if (currentGoal == null) {
                saveNewGoal()
            } else {
                updateExistingGoal()
            }
        }

        // Set up bottom navigation
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
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

        updateCreationDateEditText()
        Log.d(TAG, "GoalEntryActivity onCreate completed successfully")
    }

    private fun loadActiveGoals() {
        val activeGoals = goalDao.getAllActiveGoals()
        goalAdapter.updateGoals(activeGoals)
        layoutAddGoalForm.visibility = View.GONE
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
                selectedCreationDateInMillis = calendar.timeInMillis
                updateCreationDateEditText()
            },
            year,
            month,
            dayOfMonth
        )
        datePickerDialog.show()
    }

    private fun updateCreationDateEditText() {
        if (selectedCreationDateInMillis != null) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedCreationDateInMillis!!
            val formattedDate = String.format(Locale.getDefault(), "%d-%02d-%02d",
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)
            )
            editTextGoalCreationDate.setText(formattedDate)
        } else {
            editTextGoalCreationDate.setText("")
        }
    }

    private fun saveNewGoal() {
        val name = editTextGoalName.text.toString().trim()
        val targetValueStr = editTextTargetAmount.text.toString().trim()
        val currency = spinnerGoalCurrency.selectedItem.toString()
        val creationDateInMillis = selectedCreationDateInMillis

        if (name.isNotEmpty() && targetValueStr.isNotEmpty() && creationDateInMillis != null) {
            val targetValue = targetValueStr.toDouble()
            val insertedRowId = goalDao.addGoal(name, targetValue, creationDateInMillis, currency)

            if (insertedRowId > 0) {
                Toast.makeText(this, "Goal saved successfully", Toast.LENGTH_SHORT).show()
                editTextGoalName.text.clear()
                editTextTargetAmount.text.clear()
                selectedCreationDateInMillis = null
                updateCreationDateEditText()
                loadActiveGoals()
            } else {
                Toast.makeText(this, "Failed to save goal", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please enter goal name, target amount, and creation date", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateExistingGoal() {
        val name = editTextGoalName.text.toString().trim()
        val targetValueStr = editTextTargetAmount.text.toString().trim()
        val currency = spinnerGoalCurrency.selectedItem.toString()
        val creationDateInMillis = selectedCreationDateInMillis

        if (name.isNotEmpty() && targetValueStr.isNotEmpty() && creationDateInMillis != null) {
            val targetValue = targetValueStr.toDouble()
            val goal = currentGoal ?: return
            
            if (goalDao.updateGoal(goal.id, name, targetValue, creationDateInMillis, currency)) {
                Toast.makeText(this, "Goal updated successfully", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Failed to update goal", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please enter goal name, target amount, and creation date", Toast.LENGTH_SHORT).show()
        }
    }

    private fun populateGoalData() {
        val goal = currentGoal ?: return
        editTextGoalName.setText(goal.name)
        editTextTargetAmount.setText(goal.targetValue.toString())
        spinnerGoalCurrency.setSelection(getCurrencyIndex(goal.currency))
        selectedCreationDateInMillis = goal.creationDate
        updateCreationDateEditText()
        
        // Update button text to show "Update Goal" when editing
        buttonSaveGoal.text = "Update Goal"
    }

    private fun getCurrencyIndex(currency: String): Int {
        val currencies = arrayOf("INR", "AED")
        return currencies.indexOfFirst { it == currency }
    }

    private fun setupCurrencySpinner() {
        val currencies = arrayOf("INR", "AED")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGoalCurrency.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        goalDao.close()
    }
}