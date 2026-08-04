package io.github.meko123456.habitstreaks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreakEngineTest {

    private val today = 20_000L

    // --- currentStreak ---

    @Test
    fun `empty history has no streak`() {
        assertEquals(0, StreakEngine.currentStreak(emptySet(), today))
    }

    @Test
    fun `single completion today is a 1-day streak`() {
        assertEquals(1, StreakEngine.currentStreak(setOf(today), today))
    }

    @Test
    fun `streak ending yesterday still counts (grace until midnight)`() {
        val days = setOf(today - 3, today - 2, today - 1)
        assertEquals(3, StreakEngine.currentStreak(days, today))
    }

    @Test
    fun `streak ending two days ago is broken`() {
        val days = setOf(today - 4, today - 3, today - 2)
        assertEquals(0, StreakEngine.currentStreak(days, today))
    }

    @Test
    fun `today plus consecutive history counts fully`() {
        val days = setOf(today - 2, today - 1, today)
        assertEquals(3, StreakEngine.currentStreak(days, today))
    }

    @Test
    fun `gap resets current streak to the recent run`() {
        val days = setOf(today - 5, today - 4, today - 1, today)
        assertEquals(2, StreakEngine.currentStreak(days, today))
    }

    // --- longestStreak ---

    @Test
    fun `longest of empty history is zero`() {
        assertEquals(0, StreakEngine.longestStreak(emptySet()))
    }

    @Test
    fun `longest picks the biggest run across gaps`() {
        val days = setOf(1L, 2L, 3L, 4L, 10L, 11L, 20L)
        assertEquals(4, StreakEngine.longestStreak(days))
    }

    @Test
    fun `longest equals size when fully consecutive`() {
        val days = (100L..120L).toSet()
        assertEquals(21, StreakEngine.longestStreak(days))
    }

    @Test
    fun `longest handles single isolated days`() {
        val days = setOf(5L, 7L, 9L)
        assertEquals(1, StreakEngine.longestStreak(days))
    }

    // --- completionRate ---

    @Test
    fun `rate over empty range is null`() {
        assertNull(StreakEngine.completionRate(setOf(1L), firstDay = 10L, today = 9L))
    }

    @Test
    fun `rate counts only days inside the range`() {
        val days = setOf(8L, 10L, 11L, 99L)
        // range 10..13 -> 4 days, 2 done
        assertEquals(0.5f, StreakEngine.completionRate(days, firstDay = 10L, today = 13L))
    }

    @Test
    fun `perfect completion is 1`() {
        val days = setOf(10L, 11L, 12L)
        assertEquals(1f, StreakEngine.completionRate(days, firstDay = 10L, today = 12L))
    }

    @Test
    fun `single-day range works`() {
        assertEquals(1f, StreakEngine.completionRate(setOf(10L), firstDay = 10L, today = 10L))
        assertEquals(0f, StreakEngine.completionRate(emptySet(), firstDay = 10L, today = 10L))
    }
}
