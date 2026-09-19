package io.github.meko123456.habitstreaks.widget

import io.github.meko123456.heatmap.HeatmapLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The widget's sizing, which is what was wrong with it twice over.
 *
 * First it asked the library for fixed defaults and let the image scaler shrink the result, so the
 * graph floated small in a widget that was not. Then it sized itself from the height the launcher
 * reported — and Samsung's launcher reports the height a widget could shrink to, not the height it
 * has: 72dp for one measuring 189dp on screen. So now only the width is trusted.
 */
class WidgetHeatmapTest {

    /** The real widget on the S24 Ultra, measured from its host view: 334dp wide. */
    private val realWidgetDp = 334f - 24f // minus the card's padding

    @Test
    fun `the real four by two widget gets about a quarter of a year`() {
        val spec = WidgetHeatmap.specFor(realWidgetDp)
        assertEquals(16, spec.weeks)
    }

    @Test
    fun `the bitmap shape matches the widget it has to fill`() {
        val spec = WidgetHeatmap.specFor(realWidgetDp)
        // The widget's content box measured 1162x510 device px, a ratio of 2.28. The bitmap needs to
        // be close to that or Fit will letterbox it in one direction - which is the whole complaint.
        val bitmapRatio = spec.widthPx.toFloat() / spec.heightPx
        assertTrue("bitmap ratio was $bitmapRatio", bitmapRatio in 2.0f..2.6f)
    }

    @Test
    fun `a wider widget shows more history`() {
        assertTrue(WidgetHeatmap.specFor(600f).weeks > WidgetHeatmap.specFor(300f).weeks)
    }

    @Test
    fun `history is capped at what the calendar holds`() {
        assertEquals(WidgetHeatmap.MAX_WEEKS, WidgetHeatmap.specFor(5_000f).weeks)
    }

    @Test
    fun `a narrow widget still shows a readable stretch`() {
        assertEquals(WidgetHeatmap.MIN_WEEKS, WidgetHeatmap.specFor(10f).weeks)
        assertEquals(WidgetHeatmap.MIN_WEEKS, WidgetHeatmap.specFor(0f).weeks)
        assertEquals(WidgetHeatmap.MIN_WEEKS, WidgetHeatmap.specFor(-50f).weeks)
    }

    @Test
    fun `the widest widget still carries a full year`() {
        val spec = WidgetHeatmap.specFor(2000f)
        assertEquals(WidgetHeatmap.MAX_WEEKS, spec.weeks)
        // History is kept and resolution given up, not the other way round.
        assertTrue("step was ${spec.stepPx}", spec.stepPx < WidgetHeatmap.PREFERRED_STEP_PX)
        assertTrue(spec.bytes() <= WidgetHeatmap.MAX_BITMAP_BYTES)
    }

    // ───────── what the update is allowed to weigh ─────────

    /**
     * Every width, not a sample of them.
     *
     * The budget is held indirectly: `specFor` solves for a step size and the bitmap falls out of
     * it. So a change to the step, the gap or the week count can push a bitmap over without
     * anything in `specFor` looking wrong, and the old sample at 50dp intervals only found the
     * worst case by luck. Half a dp at a time, out past any display that exists.
     */
    @Test
    fun `the bitmap stays inside its budget at every width`() {
        for (halfDp in 0..8_000) {
            val dp = halfDp / 2f
            val bytes = WidgetHeatmap.specFor(dp).bytes()
            assertTrue("$bytes bytes at ${dp}dp wide", bytes <= WidgetHeatmap.MAX_BITMAP_BYTES)
        }
    }

    /** A width that is not a width at all still has to produce a drawable, affordable grid. */
    @Test
    fun `a nonsense width is still inside the budget`() {
        val widths = listOf(
            Float.NaN,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            -1e9f,
            1e9f,
            Float.MIN_VALUE,
        )
        for (dp in widths) {
            val spec = WidgetHeatmap.specFor(dp)
            assertTrue("$dp gave ${spec.bytes()} bytes", spec.bytes() <= WidgetHeatmap.MAX_BITMAP_BYTES)
            assertTrue("$dp gave ${spec.weeks} weeks", spec.weeks in WidgetHeatmap.MIN_WEEKS..WidgetHeatmap.MAX_WEEKS)
            assertTrue(spec.cellPx >= 1 && spec.gapPx >= 1)
        }
    }

