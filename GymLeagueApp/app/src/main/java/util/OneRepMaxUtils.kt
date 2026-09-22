package com.gymleague.app.util

import com.gymleague.app.models.PersonalRecord

object OneRepMaxUtils {

    fun sortByEstimatedOneRepMaxDescending(
        records: List<PersonalRecord>
    ): List<PersonalRecord> {
        return records.sortedByDescending { it.estimatedOneRepMax }
    }

    fun formatWeight(weight: Double): String {
        return "${trimTrailingZero(weight)} kg"
    }

    fun formatOneRepMax(oneRepMax: Double): String {
        val rounded = Math.round(oneRepMax * 10.0) / 10.0
        return "Est. 1RM: ${trimTrailingZero(rounded)} kg"
    }

    private fun trimTrailingZero(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }
}