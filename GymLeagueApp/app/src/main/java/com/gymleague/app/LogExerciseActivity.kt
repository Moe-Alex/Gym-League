package com.gymleague.app

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gymleague.app.models.CreateWorkoutRequest
import com.gymleague.app.models.WorkoutExercise
import com.gymleague.app.repository.WorkoutRepository
import com.gymleague.app.util.OfflineRules
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LogExerciseActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LogExerciseActivity"
    }

    private val workoutRepository = WorkoutRepository()

    // Workout timer
    private var countDownTimer: CountDownTimer? = null
    private var remainingTimeMillis: Long = 0L
    private var timerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_exercise)

        // Get selected exercise information
        val exerciseId = intent.getIntExtra("exerciseId", 0)

        val exerciseName =
            intent.getStringExtra("exerciseName") ?: getString(R.string.workout_default_exercise)

        val muscleGroup =
            intent.getStringExtra("muscleGroup") ?: ""

        val description =
            intent.getStringExtra("description")
                ?: getString(R.string.workout_no_description)

        val defaultSets =
            intent.getIntExtra("defaultSets", 3)

        val defaultReps =
            intent.getIntExtra("defaultReps", 10)

        // Find exercise views
        val tvExercise =
            findViewById<TextView>(R.id.tvSelectedExercise)

        val tvMuscleGroup =
            findViewById<TextView>(R.id.tvSelectedMuscleGroup)

        val tvDescription =
            findViewById<TextView>(R.id.tvExerciseDescription)

        // Find workout views
        val etWorkoutName =
            findViewById<EditText>(R.id.etWorkoutName)

        val etDuration =
            findViewById<EditText>(R.id.etDuration)

        val etSets =
            findViewById<EditText>(R.id.etSets)

        val etReps =
            findViewById<EditText>(R.id.etReps)

        val etWeight =
            findViewById<EditText>(R.id.etWeight)

        val btnSave =
            findViewById<Button>(R.id.btnAddToWorkout)

        val btnClear =
            findViewById<Button>(R.id.btnClear)

        // Find timer views
        val tvTimer =
            findViewById<TextView>(R.id.tvTimer)

        val etTimerMinutes =
            findViewById<EditText>(R.id.etTimerMinutes)

        val btnStartTimer =
            findViewById<Button>(R.id.btnStartTimer)

        val btnResetTimer =
            findViewById<Button>(R.id.btnResetTimer)

        // Display exercise information
        tvExercise.text = exerciseName
        tvMuscleGroup.text = muscleGroup
        tvDescription.text = description

        // Default workout values
        etWorkoutName.setText(getString(R.string.workout_default_name, exerciseName))
        etSets.setText(defaultSets.toString())
        etReps.setText(defaultReps.toString())

        // -----------------------------
        // WORKOUT TIMER
        // -----------------------------

        btnStartTimer.setOnClickListener {

            // If timer is already running, pause it
            if (timerRunning) {

                countDownTimer?.cancel()
                timerRunning = false
                btnStartTimer.text = getString(R.string.timer_resume)

                return@setOnClickListener
            }

            // If starting a new timer, read minutes entered
            if (remainingTimeMillis <= 0L) {

                val minutes =
                    etTimerMinutes.text
                        .toString()
                        .toLongOrNull()

                if (minutes == null || minutes <= 0) {

                    etTimerMinutes.error =
                        getString(R.string.workout_minutes_error)

                    return@setOnClickListener
                }

                remainingTimeMillis =
                    minutes * 60 * 1000
            }

            startTimer(
                remainingTimeMillis,
                tvTimer,
                btnStartTimer
            )
        }

        btnResetTimer.setOnClickListener {

            countDownTimer?.cancel()

            timerRunning = false
            remainingTimeMillis = 0L

            tvTimer.text = "00:00"
            btnStartTimer.text = getString(R.string.i18n_start)

            etTimerMinutes.text.clear()

            Toast.makeText(
                this,
                getString(R.string.workout_timer_reset),
                Toast.LENGTH_SHORT
            ).show()
        }

        // -----------------------------
        // CLEAR WORKOUT FORM
        // -----------------------------

        btnClear.setOnClickListener {

            etWorkoutName.setText(getString(R.string.workout_default_name, exerciseName))
            etDuration.text.clear()
            etSets.setText(defaultSets.toString())
            etReps.setText(defaultReps.toString())
            etWeight.text.clear()

            Toast.makeText(
                this,
                getString(R.string.workout_form_cleared),
                Toast.LENGTH_SHORT
            ).show()
        }

        // -----------------------------
        // SAVE WORKOUT
        // -----------------------------

        btnSave.setOnClickListener {

            val workoutName =
                etWorkoutName.text.toString().trim()

            val duration =
                etDuration.text.toString().toIntOrNull()

            val sets =
                etSets.text.toString().toIntOrNull()

            val reps =
                etReps.text.toString().toIntOrNull()

            val weight =
                etWeight.text.toString().toDoubleOrNull()

            // Validate input
            if (workoutName.isBlank()) {

                etWorkoutName.error =
                    getString(R.string.workout_name_error)

                return@setOnClickListener
            }

            if (duration == null || duration <= 0) {

                etDuration.error =
                    getString(R.string.workout_duration_error)

                return@setOnClickListener
            }

            if (sets == null || sets <= 0) {

                etSets.error =
                    getString(R.string.workout_sets_error)

                return@setOnClickListener
            }

            if (reps == null || reps <= 0) {

                etReps.error =
                    getString(R.string.workout_reps_error)

                return@setOnClickListener
            }

            if (weight == null || weight < 0) {

                etWeight.error =
                    getString(R.string.workout_weight_error)

                return@setOnClickListener
            }

            // Get logged-in user
            val sessionManager =
                SessionManager(this)

            val userId =
                sessionManager.getUserId()

            if (userId == -1) {

                Toast.makeText(
                    this,
                    getString(R.string.workout_session_error),
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Create exercise entry
            val workoutExercise =
                WorkoutExercise(
                    exerciseId = exerciseId,
                    sets = sets,
                    reps = reps,
                    weight = weight,
                    completed = true
                )

            // Current UTC date
            val dateFormat =
                SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    Locale.US
                )

            dateFormat.timeZone =
                TimeZone.getTimeZone("UTC")

            // 10 XP for each completed set
            val xpEarned = sets * 10

            // Create workout request
            val workout =
                CreateWorkoutRequest(
                    userId = userId,
                    workoutName = workoutName,
                    workoutDate = dateFormat.format(Date()),
                    durationMinutes = duration,
                    completed = true,
                    xpEarned = xpEarned,
                    workoutExercises =
                        listOf(workoutExercise)
                )

            Log.d(
                TAG,
                "Saving workout: $workoutName, " +
                        "exercise=$exerciseName, " +
                        "sets=$sets, reps=$reps, " +
                        "weight=$weight, duration=$duration"
            )

            // Prevent duplicate submissions
            btnSave.isEnabled = false
            btnClear.isEnabled = false
            btnSave.text = getString(R.string.workout_saving)

            lifecycleScope.launch {

                val result =
                    workoutRepository.createWorkout(workout)

                result.onSuccess { savedWorkout ->

                    Log.d(
                        TAG,
                        "Workout saved successfully. " +
                                "WorkoutId=${savedWorkout.workoutId}"
                    )

                    // A negative id means it is saved on the phone and still waiting to sync
                    val savedOffline =
                        OfflineRules.isPendingWorkoutId(savedWorkout.workoutId)

                    Toast.makeText(
                        this@LogExerciseActivity,
                        if (savedOffline) {
                            getString(R.string.workout_saved_offline)
                        } else {
                            getString(R.string.workout_saved_xp , xpEarned)
                        },
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                }

                result.onFailure { error ->

                    Log.e(
                        TAG,
                        "Workout save failed",
                        error
                    )

                    btnSave.isEnabled = true
                    btnClear.isEnabled = true
                    btnSave.text = getString(R.string.i18n_complete_workout)

                    Toast.makeText(
                        this@LogExerciseActivity,
                        getString(R.string.workout_save_failed, error.message ?: ""),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // -----------------------------
    // TIMER FUNCTION
    // -----------------------------

    private fun startTimer(
        timeMillis: Long,
        tvTimer: TextView,
        btnStartTimer: Button
    ) {

        countDownTimer?.cancel()

        countDownTimer =
            object : CountDownTimer(
                timeMillis,
                1000
            ) {

                override fun onTick(
                    millisUntilFinished: Long
                ) {

                    remainingTimeMillis =
                        millisUntilFinished

                    timerRunning = true

                    val totalSeconds =
                        millisUntilFinished / 1000

                    val minutes =
                        totalSeconds / 60

                    val seconds =
                        totalSeconds % 60

                    tvTimer.text =
                        String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            minutes,
                            seconds
                        )

                    btnStartTimer.text =
                        getString(R.string.timer_pause)
                }

                override fun onFinish() {

                    remainingTimeMillis = 0L
                    timerRunning = false

                    tvTimer.text = "00:00"
                    btnStartTimer.text = getString(R.string.i18n_start)

                    Toast.makeText(
                        this@LogExerciseActivity,
                        getString(R.string.workout_time_up),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }.start()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Prevent timer continuing after screen closes
        countDownTimer?.cancel()
    }
}