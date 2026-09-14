package io.github.meko123456.habitstreaks.widget

import io.github.meko123456.heatmap.HeatmapLayout
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Cell geometry chosen for one particular widget size. */
internal data class HeatmapSpec(val cellPx: Int, val gapPx: Int, val weeks: Int) {
    val stepPx: Int get() = cellPx + gapPx
    val widthPx: Int get() = weeks * stepPx - gapPx
    val heightPx: Int get() = HeatmapLayout.ROWS * stepPx - gapPx
}

/**
 * Decides how big to draw the contribution grid for the space a widget actually has.
 *
 * Kept out of the composable and free of Android types so it can be tested on the JVM — the old
 * widget's problem was entirely a sizing decision, not a drawing one, and a sizing decision that
 * nothing could check is how it went unnoticed.
 */
internal object WidgetHeatmap {

    /** Below this a cell reads as a smudge rather than a square. */
    const val MIN_STEP_PX = 6f

    /** A year and a bit. Beyond this the contribution calendar has nothing left to show. */
    const val MAX_WEEKS = 53

    /**
     * The grid for a box of [widthPx] by [heightPx], or `null` when the box is too small to draw
     * seven readable rows in.
     *
     * Height decides the cell size, because seven rows is fixed and they should always fill the box
     * top to bottom. Width then decides how many weeks fit. So a widget dragged larger gets both
     * bigger cells *and* more history, rather than the same small strip centred in more space.
     */
    fun specFor(widthPx: Int, heightPx: Int): HeatmapSpec? {
        if (widthPx <= 0 || heightPx <= 0) return null

        val step = heightPx.toFloat() / HeatmapLayout.ROWS
        if (step < MIN_STEP_PX) return null

        val cellPx = max(1, (step * HeatmapLayout.CELL_FRACTION).roundToInt())
        val gapPx = max(1, step.roundToInt() - cellPx)
        val weeks = min(MAX_WEEKS, max(1, (widthPx + gapPx) / (cellPx + gapPx)))

        return HeatmapSpec(cellPx = cellPx, gapPx = gapPx, weeks = weeks)
    }

    /**
     * The busiest day among those actually drawn.
     *
     * The heatmap library scales colour to the busiest day in the whole map by default, and the map
     * here holds a year while only [weeks] of it is on screen. One exceptional day last winter would
     * otherwise wash every visible week out to almost empty. Null when nothing is drawn, which the
     * library reads as "use your own default".
     */
    fun visibleMax(counts: Map<Long, Int>, endDay: Long, weeks: Int): Int? {
        val firstDay = HeatmapLayout.firstDay(endDay, weeks)
        return counts.entries
            .filter { it.key in firstDay..endDay }
            .maxOfOrNull { it.value }
            ?.takeIf { it > 0 }
    }
}