    /**
     * The worst case, written down.
     *
     * Not at the widest widget, which trades resolution away: it is at about 741dp, where the grid
     * is still drawn at 27px a column and has just reached 39 of them. Pinning the number means a
     * geometry change that eats the headroom shows up as a failing assertion rather than as a
     * still-passing inequality.
     */
    @Test
    fun `the largest bitmap the widget can produce is 771 KB`() {
        val worst = (0..8_000).map { WidgetHeatmap.specFor(it / 2f) }.maxBy { it.bytes() }
        assertEquals(39, worst.weeks)
        assertEquals(1048, worst.widthPx)
        assertEquals(184, worst.heightPx)
        assertEquals(771_328, worst.bytes())
    }

    /**
     * The limit that kills the process, rather than the one that drops the update.
     *
     * An app targeting SDK 37 may not hand a widget host a RemoteViews whose bitmaps and icons
     * exceed `1.5 × displayWidth × displayHeight × 4`; overshooting is a fatal
     * `IllegalArgumentException`. `SizeMode.Exact` means the update carries one heatmap per size
     * the launcher offers, sharing one bitmap cache, so the figure to compare is a multiple of the
     * per-bitmap budget — not the budget itself.
     *
     * The cap grows with the display, so a big screen is the easy case: the tightest panel likely
     * to be running Android 17 is what the margin should be measured against.
     */
    @Test
    fun `the whole update fits the host's bitmap memory cap`() {
        val worst = (0..8_000).maxOf { WidgetHeatmap.specFor(it / 2f).bytes() }
        val displays = listOf(720 to 1280, 1080 to 1920, 1080 to 2400, 1600 to 2560)
        for ((w, h) in displays) {
            val cap = 6L * w * h // 1.5 × w × h × 4
            // What a launcher actually asks for, with at least the same again to spare.
            assertTrue(
                "${worst.toLong() * SIZE_VARIANTS} bytes against a $cap byte cap on ${w}x$h",
                worst.toLong() * SIZE_VARIANTS <= cap / 2,
            )
            // And if a launcher ever doubled the sizes it offers, it would still fit — with less
            // room, which is the point of stating it separately.
            assertTrue(
                "${worst.toLong() * 2 * SIZE_VARIANTS} bytes against a $cap byte cap on ${w}x$h",
                worst.toLong() * 2 * SIZE_VARIANTS <= cap,
            )
        }
    }

    @Test
    fun `cells and gaps are always positive`() {
        for (dp in 0..2000 step 37) {
            val spec = WidgetHeatmap.specFor(dp.toFloat())
            assertTrue(spec.cellPx >= 1 && spec.gapPx >= 1 && spec.weeks >= 1)
        }
    }

    // ───────── colour scale ─────────

    @Test
    fun `the colour scale uses the busiest day actually on screen`() {
        val endDay = 20_000L
        val firstVisible = HeatmapLayout.firstDay(endDay, weeks = 4)
        val counts = mapOf(
            firstVisible - 200 to 500, // a huge day from last winter, off screen
            endDay - 1 to 8,
            endDay to 30,
        )
        // Without this the library scales to 500 and washes every visible week out to nearly empty.
        assertEquals(30, WidgetHeatmap.visibleMax(counts, endDay, weeks = 4))
    }

    @Test
    fun `an empty stretch has no scale of its own`() {
        assertNull(WidgetHeatmap.visibleMax(emptyMap(), endDay = 20_000L, weeks = 4))
        assertNull(WidgetHeatmap.visibleMax(mapOf(1L to 9), endDay = 20_000L, weeks = 4))
    }

    private companion object {
        /**
         * How many heatmaps ride in one update.
         *
         * Glance's `SizeMode.Exact` composes once per entry in the launcher's
         * `OPTION_APPWIDGET_SIZES`, and falls back to landscape-plus-portrait when that is absent —
         * two either way, on every launcher that has been looked at.
         */
        const val SIZE_VARIANTS = 2

        /** What the bitmap actually costs: `HeatmapBitmap.render` builds it ARGB_8888. */
        fun HeatmapSpec.bytes(): Int = widthPx * heightPx * 4
    }
}
