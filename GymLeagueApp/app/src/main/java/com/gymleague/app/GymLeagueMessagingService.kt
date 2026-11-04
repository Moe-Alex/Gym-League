package com.gymleague.app

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives push notifications from Firebase Cloud Messaging.
 *  - onNewToken: Firebase gave this phone a new token, so tell our API about it.
 *  - onMessageReceived: a push arrived while the app is open, so show it ourselves.
 */
class GymLeagueMessagingService : FirebaseMessagingService() {

    private companion object {
        const val TAG = "GymLeaguePush"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received")

        PushTokenManager.registerToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)

        val body = message.notification?.body
            ?: message.data["body"]
            ?: ""

        Log.d(TAG, "Push received: ${message.data["type"]} - $title")

        NotificationHelper.show(applicationContext, title, body)
    }
}
