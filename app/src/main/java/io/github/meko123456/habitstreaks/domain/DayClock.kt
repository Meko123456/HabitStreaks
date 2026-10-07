package io.github.meko123456.habitstreaks.domain

import java.time.Duration
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

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

    /**
     * Today's epoch day: now, again at every local midnight, and again whenever [rechecks] emits.
     *
     * The wait for midnight alone is not enough on a phone. delay() runs on Handler.postDelayed,
     * whose clock is uptime, and uptime stops while the phone is in deep sleep: after a night
     * asleep the wait has not run out, and the screen still shows yesterday. A recheck, sent when
     * the screen comes back, reads the date again and starts a fresh wait from there.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun todayTicks(rechecks: Flow<Unit>, now: () -> ZonedDateTime = ZonedDateTime::now): Flow<Long> =
        rechecks
            .onStart { emit(Unit) }
            .flatMapLatest {
                flow {
                    while (true) {
                        val at = now()
                        emit(epochDay(at))
                        delay(millisUntilNextMidnight(at))
                    }
                }
            }
            .distinctUntilChanged()
}
