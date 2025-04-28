package com.money.moneymoney

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GoalAdapter(private val goals: MutableList<GoalObject>) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.textViewGoalName)
        val targetValueTextView: TextView = itemView.findViewById(R.id.textViewGoalTargetValue)
        // Add more TextViews for other goal details if needed
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal, parent, false) // Inflate your item layout
        return GoalViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val currentGoal = goals[position]
        holder.nameTextView.text = currentGoal.name
        holder.targetValueTextView.text = "${currentGoal.targetValue} ${currentGoal.currency ?: ""}"
        // Bind other goal details to your ViewHolder's views
    }

    override fun getItemCount(): Int {
        return goals.size
    }

    fun updateGoals(newGoals: List<GoalObject>) {
        goals.clear()
        goals.addAll(newGoals)
        notifyDataSetChanged() // Tell the RecyclerView the data has changed
    }
}