package com.money.moneymoney

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GoalListAdapter(private var goals: List<GoalWithProgress>) :
    RecyclerView.Adapter<GoalListAdapter.GoalViewHolder>() {

    interface OnItemActionListener {
        fun onEditItem(goal: GoalObject)
        fun onDeleteItem(goal: GoalObject)
    }

    class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.text_view_goal_name)
        val targetValueTextView: TextView = itemView.findViewById(R.id.text_view_target_value)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar_goal)
        val percentageTextView: TextView = itemView.findViewById(R.id.text_view_percentage)
        val amountInvestedTextView: TextView = itemView.findViewById(R.id.text_view_amount_invested)
        val editButton: ImageButton = itemView.findViewById(R.id.button_edit)
        val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal_list, parent, false)
        return GoalViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val currentGoal = goals[position]
        val goal = currentGoal.goal
        holder.nameTextView.text = goal.name
        holder.targetValueTextView.text = String.format("%.2f %s", goal.targetValue, goal.currency)
        holder.progressBar.progress = currentGoal.percentageProgress
        holder.percentageTextView.text = "${currentGoal.percentageProgress}%"
        holder.amountInvestedTextView.text = String.format("%.2f %s", currentGoal.amountInvested, goal.currency)

        holder.editButton.setOnClickListener {
            (holder.itemView.context as? OnItemActionListener)?.onEditItem(goal)
        }

        holder.deleteButton.setOnClickListener {
            (holder.itemView.context as? OnItemActionListener)?.onDeleteItem(goal)
        }
    }

    override fun getItemCount() = goals.size

    fun updateData(newGoals: List<GoalWithProgress>) {
        goals = newGoals
        notifyDataSetChanged()
    }
}