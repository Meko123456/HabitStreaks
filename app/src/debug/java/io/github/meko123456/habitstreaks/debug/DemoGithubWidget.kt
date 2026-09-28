package io.github.meko123456.habitstreaks.debug

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import io.github.meko123456.habitstreaks.data.github.GithubContributions
import io.github.meko123456.habitstreaks.widget.GithubWidget
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/**
 * Debug-only: the GitHub widget as shipped, drawn from a made-up year instead of the network.
 *
 * Only the data differs — [GithubWidget] keeps its size mode and rendering final — so this is the
 * widget to put on a home screen when there is no token to hand: for screenshots, and for watching
 * the heatmap draw on a new Android release, where the update it sends weighs exactly what the
 * shipping widget's does. The bitmap's size depends on the widget's width, never on the counts.
 */
class DemoGithubWidget : GithubWidget(loadContributions = { Result.success(demoYear(LocalDate.now())) })

class DemoGithubWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DemoGithubWidget()
}

/** A year with the texture of a real one: busy weekdays, quieter weekends, and days off. */
internal fun demoYear(today: LocalDate): GithubContributions {
    val random = Random(42)
    val counts = buildMap {
        for (daysAgo in 0L until DAYS) {
            val day = today.minusDays(daysAgo)
            val weekend = day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY
            if (random.nextDouble() < if (weekend) 0.35 else 0.8) {
                put(day.toEpochDay(), 1 + random.nextInt(if (weekend) 4 else 12))
            }
        }
    }
    return GithubContributions(login = "demo", total = counts.values.sum(), countsByDay = counts)
}

/** 53 weeks: the most the widget ever draws, so the widest widget has no empty columns. */
private const val DAYS = 53L * 7
