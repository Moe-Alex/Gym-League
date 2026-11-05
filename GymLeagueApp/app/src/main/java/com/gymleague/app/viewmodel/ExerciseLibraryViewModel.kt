package com.gymleague.app.viewmodel

import com.gymleague.app.util.AppText

import com.gymleague.app.R

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymleague.app.models.Exercise
import com.gymleague.app.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExerciseLibraryViewModel : ViewModel() {

    private val repository = ExerciseRepository()

    // List of exercises displayed by the UI
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadExercises()
    }

    // Load all exercises
    fun loadExercises() {
        viewModelScope.launch {

            _isLoading.value = true
            _errorMessage.value = null

            Log.d("ExerciseLibrary", "Loading all exercises")

            repository.getExercises()
                .onSuccess { exerciseList ->

                    _exercises.value = exerciseList

                    Log.d(
                        "ExerciseLibrary",
                        "Successfully loaded ${exerciseList.size} exercises"
                    )
                }
                .onFailure { error ->

                    _errorMessage.value =
                        error.message ?: AppText.get(R.string.error_load_exercises)

                    Log.e(
                        "ExerciseLibrary",
                        "Failed to load exercises",
                        error
                    )
                }

            _isLoading.value = false
        }
    }

    // Filter exercises using the API
    fun loadExercisesByMuscleGroup(muscleGroup: String) {
        viewModelScope.launch {

            _isLoading.value = true
            _errorMessage.value = null

            Log.d(
                "ExerciseLibrary",
                "Loading exercises for muscle group: $muscleGroup"
            )

            repository.getExercisesByMuscleGroup(muscleGroup)
                .onSuccess { exerciseList ->

                    _exercises.value = exerciseList

                    Log.d(
                        "ExerciseLibrary",
                        "Loaded ${exerciseList.size} $muscleGroup exercises"
                    )
                }
                .onFailure { error ->

                    _errorMessage.value =
                        error.message ?: AppText.get(R.string.error_load_exercises)

                    Log.e(
                        "ExerciseLibrary",
                        "Failed to load $muscleGroup exercises",
                        error
                    )
                }

            _isLoading.value = false
        }
    }

    // Create a new custom exercise
    fun createExercise(
        exercise: Exercise,
        onSuccess: (Exercise) -> Unit
    ) {
        viewModelScope.launch {

            _isLoading.value = true
            _errorMessage.value = null

            Log.d(
                "ExerciseLibrary",
                "Creating exercise: ${exercise.exerciseName}"
            )

            repository.createExercise(exercise)
                .onSuccess { createdExercise ->

                    Log.d(
                        "ExerciseLibrary",
                        "Exercise created successfully: ${createdExercise.exerciseName}"
                    )

                    // Reload the list so the new exercise appears
                    loadExercises()

                    onSuccess(createdExercise)
                }
                .onFailure { error ->

                    _errorMessage.value =
                        error.message ?: AppText.get(R.string.error_create_exercise)

                    Log.e(
                        "ExerciseLibrary",
                        "Failed to create exercise",
                        error
                    )
                }

            _isLoading.value = false
        }
    }

    // Delete an exercise
    fun deleteExercise(
        exerciseId: Int,
        userId: Int,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {

            _isLoading.value = true
            _errorMessage.value = null

            Log.d("ExerciseLibrary", "Deleting exercise: $exerciseId")

            repository.deleteExercise(exerciseId, userId)
                .onSuccess {
                    Log.d("ExerciseLibrary", "Exercise deleted successfully")
                    loadExercises()
                    onSuccess()
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: AppText.get(R.string.error_delete_exercise)
                    Log.e("ExerciseLibrary", "Failed to delete exercise", error)
                }

            _isLoading.value = false
        }
    }

    // Clear the current error
    fun clearError() {
        _errorMessage.value = null
    }
}