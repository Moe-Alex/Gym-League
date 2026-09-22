package com.gymleague.app

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_exercise)

        // Get selected exercise information
        val exerciseId = intent.getIntExtra("exerciseId", 0)

        val exerciseName =
            intent.getStringExtra("exerciseName") ?: "Exercise"

        val muscleGroup =
            intent.getStringExtra("muscleGroup") ?: ""

        val description =
            intent.getStringExtra("description")
                ?: "No description available"

        val defaultSets =
            intent.getIntExtra("defaultSets", 3)

        val defaultReps =
            intent.getIntExtra("defaultReps", 10)

        // Find views
        val tvExercise =
            findViewById<TextView>(R.id.tvSelectedExercise)

        val tvMuscleGroup =
            findViewById<TextView>(R.id.tvSelectedMuscleGroup)

        val tvDescription =
            findViewById<TextView>(R.id.tvExerciseDescription)

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

        // Display exercise information
        tvExercise.text = exerciseName
        tvMuscleGroup.text = muscleGroup
        tvDescription.text = description

        // Default values
        etWorkoutName.setText("$exerciseName Workout")
        etSets.setText(defaultSets.toString())
        etReps.setText(defaultReps.toString())

        // Clear button
        btnClear.setOnClickListener {

            etWorkoutName.setText("$exerciseName Workout")
            etDuration.text.clear()
            etSets.setText(defaultSets.toString())
            etReps.setText(defaultReps.toString())
            etWeight.text.clear()

            Toast.makeText(
                this,
                "Workout form cleared",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Save workout
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
                    "Enter a workout name"

                return@setOnClickListener
            }

            if (duration == null || duration <= 0) {

                etDuration.error =
                    "Enter a valid duration"

                return@setOnClickListener
            }

            if (sets == null || sets <= 0) {

                etSets.error =
                    "Enter valid sets"

                return@setOnClickListener
            }

            if (reps == null || reps <= 0) {

                etReps.error =
                    "Enter valid reps"

                return@setOnClickListener
            }

            if (weight == null || weight < 0) {

                etWeight.error =
                    "Enter a valid weight"

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
                    "User session not found",
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

            // XP example:
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
            btnSave.text = "SAVING..."

            lifecycleScope.launch {

                val result =
                    workoutRepository.createWorkout(workout)

                result.onSuccess { savedWorkout ->

                    Log.d(
                        TAG,
                        "Workout saved successfully. " +
                                "WorkoutId=${savedWorkout.workoutId}"
                    )

                    Toast.makeText(
                        this@LogExerciseActivity,
                        "Workout saved! +$xpEarned XP",
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
                    btnSave.text = "SAVE WORKOUT"

                    Toast.makeText(
                        this@LogExerciseActivity,
                        "Could not save workout: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}