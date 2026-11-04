package com.gymleague.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymleague.app.repository.MuscleVolumeRepository
import com.gymleague.app.util.MuscleVolumeRow
import com.gymleague.app.util.MuscleVolumeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Everything the Muscle Volume screen can be showing. */
sealed interface MuscleVolumeUiState {
    data object Loading : MuscleVolumeUiState
    data object Empty : MuscleVolumeUiState
    data class Error(val message: String?) : MuscleVolumeUiState
    data class Success(val rows: List<MuscleVolumeRow>, val total: Double) : MuscleVolumeUiState
}

class MuscleVolumeViewModel : ViewModel() {

    private companion object {
        const val TAG = "MuscleVolume"
    }

    private val repository = MuscleVolumeRepository()

    private val _state = MutableStateFlow<MuscleVolumeUiState>(MuscleVolumeUiState.Loading)
    val state: StateFlow<MuscleVolumeUiState> = _state.asStateFlow()

    fun load(userId: Int) {
        viewModelScope.launch {

            _state.value = MuscleVolumeUiState.Loading

            repository.getMuscleGroupVolume(userId)
                .onSuccess { raw ->
                    val rows = MuscleVolumeUtils.buildRows(raw)

                    Log.d(TAG, "Loaded volume for ${rows.size} muscle groups")

                    _state.value = if (rows.isEmpty()) {
                        MuscleVolumeUiState.Empty
                    } else {
                        MuscleVolumeUiState.Success(rows, MuscleVolumeUtils.totalVolume(rows))
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load muscle volume", error)
                    _state.value = MuscleVolumeUiState.Error(error.message)
                }
        }
    }
}
