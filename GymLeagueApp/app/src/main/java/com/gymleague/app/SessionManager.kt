package com.gymleague.app

import android.content.Context
import android.util.Log
import com.gymleague.app.database.GymLeagueDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SessionManager(context: Context) {

    companion object {
        private const val PREF_NAME = "gymleague_session"

        private const val KEY_USER_ID = "user_id"
        private const val KEY_FIREBASE_UID = "firebase_uid"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
    }

    // Needed to reach the Room database when the user logs out
    private val appContext = context.applicationContext

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

        // Also clear the copy of the user's data saved on the phone.
        // Workouts still waiting to sync are kept so they are not lost.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                GymLeagueDatabase.getInstance(appContext).clearUserCache()
            } catch (e: Exception) {
                Log.w("GymLeagueOffline", "Could not clear saved data: ${e.javaClass.simpleName}")
            }
        }
    }
}