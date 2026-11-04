package com.gymleague.app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gymleague.app.adapter.ChallengeAdapter
import com.gymleague.app.models.CreateChallengeRequest
import com.gymleague.app.util.ChallengeHelper
import com.gymleague.app.viewmodel.BattleEvent
import com.gymleague.app.viewmodel.ChallengeViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Battle Arena: create challenges, accept or decline invitations, log progress and see who won.
 * Opening this screen from the Squad page with an opponent (EXTRA_OPPONENT_ID) starts a
 * direct challenge against that squad member.
 */
class BattleActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GymLeagueBattle"

        // Used by the Squad screen to challenge one member directly
        const val EXTRA_OPPONENT_ID = "opponent_id"
        const val EXTRA_OPPONENT_NAME = "opponent_name"

        private const val MAX_XP_REWARD = 500
    }

    private lateinit var viewModel: ChallengeViewModel
    private lateinit var challengeAdapter: ChallengeAdapter
    private lateinit var sessionManager: SessionManager

    private var userId: Int = -1

    private val typeCodes = listOf(
        "MostWorkouts",
        "LongestStreak",
        "MostXP",
        "MostReps"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battle)

        BottomNavHelper.setup(this, findViewById(R.id.bottomNavigation), NavTab.BATTLE)

        sessionManager = SessionManager(this)
        userId = sessionManager.getUserId()

        if (userId == -1) {
            Log.w(TAG, "No signed-in user, closing Battle Arena")
            Toast.makeText(this, R.string.battle_error_session, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[ChallengeViewModel::class.java]

        val recyclerView = findViewById<RecyclerView>(R.id.rvChallenges)
        val tvError = findViewById<TextView>(R.id.tvBattleError)
        val tvEmpty = findViewById<TextView>(R.id.tvBattleEmpty)

        challengeAdapter = ChallengeAdapter(
            currentUserId = userId,
            onLogProgress = { viewModel.logProgress(it.challengeId, userId) },
            onAccept = { viewModel.acceptChallenge(it.challengeId, userId) },
            onDecline = { viewModel.declineChallenge(it.challengeId, userId) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = challengeAdapter

        findViewById<Button>(R.id.btnNewChallenge).setOnClickListener {
            showCreateChallengeDialog(null, null)
        }

        lifecycleScope.launch {
            viewModel.challenges.collect { challenges ->

                challengeAdapter.updateChallenges(challenges)

                tvEmpty.visibility = if (challenges.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (challenges.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { message ->

                if (!message.isNullOrEmpty()) {
                    tvError.visibility = View.VISIBLE
                    Toast.makeText(this@BattleActivity, R.string.battle_toast_load_failed, Toast.LENGTH_LONG).show()
                } else {
                    tvError.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.events.collect { event -> showEvent(event) }
        }

        // Only handle the Squad hand-off on a fresh start, not after a rotation
        if (savedInstanceState == null) {
            handleOpponentIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpponentIntent(intent)
    }

    // Refresh whenever the screen comes back, so accepted or finished battles show up
    override fun onResume() {
        super.onResume()

        if (userId != -1) {
            viewModel.loadChallenges(userId)
        }
    }

    private fun handleOpponentIntent(intent: Intent) {
        val opponentId = intent.getIntExtra(EXTRA_OPPONENT_ID, -1)

        if (opponentId == -1) {
            return
        }

        val opponentName = intent.getStringExtra(EXTRA_OPPONENT_NAME)

        // Clear the extras so the form is not shown again later
        intent.removeExtra(EXTRA_OPPONENT_ID)
        intent.removeExtra(EXTRA_OPPONENT_NAME)

        Log.d(TAG, "Starting a direct challenge against user $opponentId")

        showCreateChallengeDialog(opponentId, opponentName)
    }

    private fun showEvent(event: BattleEvent) {
        val message = when (event) {
            is BattleEvent.Launched -> getString(R.string.battle_toast_launched)
            is BattleEvent.Sent -> getString(R.string.battle_toast_sent, event.opponentName)
            is BattleEvent.Accepted -> getString(R.string.battle_toast_accepted)
            is BattleEvent.Declined -> getString(R.string.battle_toast_declined)
            is BattleEvent.Won -> getString(R.string.battle_toast_won, event.xp)
            is BattleEvent.Failed -> event.message ?: getString(R.string.battle_toast_action_failed)
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showCreateChallengeDialog(opponentId: Int?, opponentName: String?) {

        val dialogView = layoutInflater.inflate(R.layout.dialog_create_challenge, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val tvOpponent = dialogView.findViewById<TextView>(R.id.tvChallengeOpponent)
        val etTitle = dialogView.findViewById<EditText>(R.id.etChallengeTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etChallengeDescription)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerChallengeType)
        val etTarget = dialogView.findViewById<EditText>(R.id.etChallengeTarget)
        val etXpReward = dialogView.findViewById<EditText>(R.id.etChallengeXpReward)
        val etEndDate = dialogView.findViewById<EditText>(R.id.etChallengeEndDate)
        val btnLaunch = dialogView.findViewById<Button>(R.id.btnLaunchChallenge)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelChallenge)

        if (opponentId != null) {
            tvOpponent.text = getString(R.string.battle_dialog_vs, opponentName ?: "")
            tvOpponent.visibility = View.VISIBLE
        }

        val typeLabels = listOf(
            getString(R.string.battle_type_most_workouts),
            getString(R.string.battle_type_longest_streak),
            getString(R.string.battle_type_most_xp),
            getString(R.string.battle_type_most_reps)
        )

        val typeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            typeLabels
        )
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = typeAdapter

        // The end date is picked from a calendar, so it can never be typed in the wrong format
        etEndDate.setOnClickListener {
            val todayUtc = LocalDate.now(ZoneOffset.UTC)
            val current = ChallengeHelper.parseDate(etEndDate.text.toString().trim())
                ?: todayUtc.plusDays(7)

            val picker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    // month is 0-based here; LocalDate.toString() gives YYYY-MM-DD
                    etEndDate.setText(LocalDate.of(year, month + 1, day).toString())
                    etEndDate.error = null
                },
                current.year,
                current.monthValue - 1,
                current.dayOfMonth
            )

            // Today is the earliest allowed end date (the API works in UTC)
            picker.datePicker.minDate =
                todayUtc.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            picker.show()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnLaunch.setOnClickListener {

            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val typeCode = typeCodes[spinnerType.selectedItemPosition]
            val target = etTarget.text.toString().toIntOrNull()
            val xpReward = etXpReward.text.toString().toIntOrNull()
            val endDate = etEndDate.text.toString().trim()
            val today = LocalDate.now(ZoneOffset.UTC)

            if (title.isEmpty()) {
                etTitle.error = getString(R.string.battle_error_title)
                return@setOnClickListener
            }

            if (target == null || target <= 0) {
                etTarget.error = getString(R.string.battle_error_target)
                return@setOnClickListener
            }

            if (xpReward == null || xpReward <= 0 || xpReward > MAX_XP_REWARD) {
                etXpReward.error = getString(R.string.battle_error_xp)
                return@setOnClickListener
            }

            if (endDate.isEmpty()) {
                etEndDate.error = getString(R.string.battle_error_date_empty)
                return@setOnClickListener
            }

            if (ChallengeHelper.parseDate(endDate) == null || endDate.length != 10) {
                etEndDate.error = getString(R.string.battle_error_date_format)
                return@setOnClickListener
            }

            if (!ChallengeHelper.isValidEndDate(endDate, today)) {
                etEndDate.error = getString(R.string.battle_error_date_past)
                return@setOnClickListener
            }

            val request = CreateChallengeRequest(
                title = title,
                description = description,
                type = typeCode,
                target = target,
                xpReward = xpReward,
                endDate = "${endDate}T00:00:00Z",
                createdByUserId = userId,
                opponentUserId = opponentId
            )

            btnLaunch.isEnabled = false
            btnLaunch.text = getString(R.string.battle_launching)

            viewModel.createChallenge(
                request = request,
                opponentName = opponentName,
                userId = userId,
                onSuccess = { dialog.dismiss() },
                onFailure = {
                    // Let the user fix the form and try again
                    btnLaunch.isEnabled = true
                    btnLaunch.text = getString(R.string.battle_launch)
                }
            )
        }

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        dialog.show()
    }
}
