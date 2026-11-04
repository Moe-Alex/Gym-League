package com.gymleague.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymleague.app.models.Challenge
import com.gymleague.app.models.CreateChallengeRequest
import com.gymleague.app.repository.ChallengeRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One-off things the Battle Arena screen should tell the user about. */
sealed interface BattleEvent {
    data object Launched : BattleEvent
    data class Sent(val opponentName: String) : BattleEvent
    data object Accepted : BattleEvent
    data object Declined : BattleEvent
    data class Won(val xp: Int) : BattleEvent
    data class Failed(val message: String?) : BattleEvent
}

class ChallengeViewModel : ViewModel() {

    private companion object {
        const val TAG = "Battle"
    }

    private val repository = ChallengeRepository()

    private val _challenges = MutableStateFlow<List<Challenge>>(emptyList())
    val challenges: StateFlow<List<Challenge>> = _challenges.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _events = MutableSharedFlow<BattleEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<BattleEvent> = _events.asSharedFlow()

    fun loadChallenges(userId: Int) {
        viewModelScope.launch {

            _isLoading.value = true
            _errorMessage.value = null

            repository.getChallenges(userId)
                .onSuccess { list ->
                    _challenges.value = list
                    Log.d(TAG, "Loaded ${list.size} challenges")
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Unable to load challenges"
                    Log.e(TAG, "Failed to load challenges", error)
                }

            _isLoading.value = false
        }
    }

    fun createChallenge(
        request: CreateChallengeRequest,
        opponentName: String?,
        userId: Int,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        viewModelScope.launch {

            repository.createChallenge(request)
                .onSuccess {
                    Log.d(TAG, "Challenge created: ${it.challengeId}")
                    _events.tryEmit(
                        if (request.opponentUserId != null) {
                            BattleEvent.Sent(opponentName ?: "")
                        } else {
                            BattleEvent.Launched
                        }
                    )
                    onSuccess()
                    loadChallenges(userId)
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to create challenge", error)
                    _events.tryEmit(BattleEvent.Failed(error.message))
                    onFailure()
                }
        }
    }

    fun acceptChallenge(challengeId: Int, userId: Int) {
        viewModelScope.launch {

            repository.acceptChallenge(challengeId, userId)
                .onSuccess {
                    Log.d(TAG, "Accepted challenge $challengeId")
                    _events.tryEmit(BattleEvent.Accepted)
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to accept challenge", error)
                    _events.tryEmit(BattleEvent.Failed(error.message))
                }

            loadChallenges(userId)
        }
    }

    fun declineChallenge(challengeId: Int, userId: Int) {
        viewModelScope.launch {

            repository.declineChallenge(challengeId, userId)
                .onSuccess {
                    Log.d(TAG, "Declined challenge $challengeId")
                    _events.tryEmit(BattleEvent.Declined)
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to decline challenge", error)
                    _events.tryEmit(BattleEvent.Failed(error.message))
                }

            loadChallenges(userId)
        }
    }

    fun logProgress(challengeId: Int, userId: Int) {
        viewModelScope.launch {

            repository.logProgress(challengeId, userId)
                .onSuccess { updated ->
                    Log.d(TAG, "Progress logged on challenge $challengeId")

                    if (updated.completed && updated.winnerUserId == userId) {
                        _events.tryEmit(BattleEvent.Won(updated.xpReward))
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to log progress", error)
                    _events.tryEmit(BattleEvent.Failed(error.message))
                }

            loadChallenges(userId)
        }
    }
}
