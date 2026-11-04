package com.gymleague.app.util

import java.util.Locale

/** One bar in the Muscle Group Volume chart. */
data class MuscleVolumeRow(
    val muscleGroup: String,
    val volume: Double,
    // 0.0 to 1.0, relative to the biggest muscle group (used for the bar length)
    val barFraction: Float,
    // Share of the total volume, 0 to 100
    val percentOfTotal: Int
)

/** Turns the raw "muscle group -> kg lifted" map from the API into chart rows. */
object MuscleVolumeUtils {

    fun buildRows(raw: Map<String, Double>): List<MuscleVolumeRow> {
        val positive = raw.filterValues { it > 0.0 }

        if (positive.isEmpty()) return emptyList()

        val largest = positive.values.max()
        val total = positive.values.sum()

        return positive.entries
            .sortedByDescending { it.value }
            .map { entry ->
                MuscleVolumeRow(
                    muscleGroup = entry.key,
                    volume = entry.value,
                    barFraction = (entry.value / largest).toFloat(),
                    percentOfTotal = Math.round(entry.value / total * 100).toInt()
                )
            }
    }

    fun totalVolume(rows: List<MuscleVolumeRow>): Double = rows.sumOf { it.volume }

    // e.g. 12450.0 -> "12,450 kg"
    fun formatVolume(volume: Double, locale: Locale = Locale.getDefault()): String =
        String.format(locale, "%,.0f kg", volume)
}
