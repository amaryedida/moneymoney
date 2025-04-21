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

class InvestmentListAdapter(private var investments: List<Investment> = emptyList()) :
    RecyclerView.Adapter<InvestmentListAdapter.InvestmentViewHolder>() {

    interface OnItemActionListener {
        fun onEditItem(investment: Investment)
        fun onDeleteItem(investment: Investment)
    }

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    class InvestmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryTextView: TextView = itemView.findViewById(R.id.text_view_category)
        val valueTextView: TextView = itemView.findViewById(R.id.text_view_value)
        val dateTextView: TextView = itemView.findViewById(R.id.text_view_date)
        val currencyTextView: TextView = itemView.findViewById(R.id.text_view_currency)
        val commentTextView: TextView = itemView.findViewById(R.id.text_view_comment)
        val goalNameTextView: TextView = itemView.findViewById(R.id.textViewGoalName)
        val editButton: ImageButton = itemView.findViewById(R.id.button_edit)
        val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)
    }

    private var listener: OnItemActionListener? = null

    constructor(investments: List<Investment>, listener: OnItemActionListener) : this() {
        this.listener = listener
    }

    fun setOnItemActionListener(listener: OnItemActionListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvestmentViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_investment_list, parent, false) // TODO: Ensure item_investment_list.xml exists
        return InvestmentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: InvestmentViewHolder, position: Int) {
        val currentInvestment = investments[position]
        holder.categoryTextView.text = currentInvestment.category
        holder.goalNameTextView.text = currentInvestment.goalName ?: "No Goal"
        holder.valueTextView.text = String.format("%.2f", currentInvestment.value)
        holder.currencyTextView.text = currentInvestment.currency
        holder.dateTextView.text = dateFormatter.format(Date(currentInvestment.date))
        holder.commentTextView.text = currentInvestment.comment

        holder.editButton.setOnClickListener {
            listener?.onEditItem(currentInvestment)
        }

        holder.deleteButton.setOnClickListener {
            listener?.onDeleteItem(currentInvestment)
        }
    }

    override fun getItemCount() = investments.size

    fun updateData(newInvestments: List<Investment>) {
        investments = newInvestments
        notifyDataSetChanged()
    }
}