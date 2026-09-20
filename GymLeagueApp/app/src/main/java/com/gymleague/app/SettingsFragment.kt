package com.gymleague.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.gymleague.app.models.UserSettings
import com.gymleague.app.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(
    R.layout.fragment_settings
) {

    companion object {
        private const val TAG = "GymLeagueSettings"
    }

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvSettingsStatus: TextView

    private lateinit var switchNotifications: SwitchMaterial

    private lateinit var radioGroupAppearance: RadioGroup
    private lateinit var radioSystem: RadioButton
    private lateinit var radioLight: RadioButton
    private lateinit var radioDark: RadioButton

    private lateinit var spinnerLanguage: Spinner

    private lateinit var settingsProgressBar: ProgressBar
    private lateinit var btnSaveSettings: Button
    private lateinit var btnLogout: Button

    private var currentSettings: UserSettings? = null

    private lateinit var fragmentScope: CoroutineScope

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        fragmentScope =
            CoroutineScope(
                SupervisorJob() +
                        Dispatchers.Main
            )

        settingsRepository =
            SettingsRepository()

        sessionManager =
            SessionManager(
                requireContext()
            )

        firebaseAuth =
            FirebaseAuth.getInstance()

        bindViews(view)

        setupAccountDetails()

        setupLanguageSpinner()

        btnSaveSettings.setOnClickListener {
            saveSettings()
        }

        btnLogout.setOnClickListener {
            logout()
        }

        loadSettings()
    }

    private fun bindViews(
        view: View
    ) {

        tvUsername =
            view.findViewById(
                R.id.tvUsername
            )

        tvEmail =
            view.findViewById(
                R.id.tvEmail
            )

        tvSettingsStatus =
            view.findViewById(
                R.id.tvSettingsStatus
            )

        switchNotifications =
            view.findViewById(
                R.id.switchNotifications
            )

        radioGroupAppearance =
            view.findViewById(
                R.id.radioGroupAppearance
            )

        radioSystem =
            view.findViewById(
                R.id.radioSystem
            )

        radioLight =
            view.findViewById(
                R.id.radioLight
            )

        radioDark =
            view.findViewById(
                R.id.radioDark
            )

        spinnerLanguage =
            view.findViewById(
                R.id.spinnerLanguage
            )

        settingsProgressBar =
            view.findViewById(
                R.id.settingsProgressBar
            )

        btnSaveSettings =
            view.findViewById(
                R.id.btnSaveSettings
            )

        btnLogout =
            view.findViewById(
                R.id.btnLogout
            )
    }

    private fun setupAccountDetails() {

        val username =
            sessionManager.getUsername()
                ?: "GymLeague User"

        val email =
            sessionManager.getEmail()
                ?: "No email available"

        tvUsername.text =
            username

        tvEmail.text =
            email
    }

    private fun setupLanguageSpinner() {

        val languages =
            listOf(
                "English",
                "Afrikaans"
            )

        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                languages
            )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerLanguage.adapter =
            adapter
    }

    private fun loadSettings() {

        val userId =
            sessionManager.getUserId()

        if (userId == -1) {

            Log.e(
                TAG,
                "No GymLeague user session found"
            )

            logout()

            return
        }

        showLoading(true)

        fragmentScope.launch {

            val result =
                settingsRepository
                    .getSettingsForUser(
                        userId
                    )

            result
                .onSuccess { settings ->

                    if (settings != null) {

                        currentSettings =
                            settings

                        displaySettings(
                            settings
                        )

                        Log.d(
                            TAG,
                            "Settings loaded for UserId: $userId"
                        )

                        showLoading(false)

                    } else {

                        Log.d(
                            TAG,
                            "No settings found. Creating default settings."
                        )

                        createDefaultSettings(
                            userId
                        )
                    }
                }
                .onFailure { error ->

                    showLoading(false)

                    Log.e(
                        TAG,
                        "Failed to load settings",
                        error
                    )

                    showStatus(
                        "Unable to load settings."
                    )
                }
        }
    }

    private suspend fun createDefaultSettings(
        userId: Int
    ) {

        val defaultSettings =
            UserSettings(
                userSettingsId = 0,
                userId = userId,
                notificationsEnabled = true,
                appearanceMode = "system",
                language = "English"
            )

        val result =
            settingsRepository
                .createSettings(
                    defaultSettings
                )

        result
            .onSuccess { createdSettings ->

                currentSettings =
                    createdSettings

                displaySettings(
                    createdSettings
                )

                ThemePreferences
                    .saveAppearanceMode(
                        requireContext(),
                        createdSettings.appearanceMode
                    )

                showLoading(false)

                Log.d(
                    TAG,
                    "Default settings created"
                )
            }
            .onFailure { error ->

                showLoading(false)

                Log.e(
                    TAG,
                    "Failed to create default settings",
                    error
                )

                showStatus(
                    "Unable to create default settings."
                )
            }
    }

    private fun displaySettings(
        settings: UserSettings
    ) {

        switchNotifications.isChecked =
            settings.notificationsEnabled

        when (
            settings.appearanceMode.lowercase()
        ) {

            "light" -> {
                radioLight.isChecked = true
            }

            "dark" -> {
                radioDark.isChecked = true
            }

            else -> {
                radioSystem.isChecked = true
            }
        }

        val language =
            settings.language

        val adapter =
            spinnerLanguage.adapter

        for (
        index in 0 until adapter.count
        ) {

            if (
                adapter
                    .getItem(index)
                    .toString()
                    .equals(
                        language,
                        ignoreCase = true
                    )
            ) {

                spinnerLanguage.setSelection(
                    index
                )

                break
            }
        }
    }

    private fun saveSettings() {

        val userId =
            sessionManager.getUserId()

        if (userId == -1) {

            showStatus(
                "User session not found."
            )

            return
        }

        val appearanceMode =
            when (
                radioGroupAppearance
                    .checkedRadioButtonId
            ) {

                R.id.radioLight -> {
                    "light"
                }

                R.id.radioDark -> {
                    "dark"
                }

                else -> {
                    "system"
                }
            }

        val language =
            spinnerLanguage
                .selectedItem
                .toString()

        val existingSettings =
            currentSettings

        if (existingSettings == null) {

            showStatus(
                "Settings are still loading."
            )

            return
        }

        val updatedSettings =
            existingSettings.copy(
                userId = userId,
                notificationsEnabled =
                    switchNotifications.isChecked,
                appearanceMode =
                    appearanceMode,
                language =
                    language
            )

        showLoading(true)

        fragmentScope.launch {

            val result =
                settingsRepository
                    .updateSettings(
                        updatedSettings
                    )

            result
                .onSuccess {

                    currentSettings =
                        updatedSettings

                    showLoading(false)

                    showStatus(
                        "Settings saved successfully."
                    )

                    Log.d(
                        TAG,
                        "Settings updated successfully"
                    )

                    applyAppearance(
                        appearanceMode
                    )
                }
                .onFailure { error ->

                    showLoading(false)

                    Log.e(
                        TAG,
                        "Failed to update settings",
                        error
                    )

                    showStatus(
                        "Unable to save settings."
                    )
                }
        }
    }

    private fun applyAppearance(
        appearanceMode: String
    ) {

        ThemePreferences
            .saveAppearanceMode(
                requireContext(),
                appearanceMode
            )

        Log.d(
            TAG,
            "Appearance mode saved: $appearanceMode"
        )

        ThemePreferences
            .applyTheme(
                appearanceMode
            )
    }

    private fun showLoading(
        loading: Boolean
    ) {

        settingsProgressBar.visibility =
            if (loading) {
                View.VISIBLE
            } else {
                View.GONE
            }

        btnSaveSettings.isEnabled =
            !loading
    }

    private fun showStatus(
        message: String
    ) {

        tvSettingsStatus.text =
            message

        tvSettingsStatus.visibility =
            View.VISIBLE
    }

    private fun logout() {

        Log.d(
            TAG,
            "Logging user out"
        )

        firebaseAuth.signOut()

        sessionManager.clearSession()

        val intent =
            Intent(
                requireContext(),
                LoginActivity::class.java
            )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        fragmentScope.cancel()
    }
}