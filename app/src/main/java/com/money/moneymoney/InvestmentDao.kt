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
        val investmentList = mutableListOf<InvestmentObject>()
        val query = """
            SELECT i.*, g.name AS goal_name
            FROM $TABLE_INVESTMENTS i
            LEFT JOIN $TABLE_GOALS g ON i.$COLUMN_INVESTMENT_GOAL_ID = g.$COLUMN_GOAL_ID
            WHERE i.$COLUMN_INVESTMENT_CURRENCY = ?
            ORDER BY i.$COLUMN_INVESTMENT_DATE DESC
        """.trimIndent()

        val cursor = database.rawQuery(query, arrayOf(currency))
        
        cursor.use {
            while (it.moveToNext()) {
                investmentList.add(createInvestmentFromCursor(it))
            }
        }
        return investmentList
    }

    fun getInvestmentsByCurrencyAndDateRange(
        currency: String,
        startDate: Long?,
        endDate: Long?
    ): List<InvestmentObject> {
        val investmentList = mutableListOf<InvestmentObject>()
        val queryBuilder = StringBuilder("""
            SELECT i.*, g.name AS goal_name
            FROM $TABLE_INVESTMENTS i
            LEFT JOIN $TABLE_GOALS g ON i.$COLUMN_INVESTMENT_GOAL_ID = g.$COLUMN_GOAL_ID
            WHERE i.$COLUMN_INVESTMENT_CURRENCY = ?
        """.trimIndent())

        val args = mutableListOf<String>(currency)

        if (startDate != null && endDate != null) {
            queryBuilder.append(" AND i.$COLUMN_INVESTMENT_DATE BETWEEN ? AND ?")
            args.add(startDate.toString())
            args.add(endDate.toString())
        } else if (startDate != null) {
            queryBuilder.append(" AND i.$COLUMN_INVESTMENT_DATE >= ?")
            args.add(startDate.toString())
        } else if (endDate != null) {
            queryBuilder.append(" AND i.$COLUMN_INVESTMENT_DATE <= ?")
            args.add(endDate.toString())
        }

        queryBuilder.append(" ORDER BY i.$COLUMN_INVESTMENT_DATE DESC")

        val cursor = database.rawQuery(queryBuilder.toString(), args.toTypedArray())
        
        cursor.use {
            while (it.moveToNext()) {
                investmentList.add(createInvestmentFromCursor(it))
            }
        }
        return investmentList
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
        return InvestmentObject(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_INVESTMENT_ID)),
            currency = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INVESTMENT_CURRENCY)),
            category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INVESTMENT_CATEGORY)),
            value = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_INVESTMENT_VALUE)),
            comment = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INVESTMENT_COMMENT)),
            date = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_INVESTMENT_DATE)),
            goalId = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COLUMN_INVESTMENT_GOAL_ID)),
            goalName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("goal_name"))
        )
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
        val query = """
            SELECT i.*, g.name AS goal_name
            FROM $TABLE_INVESTMENTS i
            LEFT JOIN $TABLE_GOALS g ON i.$COLUMN_INVESTMENT_GOAL_ID = g.$COLUMN_GOAL_ID
            ORDER BY i.$COLUMN_INVESTMENT_DATE DESC
            LIMIT 10
        """.trimIndent()

        val cursor = database.rawQuery(query, null)
        val investments = mutableListOf<InvestmentObject>()

        cursor.use {
            while (it.moveToNext()) {
                investments.add(createInvestmentFromCursor(it))
            }
        }
        return investments
    }
}
