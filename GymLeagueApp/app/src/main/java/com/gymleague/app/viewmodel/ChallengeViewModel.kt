package com.gymleague.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymleague.app.models.Challenge
import com.gymleague.app.models.CreateChallengeRequest
import com.gymleague.app.repository.ChallengeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChallengeViewModel : ViewModel() {

    private val repository = ChallengeRepository()

    private val _challenges = MutableStateFlow<List<Challenge>>(emptyList())
    val challenges: StateFlow<List<Challenge>> = _challenges.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadChallenges()
    }

    fun loadChallenges() {
        viewModelScope.launch {

            _isLoading.value = true
            _errorMessage.value = null

            repository.getChallenges()
                .onSuccess { list ->
                    _challenges.value = list
                }
                .onFailure { error ->
                    _errorMessage.value =
                        error.message ?: "Unable to load challenges"

                    Log.e("Battle", "Failed to load challenges", error)
                }

            _isLoading.value = false
        }
    }

    fun createChallenge(
        request: CreateChallengeRequest,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {

            _isLoading.value = true
            _errorMessage.value = null

            repository.createChallenge(request)
                .onSuccess {
                    loadChallenges()
                    onSuccess()
                }
                .onFailure { error ->
                    _errorMessage.value =
                        error.message ?: "Unable to create challenge"

                    Log.e("Battle", "Failed to create challenge", error)
                }

            _isLoading.value = false
        }
    }

    fun logProgress(challengeId: Int) {
        viewModelScope.launch {

            repository.logProgress(challengeId)
                .onSuccess {
                    loadChallenges()
                }
                .onFailure { error ->
                    _errorMessage.value =
                        error.message ?: "Unable to log progress"

                    Log.e("Battle", "Failed to log progress", error)
                }
        }
    }
}