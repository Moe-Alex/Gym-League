package com.gymleague.app

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.gymleague.app.models.RegisterDeviceTokenRequest
import com.gymleague.app.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Keeps the API informed about which phone belongs to which signed-in user, so the server
 * knows where to send that user's push notifications.
 */
object PushTokenManager {

    private const val TAG = "GymLeaguePush"

    // Lives as long as the app process, so a request is not cancelled when a screen closes
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // The user/token pair we last sent (or are sending). The app learns about a new token in two
    // places at once, so this stops the same registration being sent twice.
    @Volatile
    private var lastSent: String? = null

    // Fetch this phone's Firebase token and give it to the API (called when the app opens)
    fun registerCurrentToken(context: Context) {
        val appContext = context.applicationContext

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> registerToken(appContext, token) }
            .addOnFailureListener { error -> Log.w(TAG, "Could not get the FCM token", error) }
    }

    fun registerToken(context: Context, token: String) {
        val userId = SessionManager(context.applicationContext).getUserId()

        if (userId == -1) {
            Log.d(TAG, "Nobody is signed in, not registering the token")
            return
        }

        val key = "$userId:$token"

        synchronized(this) {
            if (lastSent == key) {
                Log.d(TAG, "Device token already registered for user $userId, skipping")
                return
            }
            lastSent = key
        }

        scope.launch {
            try {
                val response = RetrofitClient.apiService.registerDeviceToken(
                    RegisterDeviceTokenRequest(userId, token)
                )

                if (response.isSuccessful) {
                    Log.d(TAG, "Device token registered for user $userId")
                } else {
                    Log.w(TAG, "Device token registration failed: ${response.code()}")
                    lastSent = null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Device token registration error", e)
                lastSent = null
            }
        }
    }

    // Called when the user logs out, so this phone stops receiving their notifications
    fun unregisterCurrentToken() {
        lastSent = null

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                scope.launch {
                    try {
                        RetrofitClient.apiService.unregisterDeviceToken(token)
                        Log.d(TAG, "Device token removed")
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not remove the device token", e)
                    }
                }
            }
    }
}
