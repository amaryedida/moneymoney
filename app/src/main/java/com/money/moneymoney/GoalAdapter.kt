package com.money.moneymoney

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoalAdapter(private val goals: MutableList<GoalObject>) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.textViewGoalName)
        val targetValueTextView: TextView = itemView.findViewById(R.id.textViewTargetValue)
        val currencyTextView: TextView = itemView.findViewById(R.id.textViewCurrency)
        val creationDateTextView: TextView = itemView.findViewById(R.id.textViewCreationDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val currentGoal = goals[position]
        holder.nameTextView.text = currentGoal.name
        holder.targetValueTextView.text = String.format("%.2f", currentGoal.targetValue)
        holder.currencyTextView.text = currentGoal.currency ?: ""
        
        // Format and display creation date
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val creationDate = currentGoal.creationDate ?: System.currentTimeMillis()
        holder.creationDateTextView.text = "Created: " + dateFormatter.format(Date(creationDate))
    }

    override fun getItemCount(): Int {
        return goals.size
    }

    fun updateGoals(newGoals: List<GoalObject>) {
        goals.clear()
        goals.addAll(newGoals)
        notifyDataSetChanged()
    }
}