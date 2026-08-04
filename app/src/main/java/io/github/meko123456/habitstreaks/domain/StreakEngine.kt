package io.github.meko123456.habitstreaks.domain

/**
 * Pure streak math over sets of completed days (LocalDate.toEpochDay() values).
 * No Android or java.time dependencies so it is trivially unit-testable and
 * reusable from any platform.
 */
object StreakEngine {

    /**
     * Consecutive completed days ending at [today] or [today] - 1.
     * A streak is not broken by today being still incomplete — you have
     * until midnight — but it must reach at least yesterday to count.
     */
    fun currentStreak(completedDays: Set<Long>, today: Long): Int {
        val anchor = when {
            today in completedDays -> today
            (today - 1) in completedDays -> today - 1
            else -> return 0
        }
        var streak = 0
        var day = anchor
        while (day in completedDays) {
            streak++
            day--
        }
        return streak
    }

    /** Longest run of consecutive completed days anywhere in history. */
    fun longestStreak(completedDays: Set<Long>): Int {
        var longest = 0
        for (day in completedDays) {
            // Only start counting from the first day of a run.
            if ((day - 1) in completedDays) continue
            var length = 1
            while ((day + length) in completedDays) length++
            if (length > longest) longest = length
        }
        return longest
    }

    /**
     * Fraction of days completed in [firstDay]..[today] (both inclusive),
     * or null when the range is empty. Days after [today] are ignored.
     */
    fun completionRate(completedDays: Set<Long>, firstDay: Long, today: Long): Float? {
        if (today < firstDay) return null
        val total = today - firstDay + 1
        val done = completedDays.count { it in firstDay..today }
        return done.toFloat() / total
    }
}
