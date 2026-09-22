package com.gymleague.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymleague.app.models.PersonalRecord
import com.gymleague.app.repository.PersonalRecordsRepository
import com.gymleague.app.util.OneRepMaxUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PersonalRecordsViewModel : ViewModel() {

    companion object {
        private const val TAG = "PersonalRecordsVM"
    }

    private val repository = PersonalRecordsRepository()

    private val _records = MutableStateFlow<List<PersonalRecord>>(emptyList())
    val records: StateFlow<List<PersonalRecord>> = _records.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadPersonalRecords(userId: Int) {
        viewModelScope.launch {

            _isLoading.value = true
            _errorMessage.value = null

            Log.d(TAG, "Loading personal records for userId=$userId")

            repository.getPersonalRecords(userId)
                .onSuccess { recordList ->

                    _records.value =
                        OneRepMaxUtils.sortByEstimatedOneRepMaxDescending(recordList)

                    Log.d(TAG, "Loaded ${recordList.size} personal records")
                }
                .onFailure { error ->

                    _errorMessage.value =
                        error.message ?: "Unable to load personal records"

                    Log.e(TAG, "Failed to load personal records", error)
                }

            _isLoading.value = false
        }
    }
}