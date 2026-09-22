package com.gymleague.app

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gymleague.app.models.Squad
import com.gymleague.app.repository.SquadRepository
import com.gymleague.app.repository.UserRepository
import kotlinx.coroutines.launch

class SquadActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GymLeagueSquad"
    }

    private val squadRepository = SquadRepository()
    private val userRepository = UserRepository()
    private lateinit var sessionManager: SessionManager

    private var currentSquad: Squad? = null

    private lateinit var tvSquadName: TextView
    private lateinit var tvSharedStreak: TextView
    private lateinit var membersContainer: LinearLayout
    private lateinit var tvEmptySquad: TextView
    private lateinit var progressSquad: ProgressBar
    private lateinit var btnRefresh: Button
    private lateinit var etCreateSquadName: EditText
    private lateinit var btnCreateSquad: Button
    private lateinit var etInviteUsername: EditText
    private lateinit var btnInviteMember: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_squad)

        sessionManager = SessionManager(this)

        tvSquadName = findViewById(R.id.tvSquadName)
        tvSharedStreak = findViewById(R.id.tvSharedStreak)
        membersContainer = findViewById(R.id.membersContainer)
        tvEmptySquad = findViewById(R.id.tvEmptySquad)
        progressSquad = findViewById(R.id.progressSquad)
        btnRefresh = findViewById(R.id.btnRefreshSquad)
        etCreateSquadName = findViewById(R.id.etCreateSquadName)
        btnCreateSquad = findViewById(R.id.btnCreateSquad)
        etInviteUsername = findViewById(R.id.etInviteUsername)
        btnInviteMember = findViewById(R.id.btnInviteMember)

        btnRefresh.setOnClickListener { loadSquads() }

        btnCreateSquad.setOnClickListener {
            val name = etCreateSquadName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Enter a squad name first", Toast.LENGTH_SHORT).show()
            } else {
                createAndJoinSquad(name)
            }
        }

        btnInviteMember.setOnClickListener {
            val username = etInviteUsername.text.toString().trim()
            if (username.isEmpty()) {
                Toast.makeText(this, "Enter a username first", Toast.LENGTH_SHORT).show()
            } else {
                inviteByUsername(username)
            }
        }

        loadSquads()
    }

    private fun loadSquads() {
        showLoading(true)

        lifecycleScope.launch {
            val result = squadRepository.getSquads()

            result.onSuccess { squads ->
                Log.d(TAG, "Loaded ${squads.size} squad(s) from API")
                displaySquadForCurrentUser(squads)
            }

            result.onFailure { error ->
                Log.e(TAG, "Failed to load squads: ${error.message}")
                showEmptyState("Could not load squad data. Tap refresh to try again.")
            }

            showLoading(false)
        }
    }

    private fun displaySquadForCurrentUser(squads: List<Squad>) {
        val currentUserId = sessionManager.getUserId()

        val mySquad = squads.firstOrNull { squad ->
            squad.members.any { member -> member.userId == currentUserId }
        }

        currentSquad = mySquad

        if (mySquad == null) {
            showEmptyState("You're not part of a squad yet.")
            return
        }

        etCreateSquadName.visibility = View.GONE
        btnCreateSquad.visibility = View.GONE
        tvEmptySquad.visibility = View.GONE

        etInviteUsername.visibility = View.VISIBLE
        btnInviteMember.visibility = View.VISIBLE

        tvSquadName.text = mySquad.squadName
        tvSquadName.visibility = View.VISIBLE

        tvSharedStreak.text = "\uD83D\uDD25 Shared Streak: ${mySquad.sharedStreak} days"
        tvSharedStreak.visibility = View.VISIBLE

        membersContainer.removeAllViews()

        for (member in mySquad.members) {
            val memberRow = TextView(this).apply {
                text = "${member.user?.username ?: "Unknown user"} — ${member.currentStreak} day streak"
                textSize = 16f
                setPadding(0, 12, 0, 12)
                gravity = Gravity.START
            }
            membersContainer.addView(memberRow)
        }
    }

    private fun createAndJoinSquad(squadName: String) {
        showLoading(true)

        lifecycleScope.launch {
            val createResult = squadRepository.createSquad(squadName)

            createResult.onSuccess { squad ->
                Log.d(TAG, "Created squad '${squad.squadName}' with id ${squad.squadId}")

                val userId = sessionManager.getUserId()
                val joinResult = squadRepository.addMember(squad.squadId, userId)

                joinResult.onSuccess {
                    Toast.makeText(this@SquadActivity, "Squad created!", Toast.LENGTH_SHORT).show()
                    etCreateSquadName.text.clear()
                    loadSquads()
                }

                joinResult.onFailure { error ->
                    Log.e(TAG, "Failed to join own squad: ${error.message}")
                    Toast.makeText(this@SquadActivity, "Squad created, but couldn't join it: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }

            createResult.onFailure { error ->
                Log.e(TAG, "Failed to create squad: ${error.message}")
                Toast.makeText(this@SquadActivity, "Could not create squad: ${error.message}", Toast.LENGTH_LONG).show()
            }

            showLoading(false)
        }
    }

    private fun inviteByUsername(username: String) {
        val squad = currentSquad
        if (squad == null) {
            Toast.makeText(this, "You need a squad first", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            val searchResult = userRepository.getUserByUsername(username)

            searchResult.onSuccess { foundUser ->
                if (foundUser == null) {
                    Toast.makeText(this@SquadActivity, "No user found with username \"$username\"", Toast.LENGTH_LONG).show()
                    showLoading(false)
                    return@onSuccess
                }

                val addResult = squadRepository.addMember(squad.squadId, foundUser.userId)

                addResult.onSuccess {
                    Log.d(TAG, "Added ${foundUser.username} to squad ${squad.squadId}")
                    Toast.makeText(this@SquadActivity, "${foundUser.username} added to squad!", Toast.LENGTH_SHORT).show()
                    etInviteUsername.text.clear()
                    loadSquads()
                }

                addResult.onFailure { error ->
                    Log.e(TAG, "Failed to add member: ${error.message}")
                    Toast.makeText(this@SquadActivity, "Could not add member: ${error.message}", Toast.LENGTH_LONG).show()
                }

                showLoading(false)
            }

            searchResult.onFailure { error ->
                Log.e(TAG, "Username search failed: ${error.message}")
                Toast.makeText(this@SquadActivity, "Search failed: ${error.message}", Toast.LENGTH_LONG).show()
                showLoading(false)
            }
        }
    }

    private fun showEmptyState(message: String) {
        tvSquadName.visibility = View.GONE
        tvSharedStreak.visibility = View.GONE
        membersContainer.removeAllViews()

        etInviteUsername.visibility = View.GONE
        btnInviteMember.visibility = View.GONE

        etCreateSquadName.visibility = View.VISIBLE
        btnCreateSquad.visibility = View.VISIBLE

        tvEmptySquad.text = message
        tvEmptySquad.visibility = View.VISIBLE
    }

    private fun showLoading(isLoading: Boolean) {
        progressSquad.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnRefresh.isEnabled = !isLoading
        btnCreateSquad.isEnabled = !isLoading
        btnInviteMember.isEnabled = !isLoading
    }
}