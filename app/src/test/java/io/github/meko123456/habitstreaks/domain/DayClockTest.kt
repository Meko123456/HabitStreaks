package io.github.meko123456.habitstreaks.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** When today ends — the arithmetic a streak app cannot get wrong. */
@OptIn(ExperimentalCoroutinesApi::class)
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

    // --- todayTicks: which day the screen is on ---------------------------------------------------
    // The wait for midnight runs in virtual time here, which stands still unless the test moves it.
    // That is what uptime does while a phone is in deep sleep, as the wall clock moves on.

    private fun october(day: Int) = LocalDate.of(2026, 10, day).toEpochDay()

    @Test
    fun `a recheck reads the date again when the wait for midnight has not run out`() = runTest {
        var now = at(tbilisi, 2026, 10, 7, 23)
        val rechecks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val days = mutableListOf<Long>()
        val ticks = launch { DayClock.todayTicks(rechecks) { now }.toList(days) }
        runCurrent()

        now = at(tbilisi, 2026, 10, 8, 7, 30) // a night asleep: the clock moved, the wait did not
        runCurrent()
        assertEquals("the wait alone still says yesterday", listOf(october(7)), days)

        rechecks.emit(Unit) // the screen comes back
        runCurrent()
        assertEquals(listOf(october(7), october(8)), days)
        ticks.cancel()
    }

    @Test
    fun `midnight still arrives on its own while the phone stays awake`() = runTest {
        var now = at(tbilisi, 2026, 10, 7, 23)
        val days = mutableListOf<Long>()
        val ticks = launch { DayClock.todayTicks(emptyFlow()) { now }.toList(days) }
        runCurrent()

        now = at(tbilisi, 2026, 10, 8, 0)
        advanceTimeBy(60 * 60 * 1000L + 1)
        assertEquals(listOf(october(7), october(8)), days)
        ticks.cancel()
    }

    @Test
    fun `a recheck on the same day sends nothing new`() = runTest {
        val now = at(tbilisi, 2026, 10, 7, 9)
        val rechecks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val days = mutableListOf<Long>()
        val ticks = launch { DayClock.todayTicks(rechecks) { now }.toList(days) }
        runCurrent()
        rechecks.emit(Unit)
        runCurrent()
        assertEquals(listOf(october(7)), days)
        ticks.cancel()
    }
}
