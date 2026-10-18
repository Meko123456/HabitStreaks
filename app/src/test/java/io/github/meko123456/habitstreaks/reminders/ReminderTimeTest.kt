package io.github.meko123456.habitstreaks.reminders

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTimeTest {

    private val tbilisi = ZoneId.of("Asia/Tbilisi")
    private fun at(zone: ZoneId, y: Int, m: Int, d: Int, h: Int, min: Int) = ZonedDateTime.of(y, m, d, h, min, 0, 0, zone)

    @Test
    fun theMorningWaitsForTonight() =
        assertEquals(Duration.ofHours(9).plusMinutes(6), ReminderTime.untilNext(at(tbilisi, 2026, 10, 18, 10, 54), 20))

    @Test
    fun atTheHourItIsTomorrowsTurn() =
        assertEquals(Duration.ofHours(24), ReminderTime.untilNext(at(tbilisi, 2026, 10, 18, 20, 0), 20))

    @Test
    fun aLateRunStillAimsAtTomorrowsHour() =
        // Held back to 21:30, the next one is still at 20:00 tomorrow, not 21:30 tomorrow.
        assertEquals(Duration.ofHours(22).plusMinutes(30), ReminderTime.untilNext(at(tbilisi, 2026, 10, 18, 21, 30), 20))

    @Test
    fun theNightTheClocksGoBackIsAnHourLonger() {
        // London leaves summer time at 02:00 on 25 October 2026. From 20:30 the evening before,
        // the next 20:00 on the wall clock is 24 h 30 min away, not the 23 h 30 min a plain
        // local-time difference gives.
        val london = ZoneId.of("Europe/London")
        assertEquals(Duration.ofHours(24).plusMinutes(30), ReminderTime.untilNext(at(london, 2026, 10, 24, 20, 30), 20))
    }
}
