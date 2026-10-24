package io.github.meko123456.habitstreaks.widget

import io.github.meko123456.habitstreaks.data.github.GithubContributions
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/**
 * A made-up year with the texture of a real one: busy weekdays, quieter weekends, and days off.
 * Seeded, so it is the same year every time. The widget picker's preview draws it, and so does
 * the debug build's demo widget, which needs no GitHub token.
 */
internal fun sampleYear(today: LocalDate): GithubContributions {
    val random = Random(42)
    val counts = buildMap {
        for (daysAgo in 0L until SAMPLE_DAYS) {
            val day = today.minusDays(daysAgo)
            val weekend = day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY
            if (random.nextDouble() < if (weekend) 0.35 else 0.8) {
                put(day.toEpochDay(), 1 + random.nextInt(if (weekend) 4 else 12))
            }
        }
    }
    return GithubContributions(
        login = "sample",
        total = counts.values.sum(),
        countsByDay = counts,
        firstDay = today.minusDays(SAMPLE_DAYS - 1).toEpochDay(),
    )
}

/** 53 weeks: the most the widget ever draws, so the widest widget has no empty columns. */
private const val SAMPLE_DAYS = 53L * 7
