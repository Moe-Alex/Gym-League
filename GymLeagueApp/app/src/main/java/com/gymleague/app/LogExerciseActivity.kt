package com.gymleague.app

import android.os.Bundle
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

    private val workoutRepository = WorkoutRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_exercise)

        val exerciseId = intent.getIntExtra("exerciseId", 0)

        val exerciseName =
            intent.getStringExtra("exerciseName") ?: "Exercise"

        val muscleGroup =
            intent.getStringExtra("muscleGroup") ?: ""

        val defaultSets =
            intent.getIntExtra("defaultSets", 3)

        val defaultReps =
            intent.getIntExtra("defaultReps", 10)

        val tvExercise =
            findViewById<TextView>(R.id.tvSelectedExercise)

        val tvMuscleGroup =
            findViewById<TextView>(R.id.tvSelectedMuscleGroup)

        val etSets =
            findViewById<EditText>(R.id.etSets)

        val etReps =
            findViewById<EditText>(R.id.etReps)

        val etWeight =
            findViewById<EditText>(R.id.etWeight)

        val btnAdd =
            findViewById<Button>(R.id.btnAddToWorkout)

        tvExercise.text = exerciseName
        tvMuscleGroup.text = muscleGroup

        etSets.setText(defaultSets.toString())
        etReps.setText(defaultReps.toString())

        btnAdd.setOnClickListener {

            val sets = etSets.text.toString().toIntOrNull()
            val reps = etReps.text.toString().toIntOrNull()
            val weight = etWeight.text.toString().toDoubleOrNull()

            if (sets == null ||
                reps == null ||
                weight == null
            ) {
                Toast.makeText(
                    this,
                    "Please enter valid sets, reps and weight",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

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

            val workoutExercise =
                WorkoutExercise(
                    exerciseId = exerciseId,
                    sets = sets,
                    reps = reps,
                    weight = weight,
                    completed = true
                )

            val dateFormat =
                SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    Locale.US
                )

            dateFormat.timeZone =
                TimeZone.getTimeZone("UTC")

            val workout =
                CreateWorkoutRequest(
                    userId = userId,
                    workoutName = "$exerciseName Workout",
                    workoutDate = dateFormat.format(Date()),
                    durationMinutes = 0,
                    completed = true,
                    xpEarned = 10,
                    workoutExercises =
                        listOf(workoutExercise)
                )

            btnAdd.isEnabled = false
            btnAdd.text = "SAVING..."

            lifecycleScope.launch {

                val result =
                    workoutRepository.createWorkout(workout)

                result.onSuccess {

                    Toast.makeText(
                        this@LogExerciseActivity,
                        "Workout saved successfully!",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                }

                result.onFailure { error ->

                    btnAdd.isEnabled = true
                    btnAdd.text = "ADD TO WORKOUT"

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