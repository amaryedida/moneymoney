package com.money.moneymoney

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreviousIncomeAdapter(private var previousIncomes: List<IncomeObject>) :
    RecyclerView.Adapter<PreviousIncomeAdapter.PreviousIncomeViewHolder>() {

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    class PreviousIncomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.textViewPreviousIncomeDate)
        val currencyTextView: TextView = itemView.findViewById(R.id.textViewPreviousIncomeCurrency)
        val valueTextView: TextView = itemView.findViewById(R.id.textViewPreviousIncomeValue)
        val categoryTextView: TextView = itemView.findViewById(R.id.textViewPreviousIncomeCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviousIncomeViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_previous_income, parent, false)
        return PreviousIncomeViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PreviousIncomeViewHolder, position: Int) {
        val currentIncome = previousIncomes[position]
        holder.dateTextView.text = sdf.format(Date(currentIncome.date))
        holder.currencyTextView.text = currentIncome.currency
        holder.valueTextView.text = String.format(Locale.getDefault(), "%.2f", currentIncome.value)
        holder.categoryTextView.text = currentIncome.category
    }

    override fun getItemCount() = previousIncomes.size

    fun updateData(newList: List<IncomeObject>) {
        previousIncomes = newList
        notifyDataSetChanged()
    }
}