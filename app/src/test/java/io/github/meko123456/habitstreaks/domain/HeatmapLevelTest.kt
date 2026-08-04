package io.github.meko123456.habitstreaks.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HeatmapLevelTest {

    @Test
    fun `zero or negative count is level 0`() {
        assertEquals(0, HeatmapLevel.levelFor(0, 10))
        assertEquals(0, HeatmapLevel.levelFor(-1, 10))
    }

    @Test
    fun `zero max is level 0 regardless of count`() {
        assertEquals(0, HeatmapLevel.levelFor(3, 0))
    }

    @Test
    fun `the busiest day is always level 4`() {
        assertEquals(4, HeatmapLevel.levelFor(1, 1))
        assertEquals(4, HeatmapLevel.levelFor(7, 7))
        assertEquals(4, HeatmapLevel.levelFor(9, 7))
    }

    @Test
    fun `intermediate counts spread across levels 1-3`() {
        // max 6: counts 1,2 -> 1; 3,4 -> 2; 5 -> 3
        assertEquals(1, HeatmapLevel.levelFor(1, 6))
        assertEquals(1, HeatmapLevel.levelFor(2, 6))
        assertEquals(2, HeatmapLevel.levelFor(3, 6))
        assertEquals(2, HeatmapLevel.levelFor(4, 6))
        assertEquals(3, HeatmapLevel.levelFor(5, 6))
    }

    @Test
    fun `small max still uses low levels first`() {
        // max 2: count 1 -> ceil(1*3/2)=2
        assertEquals(2, HeatmapLevel.levelFor(1, 2))
        assertEquals(4, HeatmapLevel.levelFor(2, 2))
    }

    @Test
    fun `never exceeds bounds`() {
        for (max in 1..10) for (count in 0..12) {
            val level = HeatmapLevel.levelFor(count, max)
            assert(level in 0..HeatmapLevel.MAX_LEVEL) { "count=$count max=$max -> $level" }
        }
    }
}
