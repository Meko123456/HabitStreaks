package io.github.meko123456.habitstreaks.widget

import io.github.meko123456.heatmap.HeatmapLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The widget's sizing, which is the thing that was actually wrong with it.
 *
 * The old widget asked the library for its defaults — 26 weeks of 24 px cells, a 774x204 strip —
 * and let the image scaler shrink that to fit the width, so a short wide graph floated in the middle
 * of the space with most of the height unused and every cell tiny.
 */
class WidgetHeatmapTest {

    /** A 4x2 widget on a ~3x density phone, minus padding: roughly what the default looks like. */
    private val phoneWidth = 900
    private val phoneHeight = 160

    @Test
    fun `the grid fills the height it is given`() {
        val spec = WidgetHeatmap.specFor(phoneWidth, phoneHeight)!!
        assertTrue(
            "seven rows came to ${spec.heightPx}px of an available ${phoneHeight}px",
            spec.heightPx >= phoneHeight - spec.stepPx && spec.heightPx <= phoneHeight,
        )
    }

    @Test
    fun `the grid fills the width it is given`() {
        val spec = WidgetHeatmap.specFor(phoneWidth, phoneHeight)!!
        assertTrue(
            "weeks came to ${spec.widthPx}px of an available ${phoneWidth}px",
            spec.widthPx <= phoneWidth && spec.widthPx > phoneWidth - spec.stepPx,
        )
    }

    @Test
    fun `a taller widget gets bigger cells`() {
        val short = WidgetHeatmap.specFor(phoneWidth, 120)!!
        val tall = WidgetHeatmap.specFor(phoneWidth, 320)!!
        assertTrue("short=${short.cellPx} tall=${tall.cellPx}", tall.cellPx > short.cellPx)
    }

    @Test
    fun `a wider widget shows more history`() {
        val narrow = WidgetHeatmap.specFor(400, phoneHeight)!!
        val wide = WidgetHeatmap.specFor(1200, phoneHeight)!!
        assertTrue("narrow=${narrow.weeks} wide=${wide.weeks}", wide.weeks > narrow.weeks)
    }

    @Test
    fun `history is capped at what the calendar holds`() {
        val huge = WidgetHeatmap.specFor(20_000, 120)!!
        assertEquals(WidgetHeatmap.MAX_WEEKS, huge.weeks)
    }

    @Test
    fun `at least one week is always drawn`() {
        val sliver = WidgetHeatmap.specFor(5, 140)
        assertNotNull(sliver)
        assertTrue(sliver!!.weeks >= 1)
    }

    @Test
    fun `a box too small for readable cells draws nothing`() {
        assertNull(WidgetHeatmap.specFor(900, 20))
        assertNull(WidgetHeatmap.specFor(0, 160))
        assertNull(WidgetHeatmap.specFor(900, 0))
        assertNull(WidgetHeatmap.specFor(-10, -10))
    }

    @Test
    fun `cells are always at least a pixel with a visible gap`() {
        for (h in 42..400 step 7) {
            val spec = WidgetHeatmap.specFor(900, h) ?: continue
            assertTrue("cell was ${spec.cellPx} at height $h", spec.cellPx >= 1)
            assertTrue("gap was ${spec.gapPx} at height $h", spec.gapPx >= 1)
        }
    }

    @Test
    fun `the colour scale uses the busiest day actually on screen`() {
        val endDay = 20_000L
        val firstVisible = HeatmapLayout.firstDay(endDay, weeks = 4)
        val counts = mapOf(
            firstVisible - 200 to 500,
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

    /**
     * The real widget on the S24 Ultra: 401x227 dp at density 3.75, reported by the launcher.
     *
     * Pinned because this is the case the complaint was about — the graph looking tiny in a widget
     * that was not tiny — and because the old numbers are worth keeping next to the new ones.
     */
    @Test
    fun `the real four by two widget gets cells worth looking at`() {
        val density = 3.75f
        val padding = 12f
        val headerDp = 15f * 1.5f + 6f
        val widthPx = ((401f - padding * 2) * density).toInt()
        val heightPx = ((227f - padding * 2 - headerDp) * density).toInt()

        val spec = WidgetHeatmap.specFor(widthPx, heightPx)!!

        // Pinned exactly, so a change to the header estimate or the cell fraction has to be a
        // deliberate one. Before this, the library's defaults gave a 774x204 strip that Fit scaled
        // to the width: cells landed around 44px and about two fifths of the height went unused.
        assertEquals(78, spec.cellPx)
        assertEquals(17, spec.gapPx)
        assertEquals(15, spec.weeks)
        assertTrue(
            "the grid is ${spec.heightPx}px tall in a ${heightPx}px box",
            spec.heightPx >= heightPx - spec.stepPx,
        )
    }
}
