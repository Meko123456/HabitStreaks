package io.github.meko123456.habitstreaks.debug

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import io.github.meko123456.habitstreaks.widget.GithubWidget
import io.github.meko123456.habitstreaks.widget.sampleYear
import java.time.LocalDate

/**
 * Debug-only: the GitHub widget as shipped, drawn from a made-up year instead of the network.
 *
 * Only the data differs — [GithubWidget] keeps its size mode and rendering final — so this is the
 * widget to put on a home screen when there is no token to hand: for screenshots, and for watching
 * the heatmap draw on a new Android release, where the update it sends weighs exactly what the
 * shipping widget's does. The bitmap's size depends on the widget's width, never on the counts.
 */
class DemoGithubWidget : GithubWidget(loadContributions = { Result.success(sampleYear(LocalDate.now())) })

class DemoGithubWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DemoGithubWidget()
}
