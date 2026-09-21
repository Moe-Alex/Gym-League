package com.gymleague.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gymleague.app.adapter.ExerciseAdapter
import com.gymleague.app.viewmodel.ExerciseLibraryViewModel
import kotlinx.coroutines.launch

class ExerciseLibraryActivity : AppCompatActivity() {

    private lateinit var viewModel: ExerciseLibraryViewModel
    private lateinit var exerciseAdapter: ExerciseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_library)

        val recyclerView =
            findViewById<RecyclerView>(R.id.rvExercises)

        exerciseAdapter = ExerciseAdapter()

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = exerciseAdapter

        viewModel =
            ViewModelProvider(this)[ExerciseLibraryViewModel::class.java]

        lifecycleScope.launch {
            viewModel.exercises.collect { exercises ->
                exerciseAdapter.updateExercises(exercises)
            }
        }

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