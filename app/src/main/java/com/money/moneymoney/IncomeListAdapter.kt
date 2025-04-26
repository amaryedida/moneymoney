package com.money.moneymoney

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

class IncomeListAdapter(private var incomes: List<IncomeObject>) :
    RecyclerView.Adapter<IncomeListAdapter.IncomeViewHolder>() {

    interface OnItemActionListener {
        fun onEditItem(income: IncomeObject)
        fun onDeleteItem(income: IncomeObject)
    }

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    class IncomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryTextView: TextView = itemView.findViewById(R.id.text_view_category)
        val valueTextView: TextView = itemView.findViewById(R.id.text_view_value)
        val dateTextView: TextView = itemView.findViewById(R.id.text_view_date)
        val currencyTextView: TextView = itemView.findViewById(R.id.text_view_currency)
        val commentTextView: TextView = itemView.findViewById(R.id.text_view_comment)
        val editButton: ImageButton = itemView.findViewById(R.id.button_edit)
        val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)
    }

    private var listener: OnItemActionListener? = null

    constructor(incomes: List<IncomeObject>, listener: OnItemActionListener) : this(incomes) {
        this.listener = listener
    }

    fun setOnItemActionListener(listener: OnItemActionListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_income_list, parent, false)
        return IncomeViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
        val currentIncome = incomes[position]
        holder.categoryTextView.text = currentIncome.category
        holder.valueTextView.text = String.format("%.2f", currentIncome.value) // Format to 2 decimal places
        holder.currencyTextView.text = currentIncome.currency
        holder.dateTextView.text = dateFormatter.format(Date(currentIncome.date))
        holder.commentTextView.text = currentIncome.comment

        holder.editButton.setOnClickListener {
            listener?.onEditItem(currentIncome)
        }

        holder.deleteButton.setOnClickListener {
            listener?.onDeleteItem(currentIncome)
        }
    }

    override fun getItemCount() = incomes.size

    fun updateData(newIncomes: List<IncomeObject>) {
        incomes = newIncomes
        notifyDataSetChanged()
    }
}