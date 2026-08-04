package io.github.meko123456.habitstreaks.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import io.github.meko123456.habitstreaks.domain.HeatmapLevel
import java.time.DayOfWeek
import java.time.LocalDate

/** GitHub dark-theme contribution greens, index = level 1..4. */
private val LevelColors = listOf(
    Color(0xFF0E4429),
    Color(0xFF006D32),
    Color(0xFF26A641),
    Color(0xFF39D353),
)

/**
 * GitHub-style contribution heatmap drawn on a single Canvas.
 *
 * Layout mirrors github.com: one column per week, rows Sunday..Saturday,
 * newest week in the rightmost column ending at [endDay].
 *
 * @param counts completions per day, keyed by LocalDate.toEpochDay()
 * @param endDay last day to render (usually today's epoch day)
 * @param weeks number of week columns
 */
@Composable
fun ContributionHeatmap(
    counts: Map<Long, Int>,
    endDay: Long,
    modifier: Modifier = Modifier,
    weeks: Int = 20,
    levelColors: List<Color> = LevelColors,
    emptyColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
) {
    require(weeks > 0) { "weeks must be positive" }
    val maxCount = counts.values.maxOrNull() ?: 0
    // Row of endDay with Sunday as row 0, matching GitHub.
    val endRow = LocalDate.ofEpochDay(endDay).dayOfWeek.let { dow ->
        if (dow == DayOfWeek.SUNDAY) 0 else dow.value
    }
    val firstDay = endDay - endRow - (weeks - 1) * 7L

    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(weeks / 7f)) {
        val step = size.width / weeks
        val cell = step * 0.82f
        val corner = CornerRadius(cell * 0.18f)
        for (col in 0 until weeks) {
            for (row in 0 until 7) {
                val day = firstDay + col * 7L + row
                if (day > endDay) continue
                val level = HeatmapLevel.levelFor(counts[day] ?: 0, maxCount)
                drawRoundRect(
                    color = if (level == 0) emptyColor else levelColors[level - 1],
                    topLeft = Offset(col * step, row * step),
                    size = Size(cell, cell),
                    cornerRadius = corner,
                )
            }
        }
    }
}
