package com.money.moneymoney
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INCOME_CATEGORY
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INCOME_COMMENT
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INCOME_CURRENCY
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INCOME_DATE
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INCOME_ID
import com.money.moneymoney.DatabaseHelper.Companion.COLUMN_INCOME_VALUE
import com.money.moneymoney.DatabaseHelper.Companion.TABLE_INCOME
import java.util.Calendar
import android.util.Log

class IncomeDao(context: Context) {
    private val dbHelper = DatabaseHelper(context = context)
    private val database: SQLiteDatabase = dbHelper.writableDatabase
    private val TAG = "IncomeDao"

    fun addIncome(currency: String, category: String, value: Double, comment: String?, date: Long): Long {
        val values = ContentValues().apply {
            put(COLUMN_INCOME_CURRENCY, currency)
            put(COLUMN_INCOME_CATEGORY, category)
            put(COLUMN_INCOME_VALUE, value)
            put(COLUMN_INCOME_COMMENT, comment)
            put(COLUMN_INCOME_DATE, date)
        }
        val id = database.insert(TABLE_INCOME, null, values)
        Log.d(TAG, "Added income with ID: $id")
        return id
    }

    fun getIncomesForMonth(year: Int, month: Int): List<Income> {
        val incomeList = mutableListOf<Income>()
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
            TABLE_INCOME,
            arrayOf(
                COLUMN_INCOME_ID,
                COLUMN_INCOME_CURRENCY,
                COLUMN_INCOME_CATEGORY,
                COLUMN_INCOME_VALUE,
                COLUMN_INCOME_COMMENT,
                COLUMN_INCOME_DATE
            ),
            "${COLUMN_INCOME_DATE} >= ? AND ${COLUMN_INCOME_DATE} <= ?",
            arrayOf(startDate.toString(), endDate.toString()),
            null,
            null,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_INCOME_ID))
                val currency = it.getString(it.getColumnIndexOrThrow(COLUMN_INCOME_CURRENCY))
                val category = it.getString(it.getColumnIndexOrThrow(COLUMN_INCOME_CATEGORY))
                val value = it.getDouble(it.getColumnIndexOrThrow(COLUMN_INCOME_VALUE))
                val comment = it.getString(it.getColumnIndexOrThrow(COLUMN_INCOME_COMMENT))
                val date = it.getLong(it.getColumnIndexOrThrow(COLUMN_INCOME_DATE))
                val income = Income(id, currency, category, value, comment, date)
                incomeList.add(income)
            }
        }
        return incomeList
    }

    fun getIncomesByCurrency(currency: String): List<Income> {
        Log.d(TAG, "Getting incomes for currency: $currency")
        val incomeList = mutableListOf<Income>()
        val cursor = database.query(
            TABLE_INCOME,
            arrayOf(
                COLUMN_INCOME_ID,
                COLUMN_INCOME_CURRENCY,
                COLUMN_INCOME_CATEGORY,
                COLUMN_INCOME_VALUE,
                COLUMN_INCOME_COMMENT,
                COLUMN_INCOME_DATE
            ),
            "${COLUMN_INCOME_CURRENCY} = ?",
            arrayOf(currency),
            null,
            null,
            "${COLUMN_INCOME_DATE} DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_INCOME_ID))
                val incomeCurrency = it.getString(it.getColumnIndexOrThrow(COLUMN_INCOME_CURRENCY))
                val category = it.getString(it.getColumnIndexOrThrow(COLUMN_INCOME_CATEGORY))
                val value = it.getDouble(it.getColumnIndexOrThrow(COLUMN_INCOME_VALUE))
                val comment = it.getString(it.getColumnIndexOrThrow(COLUMN_INCOME_COMMENT))
                val date = it.getLong(it.getColumnIndexOrThrow(COLUMN_INCOME_DATE))
                val income = Income(id, incomeCurrency, category, value, comment, date)
                incomeList.add(income)
            }
        }
        return incomeList
    }

    fun getIncomesByCurrencyAndDateRange(currency: String, startDate: Long? = null, endDate: Long? = null): List<Income> {
        Log.d(TAG, "Getting incomes for currency: $currency, startDate: $startDate, endDate: $endDate")
        val incomeList = mutableListOf<Income>()
        val selection = StringBuilder("${COLUMN_INCOME_CURRENCY} = ?")
        val selectionArgs = mutableListOf(currency)
        // Build the date range selection
        if (startDate != null && endDate != null) {
            selection.append(" AND ${COLUMN_INCOME_DATE} >= ? AND ${COLUMN_INCOME_DATE} <= ?")
            selectionArgs.add(startDate.toString())
            selectionArgs.add(endDate.toString())
        } else if (startDate != null) {
            selection.append(" AND ${COLUMN_INCOME_DATE} >= ?")
            selectionArgs.add(startDate.toString())
        } else if (endDate != null) {
            selection.append(" AND ${COLUMN_INCOME_DATE} <= ?")
            selectionArgs.add(endDate.toString())
        }

        val cursor = database.query(
            TABLE_INCOME,
            arrayOf(
                COLUMN_INCOME_ID,
                COLUMN_INCOME_CURRENCY,
                COLUMN_INCOME_CATEGORY,
                COLUMN_INCOME_VALUE,
                COLUMN_INCOME_COMMENT,
                COLUMN_INCOME_DATE
            ),
            selection.toString(),
            selectionArgs.toTypedArray(),
            null,
            null,
            "${COLUMN_INCOME_DATE} DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_INCOME_ID))
                val incomeCurrency = it.getString(it.getColumnIndexOrThrow(COLUMN_INCOME_CURRENCY))
                val category = it.getString(it.getColumnIndexOrThrow(COLUMN_INCOME_CATEGORY))
                val value = it.getDouble(it.getColumnIndexOrThrow(COLUMN_INCOME_VALUE))
                val comment = it.getString(it.getColumnIndexOrThrow(COLUMN_INCOME_COMMENT))
                val date = it.getLong(it.getColumnIndexOrThrow(COLUMN_INCOME_DATE))
                val income = Income(id, incomeCurrency, category, value, comment, date)
                incomeList.add(income)
            }
        }
        return incomeList
    }

    fun getLastTenIncomes(): List<Income> {
        val incomes = mutableListOf<Income>()
        val cursor: Cursor = database.query(
            TABLE_INCOME, // Use the correct table name constant
            arrayOf(
                COLUMN_INCOME_ID,
                COLUMN_INCOME_CURRENCY,
                COLUMN_INCOME_CATEGORY,
                COLUMN_INCOME_VALUE,
                COLUMN_INCOME_COMMENT,
                COLUMN_INCOME_DATE
            ),
            null, // No WHERE clause to get all incomes
            null,
            null,
            null,
            "${COLUMN_INCOME_DATE} DESC", // Order by date descending (newest first)
            "10" // Limit the result to 10 rows
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_INCOME_ID))
                val currency = it.getString(it.getColumnIndexOrThrow(COLUMN_INCOME_CURRENCY))
                val category = it.getString(it.getColumnIndexOrThrow(COLUMN_INCOME_CATEGORY))
                val value = it.getDouble(it.getColumnIndexOrThrow(COLUMN_INCOME_VALUE))
                val comment = it.getString(it.getColumnIndexOrThrow(COLUMN_INCOME_COMMENT))
                val date = it.getLong(it.getColumnIndexOrThrow(COLUMN_INCOME_DATE))
                val income = Income(id, currency, category, value, comment, date)
                incomes.add(income)
            }
        }
        return incomes
    }

    fun updateIncome(income: Income) {
        Log.d(TAG, "Updating income: ${income.id}")
        val values = ContentValues().apply {
            put(COLUMN_INCOME_CURRENCY, income.currency)
            put(COLUMN_INCOME_CATEGORY, income.category)
            put(COLUMN_INCOME_VALUE, income.value)
            put(COLUMN_INCOME_COMMENT, income.comment)
            put(COLUMN_INCOME_DATE, income.date)
        }
        val rowsAffected = database.update(
            TABLE_INCOME,
            values,
            "${COLUMN_INCOME_ID} = ?",
            arrayOf(income.id.toString())
        )
        Log.d(TAG, "Updated income with ID: ${income.id}, rows affected: $rowsAffected")
    }

    fun deleteIncome(income: Income) {
        Log.d(TAG, "Deleting income: ${income.id}")
        val rowsAffected = database.delete(
            TABLE_INCOME,
            "${COLUMN_INCOME_ID} = ?",
            arrayOf(income.id.toString())
        )
        Log.d(TAG, "Deleted income with ID: ${income.id}, rows affected: $rowsAffected")
    }

    fun close() {
        dbHelper.close()
    }
}


data class Income(

    val id: Long,
    val currency: String,
    val category: String,
    val value: Double,
    val comment: String?,
    val date: Long
)