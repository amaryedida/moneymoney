package com.money.moneymoney.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.money.moneymoney.R
import java.text.NumberFormat
import java.util.*

class CurrencyReportAdapter : RecyclerView.Adapter<CurrencyReportAdapter.ViewHolder>() {
    
    data class ReportItem(
        val date: String,
        val income: Double,
        val expenses: Double,
        val balance: Double
    )
    
    private var reportItems: List<ReportItem> = emptyList()
    
    fun updateData(newItems: List<ReportItem>) {
        reportItems = newItems
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_currency_report, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = reportItems[position]
        holder.bind(item)
    }
    
    override fun getItemCount(): Int = reportItems.size
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val incomeTextView: TextView = itemView.findViewById(R.id.incomeTextView)
        private val expensesTextView: TextView = itemView.findViewById(R.id.expensesTextView)
        private val balanceTextView: TextView = itemView.findViewById(R.id.balanceTextView)
        
        private val numberFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
        
        fun bind(item: ReportItem) {
            dateTextView.text = item.date
            incomeTextView.text = numberFormat.format(item.income)
            expensesTextView.text = numberFormat.format(item.expenses)
            balanceTextView.text = numberFormat.format(item.balance)
            
            // Set balance text color based on value
            balanceTextView.setTextColor(
                itemView.context.getColor(
                    if (item.balance >= 0) android.R.color.holo_green_dark
                    else android.R.color.holo_red_dark
                )
            )
        }
    }
} 