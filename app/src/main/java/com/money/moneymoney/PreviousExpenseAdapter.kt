package com.money.moneymoney

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreviousExpenseAdapter(private var previousExpenses: List<ExpenseObject>) :
    RecyclerView.Adapter<PreviousExpenseAdapter.PreviousExpenseViewHolder>() {

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    class PreviousExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.textViewPreviousExpenseDate)
        val currencyTextView: TextView = itemView.findViewById(R.id.textViewPreviousExpenseCurrency)
        val valueTextView: TextView = itemView.findViewById(R.id.textViewPreviousExpenseValue)
        val categoryTextView: TextView = itemView.findViewById(R.id.textViewPreviousExpenseCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviousExpenseViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_previous_expense, parent, false)
        return PreviousExpenseViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PreviousExpenseViewHolder, position: Int) {
        val currentExpense = previousExpenses[position]
        holder.dateTextView.text = sdf.format(Date(currentExpense.date))
        holder.currencyTextView.text = currentExpense.currency
        holder.valueTextView.text = String.format(Locale.getDefault(), "%.2f", currentExpense.value)
        holder.categoryTextView.text = currentExpense.category
    }

    override fun getItemCount() = previousExpenses.size

    fun updateData(newList: List<ExpenseObject>) {
        previousExpenses = newList
        notifyDataSetChanged()
    }
}
