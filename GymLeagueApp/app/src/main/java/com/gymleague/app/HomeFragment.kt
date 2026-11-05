package com.gymleague.app

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gymleague.app.repository.SquadRepository
import com.gymleague.app.repository.UserRepository
import com.gymleague.app.repository.WorkoutRepository
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    companion object {
        private const val TAG = "GymLeagueHome"
    }

    private val userRepository = UserRepository()
    private val workoutRepository = WorkoutRepository()
    private val squadRepository = SquadRepository()
    private lateinit var sessionManager: SessionManager

    private lateinit var tvWelcome: TextView
    private lateinit var tvLevel: TextView
    private lateinit var tvXp: TextView
    private lateinit var tvWorkoutCount: TextView
    private lateinit var squadSection: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvLevel = view.findViewById(R.id.tvLevel)
        tvXp = view.findViewById(R.id.tvXp)
        tvWorkoutCount = view.findViewById(R.id.tvWorkoutCount)
        squadSection = view.findViewById(R.id.squadSection)

        tvWelcome.text = sessionManager.getUsername() ?: getString(R.string.home_athlete)

        view.findViewById<TextView>(R.id.tvSquadView).setOnClickListener {
            startActivity(Intent(requireContext(), SquadActivity::class.java))
        }

        // Settings sits in the top-right corner of the home screen
        view.findViewById<View>(R.id.btnHomeSettings).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, SettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        loadUserStats()
        loadWorkoutCount()
        loadSquad()
    }

    private fun loadUserStats() {
        val userId = sessionManager.getUserId()

        viewLifecycleOwner.lifecycleScope.launch {
            val result = userRepository.getUser(userId)

            result.onSuccess { user ->
                Log.d(TAG, "Loaded stats for UserId: $userId")
                tvLevel.text = getString(R.string.level_dynamic , user.level)
                tvXp.text = user.xp.toString()
            }

            result.onFailure { error ->
                Log.e(TAG, "Failed to load user stats: ${error.message}")
                tvLevel.text = getString(R.string.level_unknown)
                tvXp.text = "—"
            }
        }
    }

    private fun loadWorkoutCount() {
        val userId = sessionManager.getUserId()

        viewLifecycleOwner.lifecycleScope.launch {
            val result = workoutRepository.getWorkoutsForUser(userId)

            result.onSuccess { workouts ->
                tvWorkoutCount.text = workouts.size.toString()
            }

            result.onFailure { error ->
                Log.e(TAG, "Failed to load workout count: ${error.message}")
                tvWorkoutCount.text = "—"
            }
        }
    }

    private fun loadSquad() {
        val currentUserId = sessionManager.getUserId()

        viewLifecycleOwner.lifecycleScope.launch {
            val result = squadRepository.getSquads()

            result.onSuccess { squads ->
                val mySquad = squads.firstOrNull { squad ->
                    squad.members.any { it.userId == currentUserId }
                }

                if (mySquad == null) {
                    showNoSquad()
                } else {
                    showSquad(mySquad.squadName, mySquad.sharedStreak, mySquad.members)
                }
            }

            result.onFailure { error ->
                Log.e(TAG, "Failed to load squad: ${error.message}")
                showNoSquad()
            }
        }
    }

    private fun showNoSquad() {
        squadSection.removeAllViews()

        val emptyText = TextView(requireContext()).apply {
            text = getString(R.string.home_no_squad)
            textSize = 13f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_secondary_dark))
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_dark)
            setPadding(32, 28, 32, 28)
        }

        squadSection.addView(emptyText)
    }

    private fun showSquad(
        squadName: String,
        sharedStreak: Int,
        members: List<com.gymleague.app.models.SquadMember>
    ) {
        squadSection.removeAllViews()

        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_dark)
            setPadding(32, 28, 32, 28)
        }

        val nameView = TextView(requireContext()).apply {
            text = squadName
            textSize = 16f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_dark))
        }

        val streakView = TextView(requireContext()).apply {
            text = getString(R.string.home_shared_streak , sharedStreak)
            textSize = 13f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.gym_gold))
            setPadding(0, 4, 0, 10)
        }

        card.addView(nameView)
        card.addView(streakView)

        for (member in members.take(3)) {
            val memberRow = TextView(requireContext()).apply {
                text = "${member.user?.username ?: getString(R.string.home_unknown_member)} — 🔥${member.currentStreak}"
                textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_secondary_dark))
                setPadding(0, 4, 0, 0)
            }
            card.addView(memberRow)
        }

        squadSection.addView(card)
    }
}