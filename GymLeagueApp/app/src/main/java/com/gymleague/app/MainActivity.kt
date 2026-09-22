package com.gymleague.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GymLeagueNavigation"
    }

    private lateinit var fragmentContainer: FrameLayout

    private lateinit var btnHome: Button
    private lateinit var btnTrain: Button
    private lateinit var btnSquad: Button
    private lateinit var btnBattle: Button
    private lateinit var btnRanks: Button
    private lateinit var btnMore: Button

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        ThemePreferences.applySavedTheme(this)

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        bindViews()

        setupNavigation()

        if (savedInstanceState == null) {

            showPlaceholderScreen(
                "Home",
                "Welcome to GymLeague"
            )
        }

        Log.d(
            TAG,
            "MainActivity created"
        )
    }

    private fun bindViews() {

        fragmentContainer =
            findViewById(
                R.id.fragmentContainer
            )

        btnHome =
            findViewById(
                R.id.btnHome
            )

        btnTrain =
            findViewById(
                R.id.btnTrain
            )

        btnSquad =
            findViewById(
                R.id.btnSquad
            )

        btnBattle =
            findViewById(
                R.id.btnBattle
            )

        btnRanks =
            findViewById(
                R.id.btnRanks
            )

        btnMore =
            findViewById(
                R.id.btnMore
            )
    }

    private fun setupNavigation() {

        btnHome.setOnClickListener {

            Log.d(
                TAG,
                "Home selected"
            )

            showPlaceholderScreen(
                "Home",
                "Welcome to GymLeague"
            )
        }

        btnTrain.setOnClickListener {

            Log.d(
                TAG,
                "Train selected - opening Exercise Library"
            )

            val intent =
                Intent(
                    this,
                    ExerciseLibraryActivity::class.java
                )

            startActivity(intent)
        }

        btnSquad.setOnClickListener {

            Log.d(
                TAG,
                "Squad selected - opening Squad"
            )

            val intent =
                Intent(
                    this,
                    SquadActivity::class.java
                )

            startActivity(intent)
        }

        btnBattle.setOnClickListener {

            Log.d(
                TAG,
                "Battle selected"
            )

            showPlaceholderScreen(
                "Battle",
                "Battle features will appear here"
            )
        }

        btnRanks.setOnClickListener {

            Log.d(
                TAG,
                "Ranks selected - opening Personal Records"
            )

            val intent =
                Intent(
                    this,
                    PersonalRecordsActivity::class.java
                )

            startActivity(intent)
        }

        btnMore.setOnClickListener {

            Log.d(
                TAG,
                "More selected"
            )

            openMore()
        }
    }

    private fun showPlaceholderScreen(
        title: String,
        message: String
    ) {

        val existingFragment =
            supportFragmentManager
                .findFragmentById(
                    R.id.fragmentContainer
                )

        if (existingFragment != null) {

            supportFragmentManager
                .beginTransaction()
                .remove(existingFragment)
                .commitNow()
        }

        fragmentContainer.removeAllViews()

        val textView =
            TextView(this).apply {

                text =
                    "$title\n\n$message"

                textSize =
                    24f

                gravity =
                    Gravity.CENTER

                textAlignment =
                    View.TEXT_ALIGNMENT_CENTER

                setPadding(
                    32,
                    32,
                    32,
                    32
                )
            }

        fragmentContainer.addView(
            textView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun openMore() {

        fragmentContainer.removeAllViews()

        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                MoreFragment()
            )
            .commit()
    }
}