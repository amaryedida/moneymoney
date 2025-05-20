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
import android.util.Log

class InvestmentListAdapter(private var investments: List<InvestmentObject> = emptyList()) :
    RecyclerView.Adapter<InvestmentListAdapter.InvestmentViewHolder>() {

    companion object {
        private const val TAG = "InvestmentListAdapter"
    }

    interface OnItemActionListener {
        fun onEditItem(investment: InvestmentObject)
        fun onDeleteItem(investment: InvestmentObject)
    }

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    class InvestmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.textViewInvestmentName)
        val amountTextView: TextView = itemView.findViewById(R.id.textViewInvestmentAmount)
        val dateTextView: TextView = itemView.findViewById(R.id.textViewInvestmentDate)
        val typeTextView: TextView = itemView.findViewById(R.id.textViewInvestmentType)
        val editButton: ImageButton = itemView.findViewById(R.id.buttonEdit)
        val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDelete)
    }

    private var listener: OnItemActionListener? = null

    constructor(investments: List<InvestmentObject>, listener: OnItemActionListener) : this() {
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
        Log.d(TAG, "Binding investment: ID=${currentInvestment.id}, Category=${currentInvestment.category}, Value=${currentInvestment.value}, Currency=${currentInvestment.currency}, Date=${currentInvestment.date}, Comment=${currentInvestment.comment}")
        holder.nameTextView.text = currentInvestment.category
        holder.amountTextView.text = String.format("%.2f", currentInvestment.value)
        holder.dateTextView.text = dateFormatter.format(Date(currentInvestment.date))
        holder.typeTextView.text = currentInvestment.currency

        holder.editButton.setOnClickListener {
            listener?.onEditItem(currentInvestment)
        }

        holder.deleteButton.setOnClickListener {
            listener?.onDeleteItem(currentInvestment)
        }
    }

    override fun getItemCount() = investments.size

    fun updateData(newInvestments: List<InvestmentObject>) {
        investments = newInvestments
        notifyDataSetChanged()
    }
}