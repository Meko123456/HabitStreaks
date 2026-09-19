package io.github.meko123456.habitstreaks.widget

import io.github.meko123456.heatmap.HeatmapLayout
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Cell geometry for the bitmap the widget draws. */
internal data class HeatmapSpec(val cellPx: Int, val gapPx: Int, val weeks: Int) {
    val stepPx: Int get() = cellPx + gapPx
    val widthPx: Int get() = weeks * stepPx - gapPx
    val heightPx: Int get() = HeatmapLayout.ROWS * stepPx - gapPx
}

/**
 * Decides how much history to draw, from the width of the widget.
 *
 * **Width only, deliberately.** A widget is supposed to report its own size, and on the Samsung
 * launcher the height it reports is the height it could shrink to rather than the height it has —
 * 72dp for a widget measuring 189dp on screen. Sizing the grid from that produced a graph built for
 * a sliver. The width is reported honestly (334dp reported, 334dp measured), so the width is what
 * this trusts.
 *
 * The height then takes care of itself: the bitmap's aspect ratio is `weeks : 7`, and the image is
 * scaled to fit whatever vertical space the widget really turns out to have. Targeting a step of
 * [TARGET_STEP_DP] makes that ratio land close to a typical widget's, so almost nothing is left
 * over in either direction.
 */
internal object WidgetHeatmap {

    /**
     * Roughly how much room one column should get on screen, in dp.
     *
     * Chosen so a standard 4x2 widget comes out at about sixteen weeks, whose 16:7 shape is within a
     * couple of percent of that widget's content box — so the graph fills it nearly exactly.
     */
    const val TARGET_STEP_DP = 19f

    /** Fewer than this and it stops reading as a calendar. */
    const val MIN_WEEKS = 6

    /** A year and a bit. Beyond this the contribution calendar has nothing left to show. */
    const val MAX_WEEKS = 53

    /**
     * Cell and gap used for the bitmap itself, not for the screen.
     *
     * The image is scaled up to the widget, so this only sets the resolution it is scaled from.
     * Sixteen weeks at this size is a 538x232 bitmap — about half a megabyte, comfortably inside
     * what can be handed to a widget, where the device-resolution version would have been 3.6 MB and
     * far too big to send.
     */
    const val PREFERRED_STEP_PX = 34

    /**
     * How large the bitmap is allowed to be.
     *
     * A widget update is delivered over binder, which will not carry much more than a megabyte — the
     * device-resolution version of this graph came to 3.6 MB and simply never arrived. Staying under
     * budget matters more than resolution, because the image is scaled up on the way to the screen
     * and the cells are plain rounded squares that survive it.
     *
     * **This bounds one bitmap, not the whole update.** `SizeMode.Exact` composes the widget once
     * per size the launcher offers — two in practice, portrait and landscape — and Glance hands
     * those RemoteViews to a single shared bitmap cache, so the update carries one heatmap per
     * size. The largest [specFor] can produce is 1048x184 ARGB_8888, 771,328 bytes, which makes the
     * worst update about 1.5 MB. Nothing else in either widget adds to that: the rest is text,
     * theme colours and resource-backed checkboxes, no other `Bitmap`, `Icon` or `ImageProvider`.
     *
     * Those are also the bytes Android 17 measures. An app targeting SDK 37 may not hand a widget
     * host a RemoteViews whose bitmaps *and icons* together exceed `1.5 × displayWidth ×
     * displayHeight × 4`, and overshooting is a fatal `IllegalArgumentException` rather than a
     * dropped update. That budget is 12.4 MB on a 1080x1920 phone and still 5.5 MB on a 720x1280
     * one, so 1.5 MB leaves a factor of eight, or of three on the smallest panel likely to run it.
     * A larger display only widens the margin, because the budget grows with the display while
     * [MAX_WEEKS] and this constant keep the bitmap fixed. `WidgetHeatmapTest` pins all of it.
     */
    const val MAX_BITMAP_BYTES = 800_000

    /** The grid for a widget [contentWidthDp] wide, ignoring its self-reported height. */
    fun specFor(contentWidthDp: Float): HeatmapSpec {
        val weeks = (contentWidthDp / TARGET_STEP_DP).toInt().coerceIn(MIN_WEEKS, MAX_WEEKS)

        // A wide widget asks for a lot of columns, and at full resolution that is what pushes the
        // bitmap over what can be delivered. Resolution gives way before history does.
        val affordableStep = sqrt(MAX_BITMAP_BYTES / (4.0 * HeatmapLayout.ROWS * weeks)).toInt()
        val step = min(PREFERRED_STEP_PX, affordableStep).coerceAtLeast(4)

        val gapPx = max(1, (step * (1f - HeatmapLayout.CELL_FRACTION)).roundToInt())
        val cellPx = max(1, step - gapPx)
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
