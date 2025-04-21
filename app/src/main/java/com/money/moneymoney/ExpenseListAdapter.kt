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
import java.util.Locale

class ExpenseListAdapter(private var expenses: List<Expense>) :
    RecyclerView.Adapter<ExpenseListAdapter.ExpenseViewHolder>() {

    interface OnItemActionListener {
        fun onEditItem(expense: Expense)
        fun onDeleteItem(expense: Expense)
    }

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryTextView: TextView = itemView.findViewById(R.id.text_view_category)
        val valueTextView: TextView = itemView.findViewById(R.id.text_view_value)
        val dateTextView: TextView = itemView.findViewById(R.id.text_view_date)
        val currencyTextView: TextView = itemView.findViewById(R.id.text_view_currency)
        val commentTextView: TextView = itemView.findViewById(R.id.text_view_comment)
        val editButton: ImageButton = itemView.findViewById(R.id.button_edit)
        val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)
    }

    private var listener: OnItemActionListener? = null

    constructor(expenses: List<Expense>, listener: OnItemActionListener) : this(expenses) {
        this.listener = listener
    }

    fun setOnItemActionListener(listener: OnItemActionListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense_list, parent, false)
        return ExpenseViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val currentExpense = expenses[position]
        holder.categoryTextView.text = currentExpense.category
        holder.valueTextView.text = String.format("%.2f", currentExpense.value) // Format to 2 decimal places
        holder.currencyTextView.text = currentExpense.currency
        holder.dateTextView.text = dateFormatter.format(Date(currentExpense.date))
        holder.commentTextView.text = currentExpense.comment

        holder.editButton.setOnClickListener {
            listener?.onEditItem(currentExpense)
        }

        holder.deleteButton.setOnClickListener {
            listener?.onDeleteItem(currentExpense)
        }


    }

    override fun getItemCount() = expenses.size

    fun updateData(newExpenses: List<Expense>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }
}
