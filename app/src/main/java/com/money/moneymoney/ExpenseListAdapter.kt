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
        val categoryTextView: TextView = itemView.findViewById(R.id.text_view_category)
        val valueTextView: TextView = itemView.findViewById(R.id.text_view_value)
        val currencyTextView: TextView = itemView.findViewById(R.id.text_view_currency)
        val dateTextView: TextView = itemView.findViewById(R.id.text_view_date)
        val commentTextView: TextView = itemView.findViewById(R.id.text_view_comment)
        val editButton: View = itemView.findViewById(R.id.button_edit)
        val deleteButton: View = itemView.findViewById(R.id.button_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense_list, parent, false)
        return ExpenseViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val currentExpense = expenses[position]
        holder.categoryTextView.text = currentExpense.category
        holder.valueTextView.text = String.format(Locale.getDefault(), "%.2f", currentExpense.value)
        holder.currencyTextView.text = currentExpense.currency
        holder.dateTextView.text = sdf.format(Date(currentExpense.date))
        holder.commentTextView.text = currentExpense.comment ?: ""

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
