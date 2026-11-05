package com.gymleague.app.util

import com.gymleague.app.models.WorkoutExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonListsTest {

    private val squats = WorkoutExercise(
        exerciseId = 3,
        sets = 4,
        reps = 8,
        weight = 82.5
    )

    @Test
    fun list_survivesBeingSavedAndLoaded() {
        val json = JsonLists.encode(listOf(squats, squats.copy(exerciseId = 4)))
        val loaded = JsonLists.decode<WorkoutExercise>(json)

        assertEquals(2, loaded.size)
        assertEquals(squats, loaded[0])
        assertEquals(4, loaded[1].exerciseId)
        assertEquals(82.5, loaded[0].weight, 0.0)
    }

    @Test
    fun emptyList_comesBackEmpty() {
        val json = JsonLists.encode(emptyList<WorkoutExercise>())

        assertTrue(JsonLists.decode<WorkoutExercise>(json).isEmpty())
    }

    @Test
    fun missingOrBrokenText_givesAnEmptyListInsteadOfCrashing() {
        assertTrue(JsonLists.decode<WorkoutExercise>(null).isEmpty())
        assertTrue(JsonLists.decode<WorkoutExercise>("").isEmpty())
        assertTrue(JsonLists.decode<WorkoutExercise>("not json at all").isEmpty())
    }
}