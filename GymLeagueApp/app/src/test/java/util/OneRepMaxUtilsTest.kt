package com.gymleague.app.util

import com.gymleague.app.models.PersonalRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class OneRepMaxUtilsTest {

    private fun record(
        name: String,
        oneRepMax: Double
    ) = PersonalRecord(
        exerciseId = 1,
        exerciseName = name,
        heaviestWeight = 50.0,
        repsAtHeaviestWeight = 5,
        estimatedOneRepMax = oneRepMax,
        dateAchieved = "2026-09-20T00:00:00"
    )

    @Test
    fun sortReturnsEmptyListWhenInputIsEmpty() {
        val result = OneRepMaxUtils.sortByEstimatedOneRepMaxDescending(emptyList())
        assertEquals(0, result.size)
    }

    @Test
    fun sortOrdersRecordsByEstimatedOneRepMaxDescending() {
        val records = listOf(
            record("Squat", 100.0),
            record("Bench Press", 140.0),
            record("Deadlift", 180.0)
        )

        val sorted = OneRepMaxUtils.sortByEstimatedOneRepMaxDescending(records)

        assertEquals("Deadlift", sorted[0].exerciseName)
        assertEquals("Bench Press", sorted[1].exerciseName)
        assertEquals("Squat", sorted[2].exerciseName)
    }

    @Test
    fun sortKeepsASingleItemUnchanged() {
        val records = listOf(record("Overhead Press", 60.0))
        val sorted = OneRepMaxUtils.sortByEstimatedOneRepMaxDescending(records)
        assertEquals(1, sorted.size)
        assertEquals("Overhead Press", sorted[0].exerciseName)
    }

    @Test
    fun formatWeightDropsTrailingZeroForWholeNumbers() {
        assertEquals("100 kg", OneRepMaxUtils.formatWeight(100.0))
    }

    @Test
    fun formatWeightKeepsOneDecimalForFractionalValues() {
        assertEquals("82.5 kg", OneRepMaxUtils.formatWeight(82.5))
    }

    @Test
    fun formatOneRepMaxRoundsToOneDecimalPlace() {
        assertEquals("Est. 1RM: 96.3 kg", OneRepMaxUtils.formatOneRepMax(96.25))
    }

    @Test
    fun formatOneRepMaxDropsTrailingZeroForWholeNumbers() {
        assertEquals("Est. 1RM: 150 kg", OneRepMaxUtils.formatOneRepMax(150.0))
    }
}