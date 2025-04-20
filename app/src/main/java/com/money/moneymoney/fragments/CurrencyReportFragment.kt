package com.money.moneymoney.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.money.moneymoney.R
import com.money.moneymoney.adapters.CurrencyReportAdapter
import com.money.moneymoney.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class CurrencyReportFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CurrencyReportAdapter
    private lateinit var dbHelper: DatabaseHelper
    private var currency: String = "INR"

    companion object {
        private const val ARG_CURRENCY = "currency"

        fun newInstance(currency: String): CurrencyReportFragment {
            return CurrencyReportFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CURRENCY, currency)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currency = it.getString(ARG_CURRENCY, "INR")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_currency_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.recyclerView)
        dbHelper = DatabaseHelper(requireContext())
        
        setupRecyclerView()
        loadReportData()
    }

    private fun setupRecyclerView() {
        adapter = CurrencyReportAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@CurrencyReportFragment.adapter
        }
    }

    private fun loadReportData() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        
        // Get data from database
        val db = dbHelper.readableDatabase
        
        // Query for income
        val incomeCursor = db.query(
            DatabaseHelper.TABLE_INCOME,
            arrayOf("SUM(${DatabaseHelper.COLUMN_INCOME_VALUE})"),
            "${DatabaseHelper.COLUMN_INCOME_CURRENCY} = ? AND ${DatabaseHelper.COLUMN_INCOME_DATE} = ?",
            arrayOf(currency, currentDate),
            null,
            null,
            null
        )
        
        // Query for expenses
        val expensesCursor = db.query(
            DatabaseHelper.TABLE_EXPENSES,
            arrayOf("SUM(${DatabaseHelper.COLUMN_EXPENSE_VALUE})"),
            "${DatabaseHelper.COLUMN_EXPENSE_CURRENCY} = ? AND ${DatabaseHelper.COLUMN_EXPENSE_DATE} = ?",
            arrayOf(currency, currentDate),
            null,
            null,
            null
        )
        
        var income = 0.0
        var expenses = 0.0
        
        if (incomeCursor.moveToFirst()) {
            income = incomeCursor.getDouble(0)
        }
        
        if (expensesCursor.moveToFirst()) {
            expenses = expensesCursor.getDouble(0)
        }
        
        incomeCursor.close()
        expensesCursor.close()
        
        val balance = income - expenses
        
        val reportItems = listOf(
            CurrencyReportAdapter.ReportItem(
                date = currentDate,
                income = income,
                expenses = expenses,
                balance = balance
            )
        )
        
        adapter.updateData(reportItems)
    }
} 