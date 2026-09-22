package com.gymleague.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gymleague.app.R
import com.gymleague.app.models.Challenge

class ChallengeAdapter(
    private var challenges: List<Challenge> = emptyList(),
    private val onLogProgress: (Challenge) -> Unit
) : RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder>() {

    class ChallengeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvChallengeTitle)
        val description: TextView = itemView.findViewById(R.id.tvChallengeDescription)
        val meta: TextView = itemView.findViewById(R.id.tvChallengeMeta)
        val liveBadge: TextView = itemView.findViewById(R.id.tvLiveBadge)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressChallenge)
        val progressText: TextView = itemView.findViewById(R.id.tvChallengeProgress)
        val logButton: Button = itemView.findViewById(R.id.btnLogProgress)
    }

    private fun friendlyType(type: String): String {
        return when (type) {
            "MostWorkouts" -> "Most Workouts"
            "LongestStreak" -> "Longest Streak"
            "MostXP" -> "Most XP"
            "MostReps" -> "Most Reps"
            else -> type
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_challenge, parent, false)
        return ChallengeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        val challenge = challenges[position]

        holder.title.text = challenge.title
        holder.description.text = challenge.description.ifBlank { friendlyType(challenge.type) }
        holder.meta.text = "+${challenge.xpReward} XP · Ends ${challenge.endDate.take(10)}"

        holder.progressBar.max = challenge.target
        holder.progressBar.progress = challenge.currentProgress
        holder.progressText.text = "${challenge.currentProgress}/${challenge.target}"

        if (challenge.completed) {
            holder.liveBadge.text = "DONE"
            holder.logButton.isEnabled = false
            holder.logButton.text = "COMPLETED"
        } else {
            holder.liveBadge.text = "LIVE"
            holder.logButton.isEnabled = true
            holder.logButton.text = "LOG PROGRESS (+1)"
        }

        holder.logButton.setOnClickListener {
            onLogProgress(challenge)
        }
    }

    override fun getItemCount(): Int = challenges.size

    fun updateChallenges(newChallenges: List<Challenge>) {
        challenges = newChallenges
        notifyDataSetChanged()
    }
}