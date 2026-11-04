package com.gymleague.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.gymleague.app.util.MuscleVolumeRow
import com.gymleague.app.util.MuscleVolumeUtils
import com.gymleague.app.viewmodel.MuscleVolumeUiState
import com.gymleague.app.viewmodel.MuscleVolumeViewModel
import kotlinx.coroutines.launch

/**
 * Muscle Group Volume: how much weight (sets x reps x kg) the user has lifted for each
 * muscle group, shown as one bar per group, largest first.
 */
class MuscleVolumeActivity : AppCompatActivity() {

    private companion object {
        const val TAG = "GymLeagueMuscleVolume"
    }

    private lateinit var viewModel: MuscleVolumeViewModel

    private lateinit var progress: ProgressBar
    private lateinit var layoutSummary: View
    private lateinit var tvTotal: TextView
    private lateinit var tvTop: TextView
    private lateinit var barsContainer: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var tvError: TextView
    private lateinit var btnRetry: Button

    private var userId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_muscle_volume)

        // Muscle Volume has its own tab in the bottom navigation
        BottomNavHelper.setup(this, findViewById(R.id.bottomNavigation), NavTab.VOLUME)

        userId = SessionManager(this).getUserId()

        if (userId == -1) {
            Log.w(TAG, "No signed-in user, closing Muscle Volume")
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[MuscleVolumeViewModel::class.java]

        progress = findViewById(R.id.progressMuscleVolume)
        layoutSummary = findViewById(R.id.layoutMuscleSummary)
        tvTotal = findViewById(R.id.tvMuscleTotal)
        tvTop = findViewById(R.id.tvMuscleTop)
        barsContainer = findViewById(R.id.layoutMuscleBars)
        tvEmpty = findViewById(R.id.tvMuscleEmpty)
        tvError = findViewById(R.id.tvMuscleError)
        btnRetry = findViewById(R.id.btnMuscleRetry)

        findViewById<Button>(R.id.btnMuscleBack).setOnClickListener { finish() }
        btnRetry.setOnClickListener { viewModel.load(userId) }

        lifecycleScope.launch {
            viewModel.state.collect { render(it) }
        }

        viewModel.load(userId)
    }

    // Show exactly one of: loading, error, empty or the bars
    private fun render(state: MuscleVolumeUiState) {
        progress.visibility = if (state is MuscleVolumeUiState.Loading) View.VISIBLE else View.GONE
        tvEmpty.visibility = if (state is MuscleVolumeUiState.Empty) View.VISIBLE else View.GONE
        tvError.visibility = if (state is MuscleVolumeUiState.Error) View.VISIBLE else View.GONE
        btnRetry.visibility = if (state is MuscleVolumeUiState.Error) View.VISIBLE else View.GONE

        val hasData = state is MuscleVolumeUiState.Success
        layoutSummary.visibility = if (hasData) View.VISIBLE else View.GONE
        barsContainer.removeAllViews()

        if (state is MuscleVolumeUiState.Success) {
            tvTotal.text = MuscleVolumeUtils.formatVolume(state.total)
            tvTop.text = state.rows.first().muscleGroup
            state.rows.forEach { addBar(it) }
        }
    }

    private fun addBar(row: MuscleVolumeRow) {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.item_muscle_volume, barsContainer, false)

        view.findViewById<TextView>(R.id.tvMuscleName).text = row.muscleGroup

        view.findViewById<TextView>(R.id.tvMuscleDetail).text = getString(
            R.string.muscle_volume_row_detail,
            MuscleVolumeUtils.formatVolume(row.volume),
            row.percentOfTotal
        )

        view.findViewById<ProgressBar>(R.id.progressMuscleBar).progress =
            (row.barFraction * 1000).toInt()

        barsContainer.addView(view)
    }
}
