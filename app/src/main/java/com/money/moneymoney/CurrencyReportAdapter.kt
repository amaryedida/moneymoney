package com.money.moneymoney

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CurrencyReportAdapter : RecyclerView.Adapter<CurrencyReportAdapter.ViewHolder>() {

    private var reportItems: List<ReportItem> = emptyList()

    data class ReportItem(
        val date: String,
        val income: Double,
        val expenses: Double,
        val balance: Double
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTextView: TextView = view.findViewById(R.id.dateTextView)
        val incomeTextView: TextView = view.findViewById(R.id.incomeTextView)
        val expensesTextView: TextView = view.findViewById(R.id.expensesTextView)
        val balanceTextView: TextView = view.findViewById(R.id.balanceTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_currency_report, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = reportItems[position]
        holder.dateTextView.text = item.date
        holder.incomeTextView.text = String.format("%.2f", item.income)
        holder.expensesTextView.text = String.format("%.2f", item.expenses)
        holder.balanceTextView.text = String.format("%.2f", item.balance)
    }

    override fun getItemCount() = reportItems.size

    fun updateData(newItems: List<ReportItem>) {
        reportItems = newItems
        notifyDataSetChanged()
    }
} 