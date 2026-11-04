package com.gymleague.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class MoreFragment : Fragment(
    R.layout.fragment_more
) {

    companion object {
        private const val TAG = "GymLeagueMore"
    }

    private lateinit var sessionManager: SessionManager

    private lateinit var tvMoreUsername: TextView
    private lateinit var tvMoreEmail: TextView
    private lateinit var btnOpenSettings: Button
    private lateinit var btnOpenMuscleVolume: Button

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        sessionManager =
            SessionManager(
                requireContext()
            )

        bindViews(view)

        displayUserDetails()

        btnOpenSettings.setOnClickListener {

            Log.d(
                TAG,
                "Settings selected"
            )

            openSettings()
        }

        btnOpenMuscleVolume.setOnClickListener {

            Log.d(
                TAG,
                "Muscle Volume selected"
            )

            startActivity(
                Intent(
                    requireContext(),
                    MuscleVolumeActivity::class.java
                )
            )
        }
    }

    private fun bindViews(
        view: View
    ) {

        tvMoreUsername =
            view.findViewById(
                R.id.tvMoreUsername
            )

        tvMoreEmail =
            view.findViewById(
                R.id.tvMoreEmail
            )

        btnOpenSettings =
            view.findViewById(
                R.id.btnOpenSettings
            )

        btnOpenMuscleVolume =
            view.findViewById(
                R.id.btnOpenMuscleVolume
            )
    }

    private fun displayUserDetails() {

        val username =
            sessionManager.getUsername()
                ?: "GymLeague User"

        val email =
            sessionManager.getEmail()
                ?: "No email available"

        tvMoreUsername.text =
            username

        tvMoreEmail.text =
            email

        Log.d(
            TAG,
            "User details displayed"
        )
    }

    private fun openSettings() {

        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                SettingsFragment()
            )
            .addToBackStack(null)
            .commit()
    }
}