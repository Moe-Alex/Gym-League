package com.gymleague.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gymleague.app.repository.UserRepository
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    companion object {
        private const val TAG = "GymLeagueHome"
    }

    private val userRepository = UserRepository()
    private lateinit var sessionManager: SessionManager

    private lateinit var tvWelcome: TextView
    private lateinit var tvLevel: TextView
    private lateinit var tvXp: TextView

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

        tvWelcome.text = "Welcome back, ${sessionManager.getUsername()}!"

        loadUserStats()

        view.findViewById<Button>(R.id.btnQuickTrain).setOnClickListener {
            startActivity(Intent(requireContext(), ExerciseLibraryActivity::class.java))
        }

        view.findViewById<Button>(R.id.btnQuickSquad).setOnClickListener {
            startActivity(Intent(requireContext(), SquadActivity::class.java))
        }

        view.findViewById<Button>(R.id.btnQuickRanks).setOnClickListener {
            startActivity(Intent(requireContext(), PersonalRecordsActivity::class.java))
        }

        view.findViewById<Button>(R.id.btnQuickBattle).setOnClickListener {
            showComingSoon("Battle Arena")
        }

        view.findViewById<Button>(R.id.btnQuickAchievements).setOnClickListener {
            showComingSoon("Achievements")
        }

        view.findViewById<Button>(R.id.btnQuickSchedule).setOnClickListener {
            showComingSoon("Schedule")
        }

        view.findViewById<Button>(R.id.btnQuickRecovery).setOnClickListener {
            showComingSoon("Recovery Helper")
        }

        view.findViewById<Button>(R.id.btnQuickProgress).setOnClickListener {
            showComingSoon("Progress")
        }
    }

    private fun loadUserStats() {
        val userId = sessionManager.getUserId()

        viewLifecycleOwner.lifecycleScope.launch {
            val result = userRepository.getUser(userId)

            result.onSuccess { user ->
                Log.d(TAG, "Loaded stats for UserId: $userId")
                tvLevel.text = "Level ${user.level}"
                tvXp.text = "${user.xp} XP"
            }

            result.onFailure { error ->
                Log.e(TAG, "Failed to load user stats: ${error.message}")
                tvLevel.text = "Level —"
                tvXp.text = "— XP"
            }
        }
    }

    private fun showComingSoon(featureName: String) {
        Toast.makeText(requireContext(), "$featureName is coming soon!", Toast.LENGTH_SHORT).show()
    }
}