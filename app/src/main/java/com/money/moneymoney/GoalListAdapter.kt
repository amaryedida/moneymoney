package com.money.moneymoney

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoalListAdapter(
    private var goals: List<GoalWithProgress>,
    private val listener: OnItemActionListener
) :
    RecyclerView.Adapter<GoalListAdapter.GoalViewHolder>() {

    interface OnItemActionListener {
        fun onEditItem(goal: GoalObject)
        fun onDeleteItem(goal: GoalObject)
    }

    class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.text_view_goal_name)
        val creationDateTextView: TextView = itemView.findViewById(R.id.text_view_goal_creation_date)
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
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val creationDate = goal.creationDate ?: System.currentTimeMillis()
        holder.creationDateTextView.text = "Created: " + dateFormatter.format(Date(creationDate))
        holder.targetValueTextView.text = String.format("%.2f %s", goal.targetValue, goal.currency)
        holder.progressBar.progress = currentGoal.percentageProgress
        holder.percentageTextView.text = "${currentGoal.percentageProgress}%"
        holder.amountInvestedTextView.text = String.format("Invested: %.2f %s", currentGoal.amountInvested, goal.currency)

        holder.editButton.setOnClickListener {
            listener.onEditItem(goal)
        }

        holder.deleteButton.setOnClickListener {
            listener.onDeleteItem(goal)
        }
    }

    override fun getItemCount() = goals.size

    fun updateData(newGoals: List<GoalWithProgress>) {
        goals = newGoals
        notifyDataSetChanged()
    }
}