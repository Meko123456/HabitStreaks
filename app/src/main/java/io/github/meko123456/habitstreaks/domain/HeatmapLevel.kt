package io.github.meko123456.habitstreaks.domain

import kotlin.math.ceil

/**
 * Buckets a day's completion count into a GitHub-style intensity level:
 * 0 = empty, 1..4 = progressively darker, where the busiest day maps to 4.
 */
object HeatmapLevel {

    const val MAX_LEVEL = 4

    fun levelFor(count: Int, maxCount: Int): Int {
        if (count <= 0 || maxCount <= 0) return 0
        if (count >= maxCount) return MAX_LEVEL
        return ceil(count * (MAX_LEVEL - 1f) / maxCount).toInt().coerceIn(1, MAX_LEVEL - 1)
    }
}
