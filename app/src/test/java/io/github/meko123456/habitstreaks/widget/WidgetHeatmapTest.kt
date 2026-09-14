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
    fun `the bitmap stays small enough to hand to a widget`() {
        // A widget update travels over binder; the device-resolution version of this was 3.6 MB and
        // never arrived. Anything under about a megabyte is safe.
        for (dp in 100..2000 step 50) {
            val spec = WidgetHeatmap.specFor(dp.toFloat())
            val bytes = spec.widthPx * spec.heightPx * 4
            assertTrue("$bytes bytes at ${dp}dp wide", bytes <= WidgetHeatmap.MAX_BITMAP_BYTES)
        }
    }

    @Test
    fun `the widest widget still carries a full year`() {
        val spec = WidgetHeatmap.specFor(2000f)
        assertEquals(WidgetHeatmap.MAX_WEEKS, spec.weeks)
        // History is kept and resolution given up, not the other way round.
        assertTrue("step was ${spec.stepPx}", spec.stepPx < WidgetHeatmap.PREFERRED_STEP_PX)
        assertTrue(spec.widthPx * spec.heightPx * 4 <= WidgetHeatmap.MAX_BITMAP_BYTES)
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
}
