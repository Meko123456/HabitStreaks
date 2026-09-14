package io.github.meko123456.habitstreaks.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.meko123456.habitstreaks.MainActivity
import io.github.meko123456.habitstreaks.data.github.GithubClient
import io.github.meko123456.habitstreaks.data.github.GithubContributions
import io.github.meko123456.habitstreaks.data.github.TokenStore
import io.github.meko123456.heatmap.HeatmapBitmap
import java.time.LocalDate

class GithubWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GithubWidget()

    /** First widget added: start keeping it current. */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshWorker.schedule(context)
    }

    /** Last widget removed: stop asking GitHub anything. */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefreshWorker.cancel(context)
    }
}

/** Home-screen widget rendering the real GitHub contribution calendar. */
class GithubWidget : GlanceAppWidget() {

    /**
     * Exact, not the default Single.
     *
     * With `SizeMode.Single` a widget is composed once against its *minimum* declared size, so
     * `LocalSize.current` reports 250×90 however large the user has actually dragged it — and the
     * heatmap was drawn for that minimum and then letterboxed into whatever space was really there.
     * Exact recomposes per size, which is what lets the graph be drawn at the size it will occupy.
     */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val token = TokenStore(context).load()
        val result = token?.let { GithubClient().fetchContributions(it) }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(16.dp)
                        .padding(PADDING.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                ) {
                    when {
                        result == null -> Message("Connect GitHub in HabitStreaks to see your contribution graph")
                        result.isFailure -> {
                            Message("Couldn't load contributions")
                            Text(
                                text = "↻ Refresh",
                                style = TextStyle(color = GlanceTheme.colors.primary),
                                modifier = GlanceModifier
                                    .padding(top = 8.dp)
                                    .clickable(actionRunCallback<RefreshGithubWidgetAction>()),
                            )
                        }
                        else -> Graph(result.getOrThrow())
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Message(text: String) {
        Text(text = text, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
    }

    /** One header line, then a heatmap filling everything below it. */
    @androidx.compose.runtime.Composable
    private fun ColumnScope.Graph(contributions: GithubContributions) {
        val size = LocalSize.current
        val today = LocalDate.now()
        val todayCount = contributions.countsByDay[today.toEpochDay()] ?: 0

        // One Text, not a Row of three. The first attempt put the date, the count and the handle in
        // a Row and gave the count a weight to push the handle right; on the device only the date
        // survived - the weighted child measured to nothing and the rest went with it. A single
        // string cannot collapse.
        Text(
            text = "${today.dayOfMonth.pad()}/${today.monthValue.pad()}  ${todayCount.contributions()}",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (todayCount > 0) GlanceTheme.colors.primary else GlanceTheme.colors.onSurface,
            ),
            maxLines = 1,
            modifier = GlanceModifier.fillMaxWidth(),
        )

        val spec = WidgetHeatmap.specFor(size.width.value - PADDING * 2)
        Image(
            provider = ImageProvider(
                HeatmapBitmap.render(
                    counts = contributions.countsByDay,
                    endDay = today.toEpochDay(),
                    weeks = spec.weeks,
                    cellPx = spec.cellPx,
                    gapPx = spec.gapPx,
                    maxCount = WidgetHeatmap.visibleMax(contributions.countsByDay, today.toEpochDay(), spec.weeks),
                ),
            ),
            contentDescription = "$todayCount contributions today, ${contributions.total} in the last year",
            // Fit keeps the cells square; defaultWeight is what actually hands the image the rest of
            // the column. fillMaxSize looked equivalent and left it with no height at all.
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(top = 6.dp),
        )
    }

    private companion object {
        const val PADDING = 12f

        fun Int.pad(): String = if (this < 10) "0$this" else "$this"

        fun Int.contributions(): String = when (this) {
            0 -> "no contributions yet"
            1 -> "1 contribution"
            else -> "$this contributions"
        }
    }
}

class RefreshGithubWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        GithubWidget().updateAll(context)
    }
}
