package com.money.moneymoney

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_GOAL_COMPLETION_DATE
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_GOAL_CREATION_DATE
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_GOAL_CURRENCY
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_GOAL_ID
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_GOAL_NAME
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_GOAL_STATUS
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_GOAL_TARGET_VALUE
import com.money.moneymoney.DatabaseHelper.Companion.TABLE_GOALS

class GoalDao(context: Context) {

    private val dbHelper = DatabaseHelper(context = context)
    private val database: SQLiteDatabase = dbHelper.writableDatabase

    companion object {
        const val STATUS_ACTIVE = "Active"
        const val STATUS_COMPLETED = "Completed"
    }

    // Corrected addGoal function to include creationDate
    fun addGoal(name: String, targetValue: Double, creationDate: Long?, currency: String): Long {
        val values = ContentValues().apply {
            put(COLUMN_GOAL_NAME, name)
            put(COLUMN_GOAL_TARGET_VALUE, targetValue)
            put(COLUMN_GOAL_CURRENCY, currency)
            put(COLUMN_GOAL_CREATION_DATE, creationDate) // Add creation date
            put(COLUMN_GOAL_STATUS, STATUS_ACTIVE)
        }
        return database.insert(TABLE_GOALS, null, values)
    }

    fun updateGoalStatus(goalId: Long, status: String, completionDate: Long? = null): Int {
        val values = ContentValues().apply {
            put(COLUMN_GOAL_STATUS, status)
            completionDate?.let { put(COLUMN_GOAL_COMPLETION_DATE, it) }
        }
        return database.update(TABLE_GOALS, values, "$COLUMN_GOAL_ID = ?", arrayOf(goalId.toString()))
    }

    fun getAllActiveGoals(): MutableList<GoalObject> {
        return getGoalsByStatus(STATUS_ACTIVE)
    }

    fun getAllCompletedGoals(): MutableList<GoalObject> {
        return getGoalsByStatus(STATUS_COMPLETED)
    }

    private fun getGoalsByStatus(status: String): MutableList<GoalObject> {
        val goals = mutableListOf<GoalObject>()
        val cursor: Cursor = database.query(
            TABLE_GOALS,
            arrayOf(
                COLUMN_GOAL_ID,
                COLUMN_GOAL_NAME,
                COLUMN_GOAL_TARGET_VALUE,
                COLUMN_GOAL_CURRENCY,
                COLUMN_GOAL_CREATION_DATE,
                COLUMN_GOAL_STATUS,
                COLUMN_GOAL_COMPLETION_DATE
            ),
            "$COLUMN_GOAL_STATUS = ?",
            arrayOf(status),
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val goal = createGoalFromCursor(it)
                    goals.add(goal)
                } while (it.moveToNext())
            }
        }
        return goals
    }

    fun updateGoal(goal: GoalObject): Int {
        val values = ContentValues().apply {
            put(COLUMN_GOAL_NAME, goal.name)
            put(COLUMN_GOAL_TARGET_VALUE, goal.targetValue)
            put(COLUMN_GOAL_CURRENCY, goal.currency)
            put(COLUMN_GOAL_CREATION_DATE, goal.creationDate)
            put(COLUMN_GOAL_STATUS, goal.status)
            put(COLUMN_GOAL_COMPLETION_DATE, goal.completionDate)
        }
        return database.update(TABLE_GOALS, values, "$COLUMN_GOAL_ID = ?", arrayOf(goal.id.toString()))
    }

    fun deleteGoal(goal: GoalObject): Int {
        return database.delete(TABLE_GOALS, "$COLUMN_GOAL_ID = ?", arrayOf(goal.id.toString()))
    }

    fun getGoalById(goalId: Long): GoalObject? {
        val cursor: Cursor = database.query(
            TABLE_GOALS,
            arrayOf(
                COLUMN_GOAL_ID,
                COLUMN_GOAL_NAME,
                COLUMN_GOAL_TARGET_VALUE,
                COLUMN_GOAL_CURRENCY,
                COLUMN_GOAL_CREATION_DATE,
                COLUMN_GOAL_STATUS,
                COLUMN_GOAL_COMPLETION_DATE
            ),
            "$COLUMN_GOAL_ID = ?",
            arrayOf(goalId.toString()),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                createGoalFromCursor(it)
            } else {
                null
            }
        }
    }

    private fun createGoalFromCursor(cursor: Cursor): GoalObject {
        return GoalObject(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GOAL_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GOAL_NAME)),
            targetValue = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_GOAL_TARGET_VALUE)),
            currency = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GOAL_CURRENCY)),
            creationDate = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_GOAL_CREATION_DATE)),
            status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GOAL_STATUS)),
            completionDate = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_GOAL_COMPLETION_DATE))
        )
    }

    private fun Cursor.getLongOrNull(columnIndex: Int): Long? {
        return if (isNull(columnIndex)) null else getLong(columnIndex)
    }

    fun close() {
        dbHelper.close()
    }
}