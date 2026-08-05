package io.github.meko123456.habitstreaks.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import io.github.meko123456.habitstreaks.domain.HeatmapLevel
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Renders the contribution heatmap into a Bitmap for surfaces that cannot
 * host Compose Canvas (Glance widgets, notifications, shares). Same layout
 * rules as the in-app ContributionHeatmap: week columns, Sunday-first rows,
 * newest column on the right.
 */
object HeatmapBitmap {

    // GitHub dark-theme greens, index = level 1..4; translucent gray for empty.
    private val levelColors = intArrayOf(0xFF0E4429.toInt(), 0xFF006D32.toInt(), 0xFF26A641.toInt(), 0xFF39D353.toInt())
    private const val EMPTY_COLOR = 0x40808080

    fun render(
        counts: Map<Long, Int>,
        endDay: Long,
        weeks: Int = 26,
        cellPx: Int = 24,
        gapPx: Int = 6,
    ): Bitmap {
        require(weeks > 0 && cellPx > 0)
        val step = cellPx + gapPx
        val bitmap = Bitmap.createBitmap(
            weeks * step - gapPx,
            7 * step - gapPx,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val corner = cellPx * 0.18f
        val maxCount = counts.values.maxOrNull() ?: 0

        val endRow = LocalDate.ofEpochDay(endDay).dayOfWeek.let { dow ->
            if (dow == DayOfWeek.SUNDAY) 0 else dow.value
        }
        val firstDay = endDay - endRow - (weeks - 1) * 7L

        val rect = RectF()
        for (col in 0 until weeks) {
            for (row in 0 until 7) {
                val day = firstDay + col * 7L + row
                if (day > endDay) continue
                val level = HeatmapLevel.levelFor(counts[day] ?: 0, maxCount)
                paint.color = if (level == 0) EMPTY_COLOR else levelColors[level - 1]
                val left = (col * step).toFloat()
                val top = (row * step).toFloat()
                rect.set(left, top, left + cellPx, top + cellPx)
                canvas.drawRoundRect(rect, corner, corner, paint)
            }
        }
        return bitmap
    }
}
