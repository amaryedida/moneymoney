package com.money.moneymoney

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar
import java.util.Locale

class GoalEntryActivity : AppCompatActivity() {

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
    private var selectedCreationDateInMillis: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_entry)

        btnAddGoal = findViewById(R.id.btnAddGoal)
        recyclerViewGoals = findViewById(R.id.recyclerViewGoals)
        layoutAddGoalForm = findViewById(R.id.layoutAddGoalForm)
        editTextGoalName = findViewById(R.id.editTextGoalName)
        editTextTargetAmount = findViewById(R.id.editTextTargetAmount)
        editTextGoalCreationDate = findViewById(R.id.editTextGoalCreationDate)
        spinnerGoalCurrency = findViewById(R.id.spinnerGoalCurrency)
        buttonSaveGoal = findViewById(R.id.buttonSaveGoal)

        goalDao = GoalDao(this)
        goalAdapter = GoalAdapter(mutableListOf())
        recyclerViewGoals.layoutManager = LinearLayoutManager(this)
        recyclerViewGoals.adapter = goalAdapter

        loadActiveGoals() // Load goals on activity creation

        btnAddGoal.setOnClickListener {
            layoutAddGoalForm.visibility = View.VISIBLE
        }

        editTextGoalCreationDate.setOnClickListener {
            showDatePickerDialog()
        }

        buttonSaveGoal.setOnClickListener {
            saveNewGoal()
        }

        updateCreationDateEditText()
    }

    override fun onResume() {
        super.onResume()
        loadActiveGoals() // Reload goals when the activity resumes
    }

    private fun loadActiveGoals() {
        val activeGoals = goalDao.getAllActiveGoals()
        goalAdapter.updateGoals(activeGoals)
        layoutAddGoalForm.visibility = View.GONE // Hide form when loading list
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
                loadActiveGoals() // Reload the list
            } else {
                Toast.makeText(this, "Failed to save goal", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please enter goal name, target amount, and creation date", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        goalDao.close()
    }
}