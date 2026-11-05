package com.gymleague.app

import android.content.Intent
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
import androidx.appcompat.app.AlertDialog
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
    private lateinit var btnLeaveSquad: Button
    private lateinit var btnFindUsers: Button
    private lateinit var userSearchResults: LinearLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_squad)

        BottomNavHelper.setup(this, findViewById(R.id.bottomNavigation), NavTab.SQUAD)

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
        btnFindUsers = findViewById(R.id.btnFindUsers)
        userSearchResults = findViewById(R.id.userSearchResults)
        btnLeaveSquad = findViewById(R.id.btnLeaveSquad)

        btnRefresh.setOnClickListener { loadSquads() }

        btnCreateSquad.setOnClickListener {
            val name = etCreateSquadName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, getString(R.string.squad_name_error), Toast.LENGTH_SHORT).show()
            } else {
                createAndJoinSquad(name)
            }
        }

        btnInviteMember.setOnClickListener {
            val username = etInviteUsername.text.toString().trim()
            if (username.isEmpty()) {
                Toast.makeText(this, getString(R.string.squad_username_error), Toast.LENGTH_SHORT).show()
            } else {
                inviteByUsername(username)
            }
        }
        btnFindUsers.setOnClickListener {
            findRegisteredUsers()
        }

        btnLeaveSquad.setOnClickListener {
            confirmLeaveSquad()
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
                showEmptyState(getString(R.string.squad_load_error))
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
            showEmptyState(getString(R.string.squad_not_member))
            return
        }

        etCreateSquadName.visibility = View.GONE
        btnCreateSquad.visibility = View.GONE
        tvEmptySquad.visibility = View.GONE

        etInviteUsername.visibility = View.VISIBLE
        btnFindUsers.visibility = View.VISIBLE
        btnInviteMember.visibility = View.VISIBLE
        btnLeaveSquad.visibility = View.VISIBLE

        tvSquadName.text = mySquad.squadName
        tvSquadName.visibility = View.VISIBLE

        tvSharedStreak.text = getString(R.string.home_shared_streak, mySquad.sharedStreak)
        tvSharedStreak.visibility = View.VISIBLE

        membersContainer.removeAllViews()

        for (member in mySquad.members) {

            val memberCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL

                setPadding(
                    32,
                    24,
                    32,
                    24
                )

                background = getDrawable(
                    R.drawable.bg_exercise_card
                )
            }

            // Username
            val usernameText = TextView(this).apply {

                text = "⚡ ${member.user?.username ?: getString(R.string.squad_unknown_user)}"

                textSize = 16f

                setTextColor(
                    android.graphics.Color.WHITE
                )

                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD
                )
            }

            // Streak
            val streakText = TextView(this).apply {

                text = "🔥 ${member.currentStreak} day streak"

                textSize = 13f

                setTextColor(
                    android.graphics.Color.parseColor("#B45CFF")
                )

                setPadding(
                    0,
                    10,
                    0,
                    0
                )
            }

            memberCard.addView(usernameText)
            memberCard.addView(streakText)

            // Only show challenge for OTHER members
            if (member.userId != sessionManager.getUserId()) {

                val challengeButton = Button(this).apply {

                    text = "CHALLENGE"

                    textSize = 12f

                    setTextColor(
                        android.graphics.Color.parseColor("#25D9F5")
                    )

                    backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#21152C")
                        )

                    setOnClickListener {

                        // Open the Battle Arena's new-challenge form, aimed at this member
                        val battleIntent = Intent(
                            this@SquadActivity,
                            BattleActivity::class.java
                        ).apply {
                            putExtra(BattleActivity.EXTRA_OPPONENT_ID, member.userId)
                            putExtra(
                                BattleActivity.EXTRA_OPPONENT_NAME,
                                member.user?.username ?: ""
                            )
                        }

                        startActivity(battleIntent)
                    }
                }

                val challengeParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                challengeParams.topMargin = 12

                challengeButton.layoutParams =
                    challengeParams

                memberCard.addView(challengeButton)
            }

            // Space between member cards
            val cardParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

            cardParams.bottomMargin = 16

            memberCard.layoutParams =
                cardParams

            membersContainer.addView(memberCard)
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
                    Toast.makeText(this@SquadActivity, getString(R.string.squad_created), Toast.LENGTH_SHORT).show()
                    etCreateSquadName.text.clear()
                    loadSquads()
                }

                joinResult.onFailure { error ->
                    Log.e(TAG, "Failed to join own squad: ${error.message}")
                    Toast.makeText(this@SquadActivity, getString(R.string.squad_join_failed, error.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }

            createResult.onFailure { error ->
                Log.e(TAG, "Failed to create squad: ${error.message}")
                Toast.makeText(this@SquadActivity, getString(R.string.squad_create_failed, error.message ?: ""), Toast.LENGTH_LONG).show()
            }

            showLoading(false)
        }
    }

    private fun inviteByUsername(username: String) {
        val squad = currentSquad
        if (squad == null) {
            Toast.makeText(this, getString(R.string.squad_required), Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            val searchResult = userRepository.getUserByUsername(username)

            searchResult.onSuccess { foundUser ->
                if (foundUser == null) {
                    Toast.makeText(this@SquadActivity, getString(R.string.squad_user_not_found, username), Toast.LENGTH_LONG).show()
                    showLoading(false)
                    return@onSuccess
                }

                val addResult = squadRepository.addMember(squad.squadId, foundUser.userId)

                addResult.onSuccess {
                    Log.d(TAG, "Added ${foundUser.username} to squad ${squad.squadId}")
                    Toast.makeText(this@SquadActivity, getString(R.string.squad_member_added, foundUser.username), Toast.LENGTH_SHORT).show()
                    etInviteUsername.text.clear()
                    loadSquads()
                }

                addResult.onFailure { error ->
                    Log.e(TAG, "Failed to add member: ${error.message}")
                    Toast.makeText(this@SquadActivity, getString(R.string.squad_add_failed, error.message ?: ""), Toast.LENGTH_LONG).show()
                }

                showLoading(false)
            }

            searchResult.onFailure { error ->
                Log.e(TAG, "Username search failed: ${error.message}")
                Toast.makeText(this@SquadActivity, getString(R.string.squad_search_failed, error.message ?: ""), Toast.LENGTH_LONG).show()
                showLoading(false)
            }
        }
    }
    private fun findRegisteredUsers() {

        val squad = currentSquad

        if (squad == null) {
            Toast.makeText(
                this,
                getString(R.string.squad_required),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val searchText =
            etInviteUsername.text.toString().trim()

        showLoading(true)

        lifecycleScope.launch {

            val result = userRepository.getUsers()

            result.onSuccess { users ->

                // Current logged-in user
                val currentUserId =
                    sessionManager.getUserId()

                // IDs of users already in this squad
                val existingMemberIds =
                    squad.members.map { it.userId }

                // Filter registered users
                val matchingUsers =
                    users.filter { user ->

                        val matchesSearch =
                            searchText.isBlank() ||
                                    user.username.contains(
                                        searchText,
                                        ignoreCase = true
                                    )

                        val isNotCurrentUser =
                            user.userId != currentUserId

                        val isNotAlreadyMember =
                            user.userId !in existingMemberIds

                        matchesSearch &&
                                isNotCurrentUser &&
                                isNotAlreadyMember
                    }

                userSearchResults.removeAllViews()

                if (matchingUsers.isEmpty()) {

                    val noUsersText =
                        TextView(this@SquadActivity).apply {

                            text = getString(R.string.squad_no_users)

                            setTextColor(
                                android.graphics.Color.parseColor(
                                    "#918A9D"
                                )
                            )

                            textSize = 14f

                            setPadding(
                                12,
                                20,
                                12,
                                20
                            )
                        }

                    userSearchResults.addView(noUsersText)

                } else {

                    for (user in matchingUsers) {

                        val userButton =
                            Button(this@SquadActivity).apply {

                                text = "⚡ ${user.username}"

                                setTextColor(
                                    android.graphics.Color.WHITE
                                )

                                setBackgroundColor(
                                    android.graphics.Color.parseColor(
                                        "#21152C"
                                    )
                                )

                                isAllCaps = false

                                setOnClickListener {

                                    // Put selected username
                                    // into invite field
                                    etInviteUsername.setText(
                                        user.username
                                    )

                                    Toast.makeText(
                                        this@SquadActivity,
                                        getString(R.string.squad_user_selected, user.username),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        val params =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )

                        params.setMargins(
                            0,
                            5,
                            0,
                            5
                        )

                        userButton.layoutParams = params

                        userSearchResults.addView(
                            userButton
                        )
                    }
                }

                userSearchResults.visibility =
                    View.VISIBLE
            }

            result.onFailure { error ->

                Log.e(
                    TAG,
                    "Failed to load registered users",
                    error
                )

                Toast.makeText(
                    this@SquadActivity,
                    getString(R.string.squad_load_users_failed, error.message ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            }

            showLoading(false)
        }
    }

    private fun confirmLeaveSquad() {
        val squad = currentSquad ?: return

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.squad_leave_title))
            .setMessage(getString(R.string.squad_leave_message, squad.squadName))
            .setPositiveButton(getString(R.string.squad_leave_confirm)) { _, _ -> leaveSquad(squad.squadId) }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun leaveSquad(squadId: Int) {
        showLoading(true)

        lifecycleScope.launch {
            val userId = sessionManager.getUserId()
            val result = squadRepository.leaveSquad(squadId, userId)

            result.onSuccess {
                Log.d(TAG, "Left squad $squadId")
                Toast.makeText(this@SquadActivity, getString(R.string.squad_left), Toast.LENGTH_SHORT).show()
                currentSquad = null
                loadSquads()
            }

            result.onFailure { error ->
                Log.e(TAG, "Failed to leave squad: ${error.message}")
                Toast.makeText(this@SquadActivity, getString(R.string.squad_leave_failed, error.message ?: ""), Toast.LENGTH_LONG).show()
            }

            showLoading(false)
        }
    }

    private fun showEmptyState(message: String) {
        tvSquadName.visibility = View.GONE
        tvSharedStreak.visibility = View.GONE
        membersContainer.removeAllViews()

        etInviteUsername.visibility = View.GONE
        btnInviteMember.visibility = View.GONE
        btnLeaveSquad.visibility = View.GONE

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
        btnLeaveSquad.isEnabled = !isLoading
    }
}