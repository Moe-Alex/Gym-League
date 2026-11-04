package com.gymleague.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GymLeagueNavigation"
        const val EXTRA_SELECTED_TAB = "selected_tab"
        const val TAB_HOME = "home"
        const val TAB_MORE = "more"
    }

    private lateinit var fragmentContainer: FrameLayout
    private lateinit var navRoot: View

    // Asks for permission to show notifications (needed on Android 13 and newer)
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d(TAG, "Notification permission granted: $granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        ThemePreferences.applySavedTheme(this)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        fragmentContainer = findViewById(R.id.fragmentContainer)
        navRoot = findViewById(R.id.bottomNavigation)

        handleTabFromIntent(intent)

        setUpPushNotifications()

        Log.d(TAG, "MainActivity created")
    }

    // Create the notification channel, ask for permission and tell the API about this phone
    private fun setUpPushNotifications() {
        NotificationHelper.createChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        PushTokenManager.registerCurrentToken(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTabFromIntent(intent)
    }

    private fun handleTabFromIntent(intent: Intent) {
        when (intent.getStringExtra(EXTRA_SELECTED_TAB)) {
            TAB_MORE -> openMore()
            else -> openHome()
        }
    }

    private fun showPlaceholderScreen(title: String, message: String) {

        val existingFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

        if (existingFragment != null) {
            supportFragmentManager.beginTransaction().remove(existingFragment).commitNow()
        }

        fragmentContainer.removeAllViews()

        val textView = TextView(this).apply {
            text = "$title\n\n$message"
            textSize = 24f
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(32, 32, 32, 32)
        }

        fragmentContainer.addView(
            textView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun openHome() {
        fragmentContainer.removeAllViews()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment())
            .commit()
        BottomNavHelper.setup(this, navRoot, NavTab.HOME)
    }

    private fun openMore() {
        fragmentContainer.removeAllViews()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, MoreFragment())
            .commit()
        BottomNavHelper.setup(this, navRoot, NavTab.MORE)
    }
}