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
import android.widget.FrameLayout
import android.widget.TextView

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
    private lateinit var textViewPreviousGoals: TextView
    private lateinit var recyclerViewPreviousGoals: RecyclerView
    private lateinit var customScrollbarLeft: View
    private lateinit var previousGoalsAdapter: GoalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "GoalEntryActivity onCreate started")
        setContentView(R.layout.activity_goal_entry)
        Log.d(TAG, "Layout set to activity_goal_entry")

        try {
            initializeViews()
            setupCurrencySpinner()
            setupRecyclerView()
            setupBottomNavigation()
            setupDatePicker()
            setupSaveButton()
            
            // Check if we're editing an existing goal
            currentGoal = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_GOAL, GoalObject::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_GOAL) as? GoalObject
            }
            
            if (currentGoal != null) {
                populateGoalData()
            }
            
            loadActiveGoals()
            updateCreationDateEditText()
            
            // Set up RecyclerView
            recyclerViewPreviousGoals.layoutManager = LinearLayoutManager(this)
            previousGoalsAdapter = GoalAdapter(mutableListOf())
            recyclerViewPreviousGoals.adapter = previousGoalsAdapter

            // Get reference to custom scrollbar
            customScrollbarLeft = findViewById(R.id.customScrollbarLeft)

            // Add scroll listener to RecyclerView
            recyclerViewPreviousGoals.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val verticalScrollRange = recyclerView.computeVerticalScrollRange()
                    val verticalScrollOffset = recyclerView.computeVerticalScrollOffset()
                    val verticalScrollExtent = recyclerView.computeVerticalScrollExtent()

                    if (verticalScrollRange <= verticalScrollExtent) {
                        // Content is not scrollable, hide scrollbar
                        customScrollbarLeft.visibility = View.GONE
                    } else {
                        // Content is scrollable, show and position scrollbar
                        customScrollbarLeft.visibility = View.VISIBLE

                        // Calculate scrollbar height
                        val scrollbarHeight = (verticalScrollExtent.toFloat() / verticalScrollRange.toFloat() * customScrollbarLeft.height.toFloat()).toInt()
                        val lp = customScrollbarLeft.layoutParams as FrameLayout.LayoutParams
                        lp.height = scrollbarHeight.coerceAtLeast(resources.getDimensionPixelSize(R.dimen.min_scrollbar_height))
                        customScrollbarLeft.layoutParams = lp

                        // Calculate scrollbar position
                        val scrollbarMaxTravel = customScrollbarLeft.height - scrollbarHeight
                        val scrollbarPosition = (verticalScrollOffset.toFloat() / (verticalScrollRange - verticalScrollExtent).toFloat() * scrollbarMaxTravel.toFloat()).toInt()
                        customScrollbarLeft.translationY = scrollbarPosition.toFloat()
                    }
                }
            })

            loadPreviousGoals() // Load previous goals
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in initialization", e)
            Toast.makeText(this, "Error initializing: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }
    }

    private fun initializeViews() {
        recyclerViewGoals = findViewById(R.id.recyclerViewGoals)
        layoutAddGoalForm = findViewById(R.id.layoutAddGoalForm)
        editTextGoalName = findViewById(R.id.editTextGoalName)
        editTextTargetAmount = findViewById(R.id.editTextTargetAmount)
        editTextGoalCreationDate = findViewById(R.id.editTextGoalCreationDate)
        spinnerGoalCurrency = findViewById(R.id.spinnerGoalCurrency)
        buttonSaveGoal = findViewById(R.id.buttonSaveGoal)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        textViewPreviousGoals = findViewById(R.id.textViewPreviousGoals)
        recyclerViewPreviousGoals = findViewById(R.id.recyclerViewPreviousGoals)
    }

    private fun setupCurrencySpinner() {
        val currencies = arrayOf("INR", "AED")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGoalCurrency.adapter = adapter
    }

    private fun setupRecyclerView() {
        goalDao = GoalDao(this)
        goalAdapter = GoalAdapter(mutableListOf())
        recyclerViewGoals.layoutManager = LinearLayoutManager(this)
        recyclerViewGoals.adapter = goalAdapter
    }

    private fun setupDatePicker() {
        editTextGoalCreationDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun setupSaveButton() {
        buttonSaveGoal.text = if (currentGoal == null) "Save Goal" else "Update Goal"
        buttonSaveGoal.setOnClickListener {
            if (currentGoal == null) {
                saveNewGoal()
            } else {
                updateExistingGoal()
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menu_home -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadActiveGoals() {
        val activeGoals = goalDao.getAllActiveGoals()
        goalAdapter.updateGoals(activeGoals)
        layoutAddGoalForm.visibility = View.VISIBLE
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
        try {
            val name = editTextGoalName.text.toString().trim()
            val targetValueStr = editTextTargetAmount.text.toString().trim()
            val currency = spinnerGoalCurrency.selectedItem.toString()
            val creationDateInMillis = selectedCreationDateInMillis

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a goal name", Toast.LENGTH_SHORT).show()
                return
            }

            if (targetValueStr.isEmpty()) {
                Toast.makeText(this, "Please enter a target amount", Toast.LENGTH_SHORT).show()
                return
            }

            if (creationDateInMillis == null) {
                Toast.makeText(this, "Please select a creation date", Toast.LENGTH_SHORT).show()
                return
            }

            val targetValue = targetValueStr.toDoubleOrNull()
            if (targetValue == null || targetValue <= 0) {
                Toast.makeText(this, "Please enter a valid target amount greater than 0", Toast.LENGTH_SHORT).show()
                return
            }

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
        } catch (e: Exception) {
            Log.e(TAG, "Error saving goal", e)
            Toast.makeText(this, "Error saving goal: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateExistingGoal() {
        try {
            val name = editTextGoalName.text.toString().trim()
            val targetValueStr = editTextTargetAmount.text.toString().trim()
            val currency = spinnerGoalCurrency.selectedItem.toString()
            val creationDateInMillis = selectedCreationDateInMillis

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a goal name", Toast.LENGTH_SHORT).show()
                return
            }

            if (targetValueStr.isEmpty()) {
                Toast.makeText(this, "Please enter a target amount", Toast.LENGTH_SHORT).show()
                return
            }

            if (creationDateInMillis == null) {
                Toast.makeText(this, "Please select a creation date", Toast.LENGTH_SHORT).show()
                return
            }

            val targetValue = targetValueStr.toDoubleOrNull()
            if (targetValue == null || targetValue <= 0) {
                Toast.makeText(this, "Please enter a valid target amount greater than 0", Toast.LENGTH_SHORT).show()
                return
            }

            val oldGoal = currentGoal ?: return
            // Create a new goal object with updated values
            val updatedGoal = GoalObject(
                id = oldGoal.id,
                name = name,
                targetValue = targetValue,
                currency = currency,
                creationDate = creationDateInMillis,
                status = oldGoal.status,
                completionDate = oldGoal.completionDate
            )
            
            if (goalDao.updateGoal(updatedGoal) > 0) {
                Toast.makeText(this, "Goal updated successfully", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Failed to update goal", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating goal", e)
            Toast.makeText(this, "Error updating goal: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun populateGoalData() {
        val goal = currentGoal ?: return
        editTextGoalName.setText(goal.name)
        editTextTargetAmount.setText(goal.targetValue.toString())
        spinnerGoalCurrency.setSelection(getCurrencyIndex(goal.currency ?: "INR"), false)
        selectedCreationDateInMillis = goal.creationDate
        updateCreationDateEditText()
        buttonSaveGoal.text = "Update Goal"
    }

    private fun getCurrencyIndex(currency: String): Int {
        val currencies = arrayOf("INR", "AED")
        val index = currencies.indexOfFirst { it == currency }
        return if (index >= 0) index else 0
    }

    private fun loadPreviousGoals() {
        val activeGoals = goalDao.getAllActiveGoals()
        previousGoalsAdapter.updateGoals(activeGoals)

        // Ensure scrollbar visibility is updated after data load
        recyclerViewPreviousGoals.post { // Post to ensure layout pass is complete
            val verticalScrollRange = recyclerViewPreviousGoals.computeVerticalScrollRange()
            val verticalScrollExtent = recyclerViewPreviousGoals.computeVerticalScrollExtent()
            if (verticalScrollRange <= verticalScrollExtent) {
                customScrollbarLeft.visibility = View.GONE
            } else {
                customScrollbarLeft.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        goalDao.close()
    }
}