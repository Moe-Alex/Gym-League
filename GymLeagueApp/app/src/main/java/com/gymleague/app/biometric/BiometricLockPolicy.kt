package com.gymleague.app.biometric

/**
 * Contains the decision for whether GymLeague should ask the user
 * for biometric authentication before opening the main application.
 *
 * Keeping this rule separate from Android UI code means it can be
 * unit tested without needing an emulator or physical device.
 */
object BiometricLockPolicy {

    /**
     * GymLeague requires biometric authentication when:
     *
     * 1. A user already has an active GymLeague session.
     * 2. That user has enabled biometric unlock in Settings.
     *
     * We are using a cold-start lock for the POE. The biometric
     * prompt will therefore be checked when SplashActivity starts.
     */
    fun shouldRequireBiometric(
        isLoggedIn: Boolean,
        biometricEnabled: Boolean
    ): Boolean {

        return isLoggedIn && biometricEnabled
    }
}