package com.money.moneymoney

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class   PreviousInvestmentAdapter(
    private var previousInvestments: List<InvestmentObject>,
    private val goalDao: GoalDao
) : RecyclerView.Adapter<PreviousInvestmentAdapter.PreviousInvestmentViewHolder>() {

    class PreviousInvestmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.textViewPreviousInvestmentDate)
        val currencyTextView: TextView = itemView.findViewById(R.id.textViewPreviousInvestmentCurrency)
        val valueTextView: TextView = itemView.findViewById(R.id.textViewPreviousInvestmentValue)
        val categoryTextView: TextView = itemView.findViewById(R.id.textViewPreviousInvestmentCategory)
        val goalTextView: TextView = itemView.findViewById(R.id.textViewPreviousInvestmentGoal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviousInvestmentViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_previous_investment, parent, false)
        return PreviousInvestmentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PreviousInvestmentViewHolder, position: Int) {
        val currentInvestment = previousInvestments[position]
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        holder.dateTextView.text = sdf.format(Date(currentInvestment.date))
        holder.currencyTextView.text = currentInvestment.currency
        holder.valueTextView.text = String.format(Locale.getDefault(), "%.2f", currentInvestment.value)
        holder.categoryTextView.text = currentInvestment.category
        
        // Display the goal name if it exists
        if (currentInvestment.goalId != null) {
            val goal = goalDao.getGoalById(currentInvestment.goalId)
            if (goal != null) {
                holder.goalTextView.visibility = View.VISIBLE
                holder.goalTextView.text = "Goal: ${goal.name}"
            } else {
                holder.goalTextView.visibility = View.GONE
            }
        } else {
            holder.goalTextView.visibility = View.GONE
        }
    }

    override fun getItemCount() = previousInvestments.size

    fun updateData(newList: List<InvestmentObject>) {
        previousInvestments = newList
        notifyDataSetChanged()
    }
}