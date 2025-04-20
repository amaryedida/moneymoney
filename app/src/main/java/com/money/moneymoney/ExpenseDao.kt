package com.money.moneymoney
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_EXPENSE_CATEGORY
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_EXPENSE_COMMENT
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_EXPENSE_CURRENCY
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_EXPENSE_DATE
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_EXPENSE_ID
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_EXPENSE_VALUE
import com.money.moneymoney.DatabaseHelper.Companion.TABLE_EXPENSES
import java.util.Calendar

class ExpenseDao(context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val database: SQLiteDatabase = dbHelper.writableDatabase
    private val TAG = "ExpenseDao"

    fun addExpense(currency: String, category: String, value: Double, comment: String?, date: Long): Long {
        Log.d(TAG, "Adding expense: $currency, $category, $value, $comment, $date")
        val values = ContentValues().apply {
            put(COLUMN_EXPENSE_CURRENCY, currency)
            put(COLUMN_EXPENSE_CATEGORY, category)
            put(COLUMN_EXPENSE_VALUE, value)
            put(COLUMN_EXPENSE_COMMENT, comment)
            put(COLUMN_EXPENSE_DATE, date)
        }
        val id = database.insert(TABLE_EXPENSES, null, values)
        Log.d(TAG, "Added expense with ID: $id")
        return id
    }

    fun getExpensesForMonth(year: Int, month: Int): List<Expense> {
        Log.d(TAG, "Getting expenses for year: $year, month: $month")
        val expenseList = mutableListOf<Expense>()
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
            TABLE_EXPENSES,
            arrayOf(
                COLUMN_EXPENSE_ID,
                COLUMN_EXPENSE_CURRENCY,
                COLUMN_EXPENSE_CATEGORY,
                COLUMN_EXPENSE_VALUE,
                COLUMN_EXPENSE_COMMENT,
                COLUMN_EXPENSE_DATE
            ),
            "${COLUMN_EXPENSE_DATE} >= ? AND ${COLUMN_EXPENSE_DATE} <= ?",
            arrayOf(startDate.toString(), endDate.toString()),
            null,
            null,
            null
        )

        Log.d(TAG, "Found ${cursor.count} expenses")
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_EXPENSE_ID))
                val currency = it.getString(it.getColumnIndexOrThrow(COLUMN_EXPENSE_CURRENCY))
                val category = it.getString(it.getColumnIndexOrThrow(COLUMN_EXPENSE_CATEGORY))
                val value = it.getDouble(it.getColumnIndexOrThrow(COLUMN_EXPENSE_VALUE))
                val comment = it.getString(it.getColumnIndexOrThrow(COLUMN_EXPENSE_COMMENT))
                val date = it.getLong(it.getColumnIndexOrThrow(COLUMN_EXPENSE_DATE))
                val expense = Expense(id, currency, category, value, comment, date)
                expenseList.add(expense)
            }
        }
        return expenseList
    }

    // * New method to get the last 10 expenses *
    fun getLastTenExpenses(): List<Expense> {
        Log.d(TAG, "Getting last 10 expenses")
        val expenses = mutableListOf<Expense>()
        val cursor: Cursor = database.query(
            TABLE_EXPENSES,
            arrayOf(
                COLUMN_EXPENSE_ID,
                COLUMN_EXPENSE_CURRENCY,
                COLUMN_EXPENSE_CATEGORY,
                COLUMN_EXPENSE_VALUE,
                COLUMN_EXPENSE_COMMENT,
                COLUMN_EXPENSE_DATE),
            null, // No WHERE clause to get all expenses
            null,
            null,
            null,
            "${COLUMN_EXPENSE_DATE} DESC", // Order by date descending (newest first)
            "10" // Limit the result to 10 rows
        )

        Log.d(TAG, "Found ${cursor.count} expenses")
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_EXPENSE_ID))
                val currency = it.getString(it.getColumnIndexOrThrow(COLUMN_EXPENSE_CURRENCY))
                val category = it.getString(it.getColumnIndexOrThrow(COLUMN_EXPENSE_CATEGORY))
                val value = it.getDouble(it.getColumnIndexOrThrow(COLUMN_EXPENSE_VALUE))
                val comment = it.getString(it.getColumnIndexOrThrow(COLUMN_EXPENSE_COMMENT))
                val date = it.getLong(it.getColumnIndexOrThrow(COLUMN_EXPENSE_DATE))
                val expense = Expense(id, currency, category, value, comment, date)
                expenses.add(expense)
            }
        }
        return expenses
    }

    fun close() {
        dbHelper.close()
    }
}

data class Expense(
    val id: Long,
    val currency: String,
    val category: String,
    val value: Double,
    val comment: String?,
    val date: Long
)