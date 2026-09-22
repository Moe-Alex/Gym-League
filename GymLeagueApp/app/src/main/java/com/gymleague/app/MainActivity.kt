package com.gymleague.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GymLeagueNavigation"
        const val EXTRA_SELECTED_TAB = "selected_tab"
        const val TAB_HOME = "home"
        const val TAB_BATTLE = "battle"
        const val TAB_MORE = "more"
    }

    private lateinit var fragmentContainer: FrameLayout
    private lateinit var navRoot: View

    override fun onCreate(savedInstanceState: Bundle?) {

        ThemePreferences.applySavedTheme(this)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        fragmentContainer = findViewById(R.id.fragmentContainer)
        navRoot = findViewById(R.id.bottomNavigation)

        handleTabFromIntent(intent)

        Log.d(TAG, "MainActivity created")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTabFromIntent(intent)
    }

    private fun handleTabFromIntent(intent: Intent) {
        when (intent.getStringExtra(EXTRA_SELECTED_TAB)) {
            TAB_BATTLE -> openBattle()
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

    private fun openBattle() {
        showPlaceholderScreen("Battle", "Battle features will appear here")
        BottomNavHelper.setup(this, navRoot, NavTab.BATTLE)
    }

    private fun openMore() {
        fragmentContainer.removeAllViews()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, MoreFragment())
            .commit()
        BottomNavHelper.setup(this, navRoot, NavTab.MORE)
    }
}