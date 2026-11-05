package com.gymleague.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.gymleague.app.biometric.BiometricLockPolicy

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GymLeagueBiometric"
        private const val SPLASH_DELAY_MS = 1200L
    }

    private lateinit var sessionManager: SessionManager

    private val splashHandler =
        Handler(
            Looper.getMainLooper()
        )

    private val splashRunnable =
        Runnable {
            navigateNext()
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_splash
        )

        sessionManager =
            SessionManager(this)

        splashHandler.postDelayed(
            splashRunnable,
            SPLASH_DELAY_MS
        )
    }

    /**
     * Decides whether the user should go to LoginActivity,
     * MainActivity, or first complete biometric authentication.
     */
    private fun navigateNext() {

        val isLoggedIn =
            sessionManager.isLoggedIn()

        val biometricEnabled =
            sessionManager.isBiometricEnabled()

        val shouldRequireBiometric =
            BiometricLockPolicy
                .shouldRequireBiometric(
                    isLoggedIn = isLoggedIn,
                    biometricEnabled = biometricEnabled
                )

        Log.d(
            TAG,
            "Startup security check completed. Biometric required: $shouldRequireBiometric"
        )

        when {

            !isLoggedIn -> {
                openLogin()
            }

            shouldRequireBiometric -> {
                checkBiometricAvailability()
            }

            else -> {
                openMain()
            }
        }
    }

    /**
     * Checks whether the device is currently able to perform
     * strong biometric authentication before showing the prompt.
     */
    private fun checkBiometricAvailability() {

        val biometricManager =
            BiometricManager.from(this)

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
                    TAG,
                    "Biometric available at startup"
                )

                showBiometricPrompt()
            }

            BiometricManager
                .BIOMETRIC_ERROR_NO_HARDWARE -> {

                Log.w(
                    TAG,
                    "No biometric hardware available"
                )

                Toast.makeText(
                    this,
                    getString(
                        R.string.biometric_not_available
                    ),
                    Toast.LENGTH_LONG
                ).show()

                closeLockedApp()
            }

            BiometricManager
                .BIOMETRIC_ERROR_HW_UNAVAILABLE -> {

                Log.w(
                    TAG,
                    "Biometric hardware temporarily unavailable"
                )

                Toast.makeText(
                    this,
                    getString(
                        R.string.biometric_temporarily_unavailable
                    ),
                    Toast.LENGTH_LONG
                ).show()

                closeLockedApp()
            }

            BiometricManager
                .BIOMETRIC_ERROR_NONE_ENROLLED -> {

                Log.w(
                    TAG,
                    "No biometric credential enrolled"
                )

                showEnrollmentDialog()
            }

            else -> {

                Log.w(
                    TAG,
                    "Biometric authentication unavailable"
                )

                Toast.makeText(
                    this,
                    getString(
                        R.string.biometric_not_available
                    ),
                    Toast.LENGTH_LONG
                ).show()

                closeLockedApp()
            }
        }
    }

    /**
     * Shows Android's real system biometric authentication dialog.
     *
     * MainActivity is opened only after Android confirms
     * authentication was successful.
     */
    private fun showBiometricPrompt() {

        val executor =
            ContextCompat.getMainExecutor(
                this
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

                        Log.d(
                            TAG,
                            "Biometric authentication successful"
                        )

                        openMain()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()

                        /*
                         * A normal failed fingerprint or face attempt
                         * does not close the prompt. Android allows the
                         * user to try again.
                         */
                        Log.w(
                            TAG,
                            "Biometric authentication attempt failed"
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

                        when (errorCode) {

                            BiometricPrompt
                                .ERROR_NEGATIVE_BUTTON,

                            BiometricPrompt
                                .ERROR_USER_CANCELED,

                            BiometricPrompt
                                .ERROR_CANCELED -> {

                                Log.d(
                                    TAG,
                                    "Biometric authentication cancelled"
                                )

                                Toast.makeText(
                                    this@SplashActivity,
                                    getString(
                                        R.string.biometric_auth_cancelled
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            BiometricPrompt
                                .ERROR_LOCKOUT,

                            BiometricPrompt
                                .ERROR_LOCKOUT_PERMANENT -> {

                                Log.w(
                                    TAG,
                                    "Biometric authentication locked out"
                                )

                                Toast.makeText(
                                    this@SplashActivity,
                                    getString(
                                        R.string.biometric_auth_error
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            else -> {

                                Log.e(
                                    TAG,
                                    "Biometric startup error code: $errorCode"
                                )

                                Toast.makeText(
                                    this@SplashActivity,
                                    getString(
                                        R.string.biometric_auth_error
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        /*
                         * Authentication did not succeed.
                         * MainActivity must therefore remain inaccessible.
                         */
                        closeLockedApp()
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

        Log.d(
            TAG,
            "Showing biometric startup prompt"
        )

        biometricPrompt.authenticate(
            promptInfo
        )
    }

    /**
     * If biometrics were removed from the phone after the user
     * previously enabled biometric unlock, explain what happened
     * and offer a shortcut to Android's security settings.
     */
    private fun showEnrollmentDialog() {

        AlertDialog
            .Builder(this)
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
                )
            ) { _, _ ->

                closeLockedApp()
            }
            .setCancelable(false)
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

            /*
             * GymLeague remains locked.
             * After enrolment the user can reopen the app and
             * authenticate normally.
             */
            finish()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Unable to open biometric enrollment settings",
                e
            )

            Toast.makeText(
                this,
                getString(
                    R.string.biometric_not_enrolled
                ),
                Toast.LENGTH_LONG
            ).show()

            closeLockedApp()
        }
    }

    private fun openLogin() {

        Log.d(
            TAG,
            "No active session. Opening login."
        )

        val intent =
            Intent(
                this,
                LoginActivity::class.java
            )

        startActivity(intent)

        finish()
    }

    private fun openMain() {

        Log.d(
            TAG,
            "Startup security passed. Opening GymLeague."
        )

        val intent =
            Intent(
                this,
                MainActivity::class.java
            )

        startActivity(intent)

        finish()
    }

    /**
     * Closes the locked entry screen without opening app data.
     *
     * The user can reopen GymLeague and try biometric
     * authentication again.
     */
    private fun closeLockedApp() {

        Log.d(
            TAG,
            "GymLeague remains locked"
        )

        finishAndRemoveTask()
    }

    override fun onDestroy() {
        super.onDestroy()

        splashHandler.removeCallbacks(
            splashRunnable
        )
    }
}