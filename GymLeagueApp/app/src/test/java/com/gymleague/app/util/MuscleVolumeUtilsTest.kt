package com.gymleague.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MuscleVolumeUtilsTest {

    @Test
    fun buildRows_emptyMap_givesNoRows() {
        assertTrue(MuscleVolumeUtils.buildRows(emptyMap()).isEmpty())
    }

    @Test
    fun buildRows_ignoresZeroAndNegativeVolume() {
        val rows = MuscleVolumeUtils.buildRows(mapOf("Chest" to 0.0, "Back" to -5.0))
        assertTrue(rows.isEmpty())
    }

    @Test
    fun buildRows_sortsBiggestFirst() {
        val rows = MuscleVolumeUtils.buildRows(
            mapOf("Legs" to 500.0, "Chest" to 1500.0, "Back" to 1000.0)
        )

        assertEquals(listOf("Chest", "Back", "Legs"), rows.map { it.muscleGroup })
    }

    @Test
    fun buildRows_barFractionIsRelativeToTheLargest() {
        val rows = MuscleVolumeUtils.buildRows(mapOf("Chest" to 2000.0, "Back" to 500.0))

        assertEquals(1f, rows[0].barFraction, 0.0001f)
        assertEquals(0.25f, rows[1].barFraction, 0.0001f)
    }

    @Test
    fun buildRows_percentagesAreShareOfTotal() {
        val rows = MuscleVolumeUtils.buildRows(mapOf("Chest" to 750.0, "Back" to 250.0))

        assertEquals(75, rows[0].percentOfTotal)
        assertEquals(25, rows[1].percentOfTotal)
    }

    @Test
    fun totalVolume_addsEveryRow() {
        val rows = MuscleVolumeUtils.buildRows(mapOf("Chest" to 750.0, "Back" to 250.0))
        assertEquals(1000.0, MuscleVolumeUtils.totalVolume(rows), 0.0001)
    }

    @Test
    fun formatVolume_usesThousandsSeparatorAndUnit() {
        assertEquals("12,450 kg", MuscleVolumeUtils.formatVolume(12450.0, Locale.US))
        assertEquals("0 kg", MuscleVolumeUtils.formatVolume(0.0, Locale.US))
    }
}
