package com.money.moneymoney

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseListAdapter(private var expenses: List<ExpenseObject>) :
    RecyclerView.Adapter<ExpenseListAdapter.ExpenseViewHolder>() {

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    interface OnItemActionListener {
        fun onEditItem(expense: ExpenseObject)
        fun onDeleteItem(expense: ExpenseObject)
    }

    private var listener: OnItemActionListener? = null

    constructor(expenses: List<ExpenseObject>, listener: OnItemActionListener) : this(expenses) {
        this.listener = listener
    }

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.textViewExpenseDate)
        val currencyTextView: TextView = itemView.findViewById(R.id.textViewExpenseCurrency)
        val valueTextView: TextView = itemView.findViewById(R.id.textViewExpenseValue)
        val categoryTextView: TextView = itemView.findViewById(R.id.textViewExpenseCategory)
        val editButton: TextView = itemView.findViewById(R.id.textViewEditExpense)
        val deleteButton: TextView = itemView.findViewById(R.id.textViewDeleteExpense)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val currentExpense = expenses[position]
        holder.dateTextView.text = sdf.format(Date(currentExpense.date))
        holder.currencyTextView.text = currentExpense.currency
        holder.valueTextView.text = String.format(Locale.getDefault(), "%.2f", currentExpense.value)
        holder.categoryTextView.text = currentExpense.category

        holder.editButton.setOnClickListener {
            listener?.onEditItem(currentExpense)
        }

        holder.deleteButton.setOnClickListener {
            listener?.onDeleteItem(currentExpense)
        }
    }

    override fun getItemCount() = expenses.size

    fun updateData(newList: List<ExpenseObject>) {
        expenses = newList
        notifyDataSetChanged()
    }
}
