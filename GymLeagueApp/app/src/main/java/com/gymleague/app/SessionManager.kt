package com.gymleague.app

import android.content.Context

class SessionManager(context: Context) {

    companion object {
        private const val PREF_NAME = "gymleague_session"

        private const val KEY_USER_ID = "user_id"
        private const val KEY_FIREBASE_UID = "firebase_uid"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
    }

    private val preferences =
        context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )

    fun saveUserSession(
        userId: Int,
        firebaseUid: String,
        username: String,
        email: String
    ) {

        preferences.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_FIREBASE_UID, firebaseUid)
            .putString(KEY_USERNAME, username)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun getUserId(): Int {
        return preferences.getInt(
            KEY_USER_ID,
            -1
        )
    }

    fun getFirebaseUid(): String? {
        return preferences.getString(
            KEY_FIREBASE_UID,
            null
        )
    }

    fun getUsername(): String? {
        return preferences.getString(
            KEY_USERNAME,
            null
        )
    }

    fun getEmail(): String? {
        return preferences.getString(
            KEY_EMAIL,
            null
        )
    }

    fun isLoggedIn(): Boolean {
        return getUserId() != -1
    }

    fun clearSession() {
        preferences.edit()
            .clear()
            .apply()
    }
}