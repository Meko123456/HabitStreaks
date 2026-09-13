package io.github.meko123456.habitstreaks.domain

import java.time.Duration
import java.time.ZonedDateTime

/**
 * Pure arithmetic for "which day is it" and "when does the day roll over".
 *
 * A streak app has to know when today ends. Reading `LocalDate.now()` once and holding it — which is
 * what the home screen and the habit list both used to do — means an app left open overnight is
 * still working from yesterday: a habit checked off yesterday shows as done, and tapping it deletes
 * a row for a day that is no longer today, which deletes nothing at all. Room's invalidation is
 * per-row, so nothing re-emits and the screen stays stuck until the Activity is recreated. In a
 * streak app that silently breaks the streak.
 */
object DayClock {

    /** The local epoch day of [now]. */
    fun epochDay(now: ZonedDateTime): Long = now.toLocalDate().toEpochDay()

    /**
     * Milliseconds from [now] until the next local midnight in [now]'s own zone.
     *
     * Never less than 1, so a caller looping on it cannot spin — not exactly at midnight, and not
     * across a daylight-saving change that makes the next day start an hour early.
     */
    fun millisUntilNextMidnight(now: ZonedDateTime): Long {
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        return Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L)
    }
}
