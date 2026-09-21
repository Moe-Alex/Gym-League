package com.gymleague.app

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

        // Set up RecyclerView
        val recyclerView =
            findViewById<RecyclerView>(R.id.rvExercises)

        exerciseAdapter = ExerciseAdapter()

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
}