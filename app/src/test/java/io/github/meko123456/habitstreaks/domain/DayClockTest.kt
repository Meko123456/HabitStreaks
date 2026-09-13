package io.github.meko123456.habitstreaks.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** When today ends — the arithmetic a streak app cannot get wrong. */
class DayClockTest {

    private val tbilisi = ZoneId.of("Asia/Tbilisi")
    private val london = ZoneId.of("Europe/London")

    private fun at(zone: ZoneId, y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        ZonedDateTime.of(LocalDateTime.of(y, m, d, h, min), zone)

    @Test
    fun `the epoch day is the local one`() {
        assertEquals(
            LocalDate.of(2026, 9, 14).toEpochDay(),
            DayClock.epochDay(at(tbilisi, 2026, 9, 14, 23, 59)),
        )
        // The same instant is still the 14th in Tbilisi and already the 15th nowhere else that
        // matters here — the point is that the zone is the one the user is in.
        assertEquals(
            LocalDate.of(2026, 9, 15).toEpochDay(),
            DayClock.epochDay(at(tbilisi, 2026, 9, 15, 0, 1)),
        )
    }

    @Test
    fun `the wait is the time left until local midnight`() {
        assertEquals(60 * 60 * 1000L, DayClock.millisUntilNextMidnight(at(tbilisi, 2026, 9, 14, 23)))
        assertEquals(24 * 60 * 60 * 1000L, DayClock.millisUntilNextMidnight(at(tbilisi, 2026, 9, 14, 0)))
    }

    @Test
    fun `the wait is never zero so a loop cannot spin`() {
        // Exactly midnight is the case that would otherwise return 0 and burn the CPU.
        assertTrue(DayClock.millisUntilNextMidnight(at(tbilisi, 2026, 9, 14, 0)) >= 1)
        assertTrue(DayClock.millisUntilNextMidnight(at(london, 2026, 3, 29, 0)) >= 1)
    }

    @Test
    fun `a day that loses an hour is twenty three hours long`() {
        // Europe/London springs forward at 01:00 on 2026-03-29.
        assertEquals(23 * 60 * 60 * 1000L, DayClock.millisUntilNextMidnight(at(london, 2026, 3, 29, 0)))
    }

    @Test
    fun `a day that gains an hour is twenty five hours long`() {
        // And back again on 2026-10-25.
        assertEquals(25 * 60 * 60 * 1000L, DayClock.millisUntilNextMidnight(at(london, 2026, 10, 25, 0)))
    }
}
