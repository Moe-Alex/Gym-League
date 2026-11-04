package com.gymleague.app.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gymleague.app.R
import com.gymleague.app.models.Challenge
import com.gymleague.app.util.ChallengeAction
import com.gymleague.app.util.ChallengeBadge
import com.gymleague.app.util.ChallengeHelper
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Shows each Battle Arena challenge as a card: who is fighting, both people's progress,
 * and the buttons that make sense for the signed-in user (accept, decline or log progress).
 */
class ChallengeAdapter(
    private val currentUserId: Int,
    private val onLogProgress: (Challenge) -> Unit,
    private val onAccept: (Challenge) -> Unit,
    private val onDecline: (Challenge) -> Unit
) : RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder>() {

    private var challenges: List<Challenge> = emptyList()

    class ChallengeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvChallengeTitle)
        val badge: TextView = itemView.findViewById(R.id.tvLiveBadge)
        val versus: TextView = itemView.findViewById(R.id.tvChallengeVs)
        val description: TextView = itemView.findViewById(R.id.tvChallengeDescription)
        val meta: TextView = itemView.findViewById(R.id.tvChallengeMeta)
        val layoutProgress: View = itemView.findViewById(R.id.layoutProgress)
        val youLabel: TextView = itemView.findViewById(R.id.tvYouLabel)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressChallenge)
        val progressText: TextView = itemView.findViewById(R.id.tvChallengeProgress)
        val rivalLabel: TextView = itemView.findViewById(R.id.tvRivalLabel)
        val rivalBar: ProgressBar = itemView.findViewById(R.id.progressRival)
        val rivalText: TextView = itemView.findViewById(R.id.tvRivalProgress)
        val note: TextView = itemView.findViewById(R.id.tvChallengeNote)
        val primaryButton: Button = itemView.findViewById(R.id.btnLogProgress)
        val declineButton: Button = itemView.findViewById(R.id.btnDeclineChallenge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_challenge, parent, false)
        return ChallengeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        val challenge = challenges[position]
        val context = holder.itemView.context
        val today = LocalDate.now(ZoneOffset.UTC)

        val badge = ChallengeHelper.badge(challenge, currentUserId, today)
        val action = ChallengeHelper.action(challenge, currentUserId, today)
        val rivalName = ChallengeHelper.rivalName(challenge, currentUserId)

        holder.title.text = challenge.title
        holder.description.text = challenge.description?.takeIf { it.isNotBlank() }
            ?: friendlyType(context, challenge.type)
        holder.meta.text = context.getString(
            R.string.battle_meta,
            challenge.xpReward,
            challenge.endDate.take(10)
        )

        bindBadge(holder.badge, badge, context)
        bindVersus(holder.versus, challenge, rivalName, context)
        bindProgress(holder, challenge, rivalName, context)
        bindNote(holder.note, challenge, badge, rivalName, context)
        bindButtons(holder, challenge, action)
    }

    private fun bindBadge(view: TextView, badge: ChallengeBadge, context: Context) {
        val (label, color) = when (badge) {
            ChallengeBadge.OPEN -> R.string.battle_badge_open to "#25D9F5"
            ChallengeBadge.INVITED -> R.string.battle_badge_invited to "#25D9F5"
            ChallengeBadge.PENDING -> R.string.battle_badge_pending to "#FFB300"
            ChallengeBadge.LIVE -> R.string.battle_badge_live to "#B98CFF"
            ChallengeBadge.WON -> R.string.battle_badge_won to "#FFB300"
            ChallengeBadge.LOST -> R.string.battle_badge_lost to "#918A9D"
            ChallengeBadge.ENDED -> R.string.battle_badge_ended to "#918A9D"
            ChallengeBadge.DECLINED -> R.string.battle_badge_declined to "#918A9D"
        }

        view.text = context.getString(label)
        view.setTextColor(Color.parseColor(color))
    }

    private fun bindVersus(
        view: TextView,
        challenge: Challenge,
        rivalName: String?,
        context: Context
    ) {
        view.text = if (rivalName != null && challenge.opponentUserId != null) {
            context.getString(R.string.battle_versus, context.getString(R.string.battle_label_you), rivalName)
        } else {
            context.getString(R.string.battle_open_to_all, challenge.createdByUsername ?: "")
        }
    }

    private fun bindProgress(
        holder: ChallengeViewHolder,
        challenge: Challenge,
        rivalName: String?,
        context: Context
    ) {
        if (!ChallengeHelper.showsProgress(challenge)) {
            holder.layoutProgress.visibility = View.GONE
            return
        }

        holder.layoutProgress.visibility = View.VISIBLE

        val target = challenge.target.coerceAtLeast(1)
        val mine = ChallengeHelper.myProgress(challenge, currentUserId)
        val rival = ChallengeHelper.rivalProgress(challenge, currentUserId)

        holder.youLabel.text = context.getString(R.string.battle_label_you)
        holder.progressBar.max = target
        holder.progressBar.progress = mine
        holder.progressText.text = context.getString(R.string.battle_progress_count, mine, target)

        holder.rivalLabel.text = rivalName ?: context.getString(R.string.battle_label_opponent)
        holder.rivalBar.max = target
        holder.rivalBar.progress = rival
        holder.rivalText.text = context.getString(R.string.battle_progress_count, rival, target)
    }

    // The line under the progress: who won, or who we are waiting for
    private fun bindNote(
        view: TextView,
        challenge: Challenge,
        badge: ChallengeBadge,
        rivalName: String?,
        context: Context
    ) {
        val text: String? = when (badge) {
            ChallengeBadge.WON -> context.getString(R.string.battle_result_won, challenge.xpReward)
            ChallengeBadge.LOST -> context.getString(R.string.battle_result_lost, rivalName ?: "")
            ChallengeBadge.ENDED -> context.getString(R.string.battle_result_ended)
            ChallengeBadge.DECLINED -> context.getString(R.string.battle_result_declined)
            ChallengeBadge.PENDING ->
                if (rivalName != null) {
                    context.getString(R.string.battle_waiting_named, rivalName)
                } else {
                    context.getString(R.string.battle_waiting_opponent)
                }
            ChallengeBadge.OPEN ->
                if (ChallengeHelper.isCreator(challenge, currentUserId)) {
                    context.getString(R.string.battle_waiting_opponent)
                } else {
                    null
                }
            else -> null
        }

        view.visibility = if (text == null) View.GONE else View.VISIBLE
        view.text = text ?: ""
    }

    private fun bindButtons(
        holder: ChallengeViewHolder,
        challenge: Challenge,
        action: ChallengeAction
    ) {
        val context = holder.itemView.context

        holder.primaryButton.visibility = if (action == ChallengeAction.NONE) View.GONE else View.VISIBLE
        holder.declineButton.visibility =
            if (action == ChallengeAction.ACCEPT_OR_DECLINE) View.VISIBLE else View.GONE

        when (action) {
            ChallengeAction.LOG_PROGRESS -> {
                holder.primaryButton.text = context.getString(R.string.battle_btn_log_progress)
                holder.primaryButton.setOnClickListener { onLogProgress(challenge) }
            }

            ChallengeAction.ACCEPT, ChallengeAction.ACCEPT_OR_DECLINE -> {
                holder.primaryButton.text = context.getString(R.string.battle_btn_accept)
                holder.primaryButton.setOnClickListener { onAccept(challenge) }
                holder.declineButton.text = context.getString(R.string.battle_btn_decline)
                holder.declineButton.setOnClickListener { onDecline(challenge) }
            }

            ChallengeAction.NONE -> {
                holder.primaryButton.setOnClickListener(null)
                holder.declineButton.setOnClickListener(null)
            }
        }
    }

    private fun friendlyType(context: Context, type: String): String {
        return when (type) {
            "MostWorkouts" -> context.getString(R.string.battle_type_most_workouts)
            "LongestStreak" -> context.getString(R.string.battle_type_longest_streak)
            "MostXP" -> context.getString(R.string.battle_type_most_xp)
            "MostReps" -> context.getString(R.string.battle_type_most_reps)
            else -> type
        }
    }

    override fun getItemCount(): Int = challenges.size

    fun updateChallenges(newChallenges: List<Challenge>) {
        challenges = newChallenges
        notifyDataSetChanged()
    }
}
