package io.github.meko123456.habitstreaks.reminders

import java.time.Duration
import java.time.ZonedDateTime

/** When the evening reminder is next due. */
object ReminderTime {

    /**
     * From [now] until the next [hour]:00 on the clock of [now]'s zone: today's if it is still
     * ahead, otherwise tomorrow's. Measured between zoned times, so a night when the clocks change
     * comes out an hour longer or shorter, as the wall clock says it should.
     */
    fun untilNext(now: ZonedDateTime, hour: Int): Duration {
        val today = now.toLocalDate().atTime(hour, 0).atZone(now.zone)
        val next = if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(hour, 0).atZone(now.zone)
        return Duration.between(now, next)
    }
}
