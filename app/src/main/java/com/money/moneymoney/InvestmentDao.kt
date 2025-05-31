package com.money.moneymoney

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_GOAL_ID
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_GOAL_NAME
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INVESTMENT_CATEGORY
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INVESTMENT_COMMENT
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INVESTMENT_CURRENCY
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INVESTMENT_DATE
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INVESTMENT_GOAL_ID
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INVESTMENT_ID
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INVESTMENT_VALUE
import com.money.moneymoney.DatabaseHelper.Companion.TABLE_GOALS
import com.money.moneymoney.DatabaseHelper.Companion.TABLE_INVESTMENTS
import java.util.Calendar

class InvestmentDao(context: Context) {
    private val TAG = "InvestmentDao"
    private val dbHelper = DatabaseHelper(context)
    private val database: SQLiteDatabase = dbHelper.writableDatabase

    fun addInvestment(
        currency: String,
        category: String,
        value: Double,
        comment: String?,
        date: Long,
        goalId: Long? = null
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

    fun getInvestmentsByCurrency(currency: String): List<InvestmentObject> {
        val investments = mutableListOf<InvestmentObject>()
        val query = "SELECT i.*, g.${DatabaseHelper.COLUMN_GOAL_NAME} FROM ${DatabaseHelper.TABLE_INVESTMENTS} i " +
                "LEFT JOIN ${DatabaseHelper.TABLE_GOALS} g ON i.${DatabaseHelper.COLUMN_INVESTMENT_GOAL_ID} = g.${DatabaseHelper.COLUMN_GOAL_ID} " +
                "WHERE i.${DatabaseHelper.COLUMN_INVESTMENT_CURRENCY} = ?" +
                " ORDER BY i.${DatabaseHelper.COLUMN_INVESTMENT_DATE} DESC"
        val cursor = database.rawQuery(query, arrayOf(currency))

        cursor.use {
            while (it.moveToNext()) {
                investments.add(createInvestmentFromCursor(it))
            }
        }
        return investments
    }

    fun getInvestmentsByCurrencyAndDateRange(
        currency: String,
        startDate: Long?,
        endDate: Long?
    ): List<InvestmentObject> {
        val investments = mutableListOf<InvestmentObject>()
        val selectionArgs = mutableListOf<String>()
        var query = "SELECT i.*, g.${DatabaseHelper.COLUMN_GOAL_NAME} FROM ${DatabaseHelper.TABLE_INVESTMENTS} i " +
                "LEFT JOIN ${DatabaseHelper.TABLE_GOALS} g ON i.${DatabaseHelper.COLUMN_INVESTMENT_GOAL_ID} = g.${DatabaseHelper.COLUMN_GOAL_ID} " +
                "WHERE i.${DatabaseHelper.COLUMN_INVESTMENT_CURRENCY} = ?"
        selectionArgs.add(currency)

        if (startDate != null) {
            query += " AND i.${DatabaseHelper.COLUMN_INVESTMENT_DATE} >= ?"
            selectionArgs.add(startDate.toString())
        }

        if (endDate != null) {
            query += " AND i.${DatabaseHelper.COLUMN_INVESTMENT_DATE} <= ?"
            selectionArgs.add(endDate.toString())
        }

        query += " ORDER BY i.${DatabaseHelper.COLUMN_INVESTMENT_DATE} DESC"

        val cursor = database.rawQuery(query, selectionArgs.toTypedArray())

        cursor.use {
            while (it.moveToNext()) {
                investments.add(createInvestmentFromCursor(it))
            }
        }
        return investments
    }

    fun getInvestmentsByGoalId(goalId: Long): List<InvestmentObject> {
        val investments = mutableListOf<InvestmentObject>()
        val query = """
            SELECT i.*, g.name AS goal_name
            FROM $TABLE_INVESTMENTS i
            LEFT JOIN $TABLE_GOALS g ON i.$COLUMN_INVESTMENT_GOAL_ID = g.$COLUMN_GOAL_ID
            WHERE i.$COLUMN_INVESTMENT_GOAL_ID = ?
        """.trimIndent()

        val cursor = database.rawQuery(query, arrayOf(goalId.toString()))
        
        cursor.use {
            while (it.moveToNext()) {
                investments.add(createInvestmentFromCursor(it))
            }
        }
        return investments
    }

    fun getInvestmentsForMonth(year: Int, month: Int): List<InvestmentObject> {
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

        val query = """
            SELECT i.*, g.name AS goal_name
            FROM $TABLE_INVESTMENTS i
            LEFT JOIN $TABLE_GOALS g ON i.$COLUMN_INVESTMENT_GOAL_ID = g.$COLUMN_GOAL_ID
            WHERE i.$COLUMN_INVESTMENT_DATE BETWEEN ? AND ?
        """.trimIndent()

        val cursor = database.rawQuery(query, arrayOf(startDate.toString(), endDate.toString()))
        val investments = mutableListOf<InvestmentObject>()
        
        cursor.use {
            while (it.moveToNext()) {
                investments.add(createInvestmentFromCursor(it))
            }
        }
        return investments
    }

    fun updateInvestment(investment: InvestmentObject): Int {
        val values = ContentValues().apply {
            put(COLUMN_INVESTMENT_CURRENCY, investment.currency)
            put(COLUMN_INVESTMENT_CATEGORY, investment.category)
            put(COLUMN_INVESTMENT_VALUE, investment.value)
            put(COLUMN_INVESTMENT_COMMENT, investment.comment)
            put(COLUMN_INVESTMENT_DATE, investment.date)
            investment.goalId?.let { put(COLUMN_INVESTMENT_GOAL_ID, it) }
        }
        return database.update(
            TABLE_INVESTMENTS,
            values,
            "$COLUMN_INVESTMENT_ID = ?",
            arrayOf(investment.id.toString())
        )
    }

    fun deleteInvestment(investment: InvestmentObject): Int {
        return database.delete(
            TABLE_INVESTMENTS,
            "$COLUMN_INVESTMENT_ID = ?",
            arrayOf(investment.id.toString())
        )
    }

    private fun createInvestmentFromCursor(cursor: Cursor): InvestmentObject {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INVESTMENT_ID))
        val currency = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INVESTMENT_CURRENCY))
        val category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INVESTMENT_CATEGORY))
        val value = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INVESTMENT_VALUE))
        val comment = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INVESTMENT_COMMENT))
        val date = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INVESTMENT_DATE))
        val goalId = if (cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INVESTMENT_GOAL_ID))) null else cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INVESTMENT_GOAL_ID))
        
        // Check if goal_name column exists before trying to read it (for backward compatibility if needed)
        val goalNameColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_GOAL_NAME)
        val goalName = if (goalNameColumnIndex != -1 && !cursor.isNull(goalNameColumnIndex)) {
            cursor.getString(goalNameColumnIndex)
        } else {
            null
        }
        
        return InvestmentObject(id, currency, category, value, comment, date, goalId, goalName)
    }

    private fun Cursor.getLongOrNull(columnIndex: Int): Long? {
        return if (isNull(columnIndex)) null else getLong(columnIndex)
    }

    private fun Cursor.getStringOrNull(columnIndex: Int): String? {
        return if (isNull(columnIndex)) null else getString(columnIndex)
    }

    fun close() {
        dbHelper.close()
    }

    fun getLastTenInvestments(): List<InvestmentObject> {
        val investments = mutableListOf<InvestmentObject>()
        val query = "SELECT i.*, g.${DatabaseHelper.COLUMN_GOAL_NAME} FROM ${DatabaseHelper.TABLE_INVESTMENTS} i " +
                "LEFT JOIN ${DatabaseHelper.TABLE_GOALS} g ON i.${DatabaseHelper.COLUMN_INVESTMENT_GOAL_ID} = g.${DatabaseHelper.COLUMN_GOAL_ID} " +
                "ORDER BY i.${DatabaseHelper.COLUMN_INVESTMENT_DATE} DESC LIMIT 10"
        val cursor = database.rawQuery(query, null)

        cursor.use {
            while (it.moveToNext()) {
                investments.add(createInvestmentFromCursor(it))
            }
        }
        return investments
    }
}
