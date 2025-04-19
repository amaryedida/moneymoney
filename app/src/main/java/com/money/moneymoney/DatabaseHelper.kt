package com.money.moneymoney
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "MoneyMoney.db"
        const val DATABASE_VERSION = 2 // Ensured version is incremented

        // Income Table
        const val TABLE_INCOME = "income"
        const val COLUMN_INCOME_ID = "_id" // Standard convention for primary key
        const val COLUMN_INCOME_CURRENCY = "currency"
        const val COLUMN_INCOME_CATEGORY = "income_category"
        const val COLUMN_INCOME_VALUE = "value"
        const val COLUMN_INCOME_COMMENT = "comment"
        const val COLUMN_INCOME_DATE = "date"

        // Expenses Table
        const val TABLE_EXPENSES = "expenses"
        const val COLUMN_EXPENSE_ID = "_id"
        const val COLUMN_EXPENSE_CURRENCY = "currency"
        const val COLUMN_EXPENSE_CATEGORY = "expense_category"
        const val COLUMN_EXPENSE_VALUE = "value"
        const val COLUMN_EXPENSE_COMMENT = "comment"
        const val COLUMN_EXPENSE_DATE = "date"

        // Investments Table
        const val TABLE_INVESTMENTS = "investments"
        const val COLUMN_INVESTMENT_ID = "_id"
        const val COLUMN_INVESTMENT_CURRENCY = "currency"
        const val COLUMN_INVESTMENT_CATEGORY = "investment_category"
        const val COLUMN_INVESTMENT_VALUE = "value"
        const val COLUMN_INVESTMENT_COMMENT = "comment"
        const val COLUMN_INVESTMENT_DATE = "date"
        const val COLUMN_INVESTMENT_GOAL_ID = "goal_id"

        // Goals Table
        const val TABLE_GOALS = "goals"
        const val COLUMN_GOAL_ID = "_id"
        const val COLUMN_GOAL_NAME = "name"
        const val COLUMN_GOAL_TARGET_VALUE = "target_value"
        const val COLUMN_GOAL_CURRENCY = "currency"
        const val COLUMN_GOAL_CREATION_DATE = "creation_date"
        const val COLUMN_GOAL_STATUS = "status"
        const val COLUMN_GOAL_COMPLETION_DATE = "completion_date"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        Log.d("DatabaseHelper", "onCreate() called") // Added log

        val createIncomeTable = "CREATE TABLE $TABLE_INCOME (" +
                "$COLUMN_INCOME_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_INCOME_CURRENCY TEXT NOT NULL," +
                "$COLUMN_INCOME_CATEGORY TEXT NOT NULL," +
                "$COLUMN_INCOME_VALUE REAL NOT NULL," +
                "$COLUMN_INCOME_COMMENT TEXT," +
                "$COLUMN_INCOME_DATE INTEGER NOT NULL);"

        val createExpensesTable = "CREATE TABLE $TABLE_EXPENSES (" +
                "$COLUMN_EXPENSE_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_EXPENSE_CURRENCY TEXT NOT NULL," +
                "$COLUMN_EXPENSE_CATEGORY TEXT NOT NULL," +
                "$COLUMN_EXPENSE_VALUE REAL NOT NULL," +
                "$COLUMN_EXPENSE_COMMENT TEXT," +
                "$COLUMN_EXPENSE_DATE INTEGER NOT NULL);"

        val createInvestmentsTable = "CREATE TABLE $TABLE_INVESTMENTS (" +
                "$COLUMN_INVESTMENT_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_INVESTMENT_CURRENCY TEXT NOT NULL," +
                "$COLUMN_INVESTMENT_CATEGORY TEXT NOT NULL," +
                "$COLUMN_INVESTMENT_VALUE REAL NOT NULL," +
                "$COLUMN_INVESTMENT_COMMENT TEXT," +
                "$COLUMN_INVESTMENT_DATE INTEGER NOT NULL," +
                "$COLUMN_INVESTMENT_GOAL_ID INTEGER);"

        val createGoalsTable = "CREATE TABLE $TABLE_GOALS (" +
                "$COLUMN_GOAL_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_GOAL_NAME TEXT NOT NULL," +
                "$COLUMN_GOAL_TARGET_VALUE REAL NOT NULL," +
                "$COLUMN_GOAL_CURRENCY TEXT NOT NULL," +
                "$COLUMN_GOAL_CREATION_DATE INTEGER NOT NULL," +
                "$COLUMN_GOAL_STATUS TEXT NOT NULL DEFAULT 'Active'," +
                "$COLUMN_GOAL_COMPLETION_DATE INTEGER);"

        Log.d("DatabaseHelper", "Executing SQL: $createIncomeTable") // Added log
        db?.execSQL(createIncomeTable)
        Log.d("DatabaseHelper", "Executing SQL: $createExpensesTable") // Added log
        db?.execSQL(createExpensesTable)
        Log.d("DatabaseHelper", "Executing SQL: $createInvestmentsTable") // Added log
        db?.execSQL(createInvestmentsTable)
        Log.d("DatabaseHelper", "Executing SQL: $createGoalsTable") // Added log
        db?.execSQL(createGoalsTable)
    }
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.d("DatabaseHelper", "onUpgrade() called from version $oldVersion to $newVersion") // Added log
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_INCOME")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_EXPENSES")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_INVESTMENTS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_GOALS")
        onCreate(db)
    }
}
