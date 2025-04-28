package com.money.moneymoney

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class GoalListActivity : AppCompatActivity(), GoalListAdapter.OnItemActionListener {

    private lateinit var recyclerViewGoals: RecyclerView
    private lateinit var goalAdapter: GoalListAdapter
    private lateinit var goalDao: GoalDao
    private lateinit var investmentDao: InvestmentDao
    private lateinit var editTextStartDate: EditText
    private lateinit var editTextEndDate: EditText
    private lateinit var buttonFilter: Button
    private lateinit var buttonClearFilter: Button

    private val editGoalLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadGoals() // Refresh the list after editing
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_list)

        recyclerViewGoals = findViewById(R.id.recycler_view_goals)
        editTextStartDate = findViewById(R.id.edit_text_start_date)
        editTextEndDate = findViewById(R.id.edit_text_end_date)
        buttonFilter = findViewById(R.id.button_filter)
        buttonClearFilter = findViewById(R.id.button_clear_filter)

        investmentDao = InvestmentDao(this)
        goalDao = GoalDao(this)
        goalAdapter = GoalListAdapter(emptyList(), this)
        recyclerViewGoals.layoutManager = LinearLayoutManager(this)
        recyclerViewGoals.adapter = goalAdapter

        loadGoals()
    }

    private fun loadGoals() {
        val goalsWithProgress = calculateGoalProgress()
    }

    override fun onDestroy() {
        super.onDestroy()
        goalDao.close()
        investmentDao.close()
    }

    override fun onEditItem(goal: GoalObject) {
        val intent = Intent(this, GoalEntryActivity::class.java)
        intent.putExtra("EXTRA_GOAL", goal)
        editGoalLauncher.launch(intent)
    }

    override fun onDeleteItem(goal: GoalObject) {
        goalDao.deleteGoal(goal)
        loadGoals()
        Toast.makeText(this, "Goal deleted", Toast.LENGTH_SHORT).show()
    }

    private fun calculateGoalProgress(): List<GoalWithProgress> {
        val goalsWithProgress = mutableListOf<GoalWithProgress>()
        val activeGoals = goalDao.getAllActiveGoals()

        for (goal in activeGoals) {
            val investmentsForGoal = investmentDao.getInvestmentsByGoalId(goal.id)
            val amountInvested = investmentsForGoal.sumOf { it.value }
            val percentageProgress = if (goal.targetValue > 0) (amountInvested / goal.targetValue * 100).toInt() else 0
            val remainingAmount = goal.targetValue - amountInvested
            goalsWithProgress.add(GoalWithProgress(goal, amountInvested, percentageProgress, remainingAmount))
        }

        goalAdapter.updateData(goalsWithProgress)
        return goalsWithProgress
    }

    /*
    private fun showDatePickerDialog(editText: EditText) {
        // Implement date picker if needed for filtering (though goals might not be directly filterable by date)
    }

    private fun filterGoals() {
        // Implement filtering logic if needed
    }

    private fun clearFilter() {
        // Implement clear filter logic if needed
    }
    */

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

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val EDIT_GOAL_REQUEST = 1
    }
}