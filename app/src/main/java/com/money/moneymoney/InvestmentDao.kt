package com.money.moneymoney

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INVESTMENT_CATEGORY
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INVESTMENT_COMMENT
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INVESTMENT_CURRENCY
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INVESTMENT_DATE
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INVESTMENT_GOAL_ID
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INVESTMENT_ID
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INVESTMENT_VALUE
import com.money.moneymoney.DatabaseHelper.Companion.TABLE_INVESTMENTS
import java.util.Calendar

class InvestmentDao(context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val database: SQLiteDatabase = dbHelper.writableDatabase

    fun addInvestment(
        currency: String,
        category: String,
        value: Double,
        comment: String?,
        date: Long,
        goalId: Long? = null // Goal ID can be null if no goal is associated
    ): Long {
        val values = ContentValues().apply {
            put(COLUMN_INVESTMENT_CURRENCY, currency)
            put(COLUMN_INVESTMENT_CATEGORY, category)
            put(COLUMN_INVESTMENT_VALUE, value)
            put(COLUMN_INVESTMENT_COMMENT, comment)
            put(COLUMN_INVESTMENT_DATE, date)
            goalId?.let { put(COLUMN_INVESTMENT_GOAL_ID, it) }
        }
        return database.insert(TABLE_INVESTMENTS, null, values)
    }

    fun getLastTenInvestments(): List<Investment> {
        val investments = mutableListOf<Investment>()
        val cursor: Cursor = database.query(
            TABLE_INVESTMENTS,
            arrayOf(
                COLUMN_INVESTMENT_ID,
                COLUMN_INVESTMENT_CURRENCY,
                COLUMN_INVESTMENT_CATEGORY,
                COLUMN_INVESTMENT_VALUE,
                COLUMN_INVESTMENT_COMMENT,
                COLUMN_INVESTMENT_DATE,
                COLUMN_INVESTMENT_GOAL_ID
            ),
            null,
            null,
            null,
            null,
            "${COLUMN_INVESTMENT_DATE} DESC",
            "10"
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_ID))
                val currency = it.getString(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_CURRENCY))
                val category = it.getString(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_CATEGORY))
                val value = it.getDouble(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_VALUE))
                val comment = it.getString(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_COMMENT))
                val date = it.getLong(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_DATE))
                val goalIdIndex = it.getColumnIndexOrThrow(COLUMN_INVESTMENT_GOAL_ID)
                val goalId = if (it.isNull(goalIdIndex)) null else it.getLong(goalIdIndex)

                val investment = Investment(id, currency, category, value, comment, date, goalId)
                investments.add(investment)
            }
        }
        return investments
    }

    fun getInvestmentsByGoalId(goalId: Long): List<Investment> {
        val investments = mutableListOf<Investment>()
        val cursor = database.query(
            TABLE_INVESTMENTS,
            arrayOf(
                COLUMN_INVESTMENT_ID,
                COLUMN_INVESTMENT_CURRENCY,
                COLUMN_INVESTMENT_CATEGORY,
                COLUMN_INVESTMENT_VALUE,
                COLUMN_INVESTMENT_COMMENT,
                COLUMN_INVESTMENT_DATE,
                COLUMN_INVESTMENT_GOAL_ID
            ),
            "$COLUMN_INVESTMENT_GOAL_ID = ?",
            arrayOf(goalId.toString()),
            null,
            null,
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_ID))
                val currency = it.getString(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_CURRENCY))
                val category = it.getString(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_CATEGORY))
                val value = it.getDouble(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_VALUE))
                val comment = it.getString(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_COMMENT))
                val date = it.getLong(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_DATE))
                val goalIdIndex = it.getColumnIndexOrThrow(COLUMN_INVESTMENT_GOAL_ID)
                val retrievedGoalId = if (it.isNull(goalIdIndex)) null else it.getLong(goalIdIndex)

                val investment = Investment(id, currency, category, value, comment, date, retrievedGoalId)
                investments.add(investment)
            }
        }
        return investments
    }

    fun getInvestmentsForMonth(year: Int, month: Int): List<Investment> {
        val investmentList = mutableListOf<Investment>()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startDate = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.timeInMillis
        val cursor = database.query(
            TABLE_INVESTMENTS,
            arrayOf(
                COLUMN_INVESTMENT_ID,
                COLUMN_INVESTMENT_CURRENCY,
                COLUMN_INVESTMENT_CATEGORY,
                COLUMN_INVESTMENT_VALUE,
                COLUMN_INVESTMENT_COMMENT,
                COLUMN_INVESTMENT_DATE,
                COLUMN_INVESTMENT_GOAL_ID
            ),
            "${COLUMN_INVESTMENT_DATE} >= ? AND ${COLUMN_INVESTMENT_DATE} <= ?",
            arrayOf(startDate.toString(), endDate.toString()),
            null,
            null,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_ID))
                val currency = it.getString(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_CURRENCY))
                val category = it.getString(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_CATEGORY))
                val value = it.getDouble(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_VALUE))
                val comment = it.getString(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_COMMENT))
                val date = it.getLong(it.getColumnIndexOrThrow(COLUMN_INVESTMENT_DATE))
                val goalIdIndex = it.getColumnIndexOrThrow(COLUMN_INVESTMENT_GOAL_ID)
                val retrievedGoalId = if (it.isNull(goalIdIndex)) null else it.getLong(goalIdIndex)

                val investment = Investment(id, currency, category, value, comment, date, retrievedGoalId)
                investmentList.add(investment)
            }
        }
        return investmentList
    }

    fun close() {
        dbHelper.close()
    }
}

data class Investment(
    val id: Long,
    val currency: String,
    val category: String,
    val value: Double,
    val comment: String?,
    val date: Long,
    val goalId: Long?
)
