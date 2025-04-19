package com.money.moneymoney

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GoalProgressAdapter(private var goalProgressList: List<GoalWithProgress>) :
    RecyclerView.Adapter<GoalProgressAdapter.GoalProgressViewHolder>() {

    class GoalProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewGoalName: TextView = itemView.findViewById(R.id.textViewGoalName)
        val textViewGoalTargetValue: TextView = itemView.findViewById(R.id.textViewGoalTargetValue)
        val textViewAmountInvested: TextView = itemView.findViewById(R.id.textViewAmountInvested)
        val progressBarGoalProgress: ProgressBar = itemView.findViewById(R.id.progressBarGoalProgress)
        val textViewPercentageProgress: TextView = itemView.findViewById(R.id.textViewPercentageProgress)
        val textViewRemainingAmount: TextView = itemView.findViewById(R.id.textViewRemainingAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalProgressViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal_progress, parent, false)
        return GoalProgressViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: GoalProgressViewHolder, position: Int) {
        val currentGoalProgress = goalProgressList[position]
        val goal = currentGoalProgress.goal

        holder.textViewGoalName.text = goal.name
        holder.textViewGoalTargetValue.text = "${goal.currency ?: ""} ${String.format("%.2f", goal.targetValue)}"
        holder.textViewAmountInvested.text = "${goal.currency ?: ""} ${String.format("%.2f", currentGoalProgress.amountInvested)}"
        holder.progressBarGoalProgress.max = 100
        holder.progressBarGoalProgress.progress = currentGoalProgress.percentageProgress
        holder.textViewPercentageProgress.text = "${currentGoalProgress.percentageProgress}%"
        holder.textViewRemainingAmount.text = "${goal.currency ?: ""} ${String.format("%.2f", currentGoalProgress.remainingAmount)}"
    }

    override fun getItemCount(): Int {
        return goalProgressList.size
    }

    fun updateGoals(newGoalProgressList: List<GoalWithProgress>) {
        goalProgressList = newGoalProgressList
        notifyDataSetChanged()
    }
}