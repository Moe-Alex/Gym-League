package com.gymleague.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.gymleague.app.models.UserSettings
import com.gymleague.app.repository.SettingsRepository
import com.gymleague.app.repository.UserRepository
import com.gymleague.app.repository.WorkoutRepository
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
        private const val BIOMETRIC_TAG = "GymLeagueBiometric"
    }

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var userRepository: UserRepository
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvSettingsStatus: TextView

    private lateinit var tvAvatarInitial: TextView
    private lateinit var tvProfileLevel: TextView
    private lateinit var tvProfileXp: TextView
    private lateinit var tvProfileWorkoutCount: TextView

    private lateinit var switchNotifications: SwitchMaterial
    private lateinit var switchBiometric: SwitchMaterial

    private lateinit var radioGroupAppearance: RadioGroup
    private lateinit var radioSystem: RadioButton
    private lateinit var radioLight: RadioButton
    private lateinit var radioDark: RadioButton

    private lateinit var spinnerLanguage: Spinner

    private lateinit var settingsProgressBar: ProgressBar
    private lateinit var btnSaveSettings: Button
    private lateinit var btnLogout: Button

    private var currentSettings: UserSettings? = null

    /*
     * Used when we change the biometric switch ourselves.
     * Without this flag, setting isChecked in code would trigger
     * the listener and accidentally open the biometric prompt.
     */
    private var suppressBiometricSwitchCallback = false

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

        userRepository =
            UserRepository()

        workoutRepository =
            WorkoutRepository()

        sessionManager =
            SessionManager(
                requireContext()
            )

        firebaseAuth =
            FirebaseAuth.getInstance()

        bindViews(view)

        setupAccountDetails()

        setupLanguageSpinner()

        setupBiometricSwitch()

        btnSaveSettings.setOnClickListener {
            saveSettings()
        }

        btnLogout.setOnClickListener {
            logout()
        }

        loadSettings()

        loadProfileStats()
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

        tvAvatarInitial =
            view.findViewById(
                R.id.tvAvatarInitial
            )

        tvProfileLevel =
            view.findViewById(
                R.id.tvProfileLevel
            )

        tvProfileXp =
            view.findViewById(
                R.id.tvProfileXp
            )

        tvProfileWorkoutCount =
            view.findViewById(
                R.id.tvProfileWorkoutCount
            )

        switchNotifications =
            view.findViewById(
                R.id.switchNotifications
            )

        switchBiometric =
            view.findViewById(
                R.id.switchBiometric
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
                ?: getString(
                    R.string.generic_user
                )

        val email =
            sessionManager.getEmail()
                ?: getString(
                    R.string.no_email
                )

        tvUsername.text =
            username

        tvEmail.text =
            email

        tvAvatarInitial.text =
            username
                .firstOrNull()
                ?.uppercase()
                ?: "?"
    }

    private fun loadProfileStats() {

        val userId =
            sessionManager.getUserId()

        if (userId == -1) {
            return
        }

        fragmentScope.launch {

            val result =
                userRepository.getUser(
                    userId
                )

            result.onSuccess { user ->

                tvProfileLevel.text =
                    getString(
                        R.string.level_dynamic,
                        user.level
                    )

                tvProfileXp.text =
                    user.xp.toString()
            }

            result.onFailure { error ->

                Log.e(
                    TAG,
                    "Failed to load profile stats: ${error.message}"
                )

                tvProfileLevel.text =
                    getString(
                        R.string.level_unknown
                    )

                tvProfileXp.text =
                    "—"
            }
        }

        fragmentScope.launch {

            val result =
                workoutRepository
                    .getWorkoutsForUser(
                        userId
                    )

            result.onSuccess { workouts ->

                tvProfileWorkoutCount.text =
                    workouts.size.toString()
            }

            result.onFailure { error ->

                Log.e(
                    TAG,
                    "Failed to load workout count: ${error.message}"
                )

                tvProfileWorkoutCount.text =
                    "—"
            }
        }
    }

    private fun setupLanguageSpinner() {

        val languages =
            listOf(
                "English",
                "Afrikaans",
                "isiZulu"
            )

        val adapter =
            object : ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                languages
            ) {

                override fun getView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {

                    val view =
                        super.getView(
                            position,
                            convertView,
                            parent
                        )

                    val textView =
                        view as TextView

                    textView.setTextColor(
                        android.graphics.Color.WHITE
                    )

                    return view
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {

                    val view =
                        super.getDropDownView(
                            position,
                            convertView,
                            parent
                        )

                    val textView =
                        view as TextView

                    textView.setTextColor(
                        android.graphics.Color.WHITE
                    )

                    textView.setBackgroundColor(
                        android.graphics.Color.rgb(
                            38,
                            35,
                            65
                        )
                    )

                    return view
                }
            }

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerLanguage.adapter =
            adapter
    }

    /**
     * Loads the locally saved biometric preference and listens for
     * the user turning biometric unlock on or off.
     *
     * When enabling it, GymLeague requires one successful biometric
     * authentication first. This prevents the app from enabling a lock
     * that has never been confirmed to work on the device.
     */
    private fun setupBiometricSwitch() {

        suppressBiometricSwitchCallback =
            true

        switchBiometric.isChecked =
            sessionManager
                .isBiometricEnabled()

        suppressBiometricSwitchCallback =
            false

        switchBiometric
            .setOnCheckedChangeListener { _, isChecked ->

                if (
                    suppressBiometricSwitchCallback
                ) {
                    return@setOnCheckedChangeListener
                }

                if (isChecked) {

                    checkBiometricAvailabilityAndEnable()

                } else {

                    sessionManager
                        .setBiometricEnabled(
                            false
                        )

                    Log.d(
                        BIOMETRIC_TAG,
                        "Biometric unlock disabled by user"
                    )

                    showStatus(
                        getString(
                            R.string.biometric_disabled
                        )
                    )
                }
            }
    }

    /**
     * Checks whether this phone can currently perform strong
     * biometric authentication.
     */
    private fun checkBiometricAvailabilityAndEnable() {

        val biometricManager =
            BiometricManager.from(
                requireContext()
            )

        when (
            biometricManager.canAuthenticate(
                BiometricManager
                    .Authenticators
                    .BIOMETRIC_STRONG
            )
        ) {

            BiometricManager
                .BIOMETRIC_SUCCESS -> {

                Log.d(
                    BIOMETRIC_TAG,
                    "Biometric authentication available"
                )

                showEnableBiometricPrompt()
            }

            BiometricManager
                .BIOMETRIC_ERROR_NO_HARDWARE -> {

                Log.w(
                    BIOMETRIC_TAG,
                    "No biometric hardware available"
                )

                revertBiometricSwitch()

                showStatus(
                    getString(
                        R.string.biometric_not_available
                    )
                )
            }

            BiometricManager
                .BIOMETRIC_ERROR_HW_UNAVAILABLE -> {

                Log.w(
                    BIOMETRIC_TAG,
                    "Biometric hardware temporarily unavailable"
                )

                revertBiometricSwitch()

                showStatus(
                    getString(
                        R.string.biometric_temporarily_unavailable
                    )
                )
            }

            BiometricManager
                .BIOMETRIC_ERROR_NONE_ENROLLED -> {

                Log.w(
                    BIOMETRIC_TAG,
                    "No biometric credential enrolled"
                )

                revertBiometricSwitch()

                showBiometricEnrollmentDialog()
            }

            else -> {

                Log.w(
                    BIOMETRIC_TAG,
                    "Biometric authentication unavailable"
                )

                revertBiometricSwitch()

                showStatus(
                    getString(
                        R.string.biometric_not_available
                    )
                )
            }
        }
    }

    /**
     * Shows Android's real system biometric prompt.
     *
     * The biometric preference is saved only after authentication
     * succeeds.
     */
    private fun showEnableBiometricPrompt() {

        val executor =
            ContextCompat.getMainExecutor(
                requireContext()
            )

        val biometricPrompt =
            BiometricPrompt(
                this,
                executor,
                object :
                    BiometricPrompt.AuthenticationCallback() {

                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        super.onAuthenticationSucceeded(
                            result
                        )

                        sessionManager
                            .setBiometricEnabled(
                                true
                            )

                        setBiometricSwitchChecked(
                            true
                        )

                        Log.d(
                            BIOMETRIC_TAG,
                            "Biometric authentication succeeded and unlock was enabled"
                        )

                        showStatus(
                            getString(
                                R.string.biometric_enabled
                            )
                        )
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()

                        /*
                         * Android keeps the prompt open after a normal
                         * failed fingerprint/face attempt, allowing the
                         * user to retry.
                         */
                        Log.w(
                            BIOMETRIC_TAG,
                            "Biometric authentication attempt failed"
                        )

                        showStatus(
                            getString(
                                R.string.biometric_auth_failed
                            )
                        )
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence
                    ) {
                        super.onAuthenticationError(
                            errorCode,
                            errString
                        )

                        sessionManager
                            .setBiometricEnabled(
                                false
                            )

                        revertBiometricSwitch()

                        when (errorCode) {

                            BiometricPrompt
                                .ERROR_NEGATIVE_BUTTON,

                            BiometricPrompt
                                .ERROR_USER_CANCELED,

                            BiometricPrompt
                                .ERROR_CANCELED -> {

                                Log.d(
                                    BIOMETRIC_TAG,
                                    "Biometric authentication cancelled"
                                )

                                showStatus(
                                    getString(
                                        R.string.biometric_auth_cancelled
                                    )
                                )
                            }

                            BiometricPrompt
                                .ERROR_LOCKOUT,

                            BiometricPrompt
                                .ERROR_LOCKOUT_PERMANENT -> {

                                Log.w(
                                    BIOMETRIC_TAG,
                                    "Biometric authentication locked out"
                                )

                                showStatus(
                                    getString(
                                        R.string.biometric_auth_error
                                    )
                                )
                            }

                            else -> {

                                Log.e(
                                    BIOMETRIC_TAG,
                                    "Biometric error code: $errorCode"
                                )

                                showStatus(
                                    getString(
                                        R.string.biometric_auth_error
                                    )
                                )
                            }
                        }
                    }
                }
            )

        val promptInfo =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(
                    getString(
                        R.string.biometric_prompt_title
                    )
                )
                .setSubtitle(
                    getString(
                        R.string.biometric_prompt_subtitle
                    )
                )
                .setAllowedAuthenticators(
                    BiometricManager
                        .Authenticators
                        .BIOMETRIC_STRONG
                )
                .setNegativeButtonText(
                    getString(
                        R.string.biometric_cancel
                    )
                )
                .build()

        biometricPrompt.authenticate(
            promptInfo
        )
    }

    /**
     * Gives the user a helpful route to Android's biometric
     * enrollment settings instead of failing or crashing.
     */
    private fun showBiometricEnrollmentDialog() {

        AlertDialog
            .Builder(
                requireContext()
            )
            .setTitle(
                getString(
                    R.string.biometric_setup
                )
            )
            .setMessage(
                getString(
                    R.string.biometric_setup_message
                )
            )
            .setPositiveButton(
                getString(
                    R.string.biometric_setup
                )
            ) { _, _ ->

                openBiometricEnrollmentSettings()
            }
            .setNegativeButton(
                getString(
                    R.string.biometric_cancel
                ),
                null
            )
            .show()
    }

    private fun openBiometricEnrollmentSettings() {

        try {

            val intent =
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.R
                ) {

                    Intent(
                        Settings.ACTION_BIOMETRIC_ENROLL
                    ).apply {

                        putExtra(
                            Settings
                                .EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BiometricManager
                                .Authenticators
                                .BIOMETRIC_STRONG
                        )
                    }

                } else {

                    Intent(
                        Settings.ACTION_SECURITY_SETTINGS
                    )
                }

            startActivity(intent)

        } catch (e: Exception) {

            Log.e(
                BIOMETRIC_TAG,
                "Unable to open biometric enrollment settings",
                e
            )

            showStatus(
                getString(
                    R.string.biometric_not_enrolled
                )
            )
        }
    }

    private fun revertBiometricSwitch() {

        setBiometricSwitchChecked(
            false
        )
    }

    private fun setBiometricSwitchChecked(
        checked: Boolean
    ) {

        suppressBiometricSwitchCallback =
            true

        switchBiometric.isChecked =
            checked

        suppressBiometricSwitchCallback =
            false
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
                            "Settings loaded successfully"
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
                        getString(
                            R.string.settings_load_error
                        )
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
                    getString(
                        R.string.settings_create_error
                    )
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
                radioLight.isChecked =
                    true
            }

            "dark" -> {
                radioDark.isChecked =
                    true
            }

            else -> {
                radioSystem.isChecked =
                    true
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

                spinnerLanguage
                    .setSelection(
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
                getString(
                    R.string.session_missing
                )
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
                getString(
                    R.string.settings_loading
                )
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
                        getString(
                            R.string.settings_saved
                        )
                    )

                    Log.d(
                        TAG,
                        "Settings updated successfully"
                    )

                    applyAppearance(
                        appearanceMode
                    )

                    applyLanguage(
                        language
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
                        getString(
                            R.string.settings_save_error
                        )
                    )
                }
        }
    }

    private fun applyLanguage(
        language: String
    ) {

        val languageCode =
            when (language) {

                "Afrikaans" -> {
                    "af"
                }

                "isiZulu" -> {
                    "zu"
                }

                else -> {
                    "en"
                }
            }

        val currentCode =
            AppCompatDelegate
                .getApplicationLocales()
                .toLanguageTags()

        if (
            currentCode != languageCode
        ) {

            AppCompatDelegate
                .setApplicationLocales(
                    LocaleListCompat
                        .forLanguageTags(
                            languageCode
                        )
                )
        }

        Log.d(
            TAG,
            "Language changed to: $language"
        )
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

        // Stop this phone receiving the user's push notifications.
        PushTokenManager
            .unregisterCurrentToken()

        firebaseAuth.signOut()

        /*
         * clearSession() also disables biometric unlock.
         */
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