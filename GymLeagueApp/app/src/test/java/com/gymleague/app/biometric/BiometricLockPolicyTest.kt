package com.gymleague.app.biometric

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricLockPolicyTest {

    @Test
    fun loggedInAndBiometricEnabled_requiresBiometric() {

        val result =
            BiometricLockPolicy.shouldRequireBiometric(
                isLoggedIn = true,
                biometricEnabled = true
            )

        assertTrue(result)
    }

    @Test
    fun loggedInButBiometricDisabled_doesNotRequireBiometric() {

        val result =
            BiometricLockPolicy.shouldRequireBiometric(
                isLoggedIn = true,
                biometricEnabled = false
            )

        assertFalse(result)
    }

    @Test
    fun loggedOutButBiometricEnabled_doesNotRequireBiometric() {

        val result =
            BiometricLockPolicy.shouldRequireBiometric(
                isLoggedIn = false,
                biometricEnabled = true
            )

        assertFalse(result)
    }

    @Test
    fun loggedOutAndBiometricDisabled_doesNotRequireBiometric() {

        val result =
            BiometricLockPolicy.shouldRequireBiometric(
                isLoggedIn = false,
                biometricEnabled = false
            )

        assertFalse(result)
    }
}