package com.gymleague.app

import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gymleague.app.adapter.ExerciseAdapter
import com.gymleague.app.models.Exercise
import com.gymleague.app.viewmodel.ExerciseLibraryViewModel
import kotlinx.coroutines.launch

class ExerciseLibraryActivity : AppCompatActivity() {

    private lateinit var viewModel: ExerciseLibraryViewModel
    private lateinit var exerciseAdapter: ExerciseAdapter

    // Keeps the current list returned by the API
    private var currentExercises: List<Exercise> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_library)

        BottomNavHelper.setup(this, findViewById(R.id.bottomNavigation), NavTab.TRAIN)

        // Add Your Own Exercise button
        findViewById<Button>(R.id.btnAddExercise).setOnClickListener {
            showAddExerciseDialog()
        }

        // Set up RecyclerView
        val recyclerView =
            findViewById<RecyclerView>(R.id.rvExercises)

        exerciseAdapter = ExerciseAdapter { exercise ->

            val intent = Intent(
                this,
                LogExerciseActivity::class.java
            )

            intent.putExtra("exerciseId", exercise.exerciseId)
            intent.putExtra("exerciseName", exercise.exerciseName)
            intent.putExtra("muscleGroup", exercise.muscleGroup)
            intent.putExtra("defaultSets", exercise.defaultSets)
            intent.putExtra("description", exercise.description)
            intent.putExtra("defaultReps", exercise.defaultReps)

            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = exerciseAdapter

        // Set up ViewModel
        viewModel =
            ViewModelProvider(this)[ExerciseLibraryViewModel::class.java]

        // Search box
        val searchBox =
            findViewById<EditText>(R.id.etSearch)

        // -----------------------------
        // Muscle Group Filter Buttons
        // -----------------------------

        findViewById<Button>(R.id.btnAll).setOnClickListener {
            searchBox.text.clear()
            viewModel.loadExercises()
        }

        findViewById<Button>(R.id.btnChest).setOnClickListener {
            searchBox.text.clear()
            viewModel.loadExercisesByMuscleGroup("Chest")
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            searchBox.text.clear()
            viewModel.loadExercisesByMuscleGroup("Back")
        }

        findViewById<Button>(R.id.btnLegs).setOnClickListener {
            searchBox.text.clear()
            viewModel.loadExercisesByMuscleGroup("Legs")
        }

        findViewById<Button>(R.id.btnShoulders).setOnClickListener {
            searchBox.text.clear()
            viewModel.loadExercisesByMuscleGroup("Shoulders")
        }

        findViewById<Button>(R.id.btnArms).setOnClickListener {
            searchBox.text.clear()
            viewModel.loadExercisesByMuscleGroup("Arms")
        }

        // -----------------------------
        // Search
        // -----------------------------

        searchBox.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                text: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // Nothing needed here
            }

            override fun onTextChanged(
                text: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                val searchText = text.toString().trim()

                val filteredExercises =
                    if (searchText.isEmpty()) {
                        currentExercises
                    } else {
                        currentExercises.filter { exercise ->
                            exercise.exerciseName.contains(
                                searchText,
                                ignoreCase = true
                            )
                        }
                    }

                exerciseAdapter.updateExercises(filteredExercises)
            }

            override fun afterTextChanged(text: Editable?) {
                // Nothing needed here
            }
        })

        // -----------------------------
        // Observe Exercise Data
        // -----------------------------

        lifecycleScope.launch {
            viewModel.exercises.collect { exercises ->

                currentExercises = exercises

                exerciseAdapter.updateExercises(exercises)
            }
        }

        // -----------------------------
        // Observe Errors
        // -----------------------------

        lifecycleScope.launch {
            viewModel.errorMessage.collect { message ->

                if (!message.isNullOrEmpty()) {
                    Toast.makeText(
                        this@ExerciseLibraryActivity,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    private fun showAddExerciseDialog() {

        val dialogView = layoutInflater.inflate(
            R.layout.dialog_add_exercise,
            null
        )

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val exerciseName =
            dialogView.findViewById<EditText>(R.id.etCustomExerciseName)

        val muscleGroupSpinner =
            dialogView.findViewById<Spinner>(R.id.spinnerCustomMuscleGroup)

        val muscleGroups = listOf(
            "Chest",
            "Back",
            "Legs",
            "Shoulders",
            "Arms"
        )

        val muscleGroupAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            muscleGroups
        )

        muscleGroupAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        muscleGroupSpinner.adapter = muscleGroupAdapter
        val description =
            dialogView.findViewById<EditText>(R.id.etCustomDescription)

        val sets =
            dialogView.findViewById<EditText>(R.id.etCustomSets)

        val reps =
            dialogView.findViewById<EditText>(R.id.etCustomReps)

        val saveButton =
            dialogView.findViewById<Button>(R.id.btnSaveCustomExercise)

        val cancelButton =
            dialogView.findViewById<Button>(R.id.btnCancelCustomExercise)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        saveButton.setOnClickListener {

            val name = exerciseName.text.toString().trim()
            val muscle = muscleGroupSpinner.selectedItem.toString()
            val desc = description.text.toString().trim()

            val defaultSets =
                sets.text.toString().toIntOrNull()

            val defaultReps =
                reps.text.toString().toIntOrNull()

            // Validation
            if (name.isEmpty()) {
                exerciseName.error = "Enter an exercise name"
                return@setOnClickListener
            }


            if (defaultSets == null || defaultSets <= 0) {
                sets.error = "Enter valid sets"
                return@setOnClickListener
            }

            if (defaultReps == null || defaultReps <= 0) {
                reps.error = "Enter valid reps"
                return@setOnClickListener
            }

            val newExercise = Exercise(
                exerciseId = 0,
                exerciseName = name,
                muscleGroup = muscle,
                description = desc.ifEmpty { null },
                videoUrl = null,
                defaultSets = defaultSets,
                defaultReps = defaultReps
            )

            saveButton.isEnabled = false
            saveButton.text = "SAVING..."

            viewModel.createExercise(newExercise) { createdExercise ->

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "${createdExercise.exerciseName} added!",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                }
            }
        }

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(
                android.R.color.transparent
            )
        }

        dialog.show()
    }

}