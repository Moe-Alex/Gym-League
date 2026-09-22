package com.gymleague.app

import android.os.Bundle
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
import com.gymleague.app.viewmodel.ChallengeViewModel
import kotlinx.coroutines.launch

class BattleActivity : AppCompatActivity() {

    private lateinit var viewModel: ChallengeViewModel
    private lateinit var challengeAdapter: ChallengeAdapter

    private val typeCodes = listOf(
        "MostWorkouts",
        "LongestStreak",
        "MostXP",
        "MostReps"
    )

    private val typeLabels = listOf(
        "Most Workouts",
        "Longest Streak",
        "Most XP",
        "Most Reps"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battle)

        BottomNavHelper.setup(this, findViewById(R.id.bottomNavigation), NavTab.BATTLE)

        viewModel = ViewModelProvider(this)[ChallengeViewModel::class.java]

        val recyclerView = findViewById<RecyclerView>(R.id.rvChallenges)
        val tvError = findViewById<TextView>(R.id.tvBattleError)
        val tvEmpty = findViewById<TextView>(R.id.tvBattleEmpty)

        challengeAdapter = ChallengeAdapter { challenge ->
            viewModel.logProgress(challenge.challengeId)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = challengeAdapter

        findViewById<Button>(R.id.btnNewChallenge).setOnClickListener {
            showCreateChallengeDialog()
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
                    Toast.makeText(this@BattleActivity, message, Toast.LENGTH_LONG).show()
                } else {
                    tvError.visibility = View.GONE
                }
            }
        }
    }

    private fun showCreateChallengeDialog() {

        val dialogView = layoutInflater.inflate(R.layout.dialog_create_challenge, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val etTitle = dialogView.findViewById<EditText>(R.id.etChallengeTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etChallengeDescription)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerChallengeType)
        val etTarget = dialogView.findViewById<EditText>(R.id.etChallengeTarget)
        val etXpReward = dialogView.findViewById<EditText>(R.id.etChallengeXpReward)
        val etEndDate = dialogView.findViewById<EditText>(R.id.etChallengeEndDate)
        val btnLaunch = dialogView.findViewById<Button>(R.id.btnLaunchChallenge)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelChallenge)

        val typeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            typeLabels
        )
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = typeAdapter

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

            if (title.isEmpty()) {
                etTitle.error = "Enter a title"
                return@setOnClickListener
            }

            if (target == null || target <= 0) {
                etTarget.error = "Enter a valid target"
                return@setOnClickListener
            }

            if (xpReward == null || xpReward <= 0) {
                etXpReward.error = "Enter a valid XP reward"
                return@setOnClickListener
            }

            if (endDate.isEmpty()) {
                etEndDate.error = "Enter an end date"
                return@setOnClickListener
            }

            val sessionManager = SessionManager(this)
            val userId = sessionManager.getUserId()

            if (userId == -1) {
                Toast.makeText(this, "User session not found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = CreateChallengeRequest(
                title = title,
                description = description,
                type = typeCode,
                target = target,
                xpReward = xpReward,
                endDate = "${endDate}T00:00:00Z",
                createdByUserId = userId
            )

            btnLaunch.isEnabled = false
            btnLaunch.text = "LAUNCHING..."

            viewModel.createChallenge(request) {

                runOnUiThread {
                    Toast.makeText(this, "Challenge launched!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        dialog.show()
    }
}