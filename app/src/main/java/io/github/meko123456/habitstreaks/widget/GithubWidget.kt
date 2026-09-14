package io.github.meko123456.habitstreaks.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.meko123456.habitstreaks.MainActivity
import io.github.meko123456.habitstreaks.data.github.GithubClient
import io.github.meko123456.habitstreaks.data.github.GithubContributions
import io.github.meko123456.habitstreaks.data.github.TokenStore
import io.github.meko123456.heatmap.HeatmapBitmap
import java.time.LocalDate
import kotlin.math.roundToInt

class GithubWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GithubWidget()
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

    /** The header line plus a heatmap drawn to fill whatever is left of the widget. */
    @androidx.compose.runtime.Composable
    private fun Graph(contributions: GithubContributions) {
        val size = LocalSize.current
        val density = LocalContext.current.resources.displayMetrics.density
        val today = LocalDate.now()
        val todayCount = contributions.countsByDay[today.toEpochDay()] ?: 0

        val compact = size.height.value < 120f
        val headerSp = if (compact) 13f else 15f
        // Roughly what the header row occupies, so the graph can claim the rest. Deliberately a
        // small over-estimate: leaving a spare dp is invisible, overflowing hides a row of the graph.
        val headerDp = headerSp * 1.5f + 6f

        Row(
            modifier = GlanceModifier.fillMaxWidth().wrapContentHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${today.dayOfMonth.pad()}/${today.monthValue.pad()}",
                style = TextStyle(
                    fontSize = headerSp.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface,
                ),
            )
            Spacer(GlanceModifier.padding(horizontal = 4.dp))
            Text(
                text = todayCount.contributions(),
                style = TextStyle(
                    fontSize = headerSp.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (todayCount > 0) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (!compact) {
                Text(
                    text = "@${contributions.login}",
                    style = TextStyle(
                        fontSize = (headerSp - 2f).sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
                )
            }
        }

        val heatmap = renderHeatmap(
            counts = contributions.countsByDay,
            endDay = today.toEpochDay(),
            widthPx = ((size.width.value - PADDING * 2) * density).roundToInt(),
            heightPx = ((size.height.value - PADDING * 2 - headerDp) * density).roundToInt(),
        )
        if (heatmap != null) {
            Image(
                provider = ImageProvider(heatmap),
                contentDescription = "$todayCount contributions today, " +
                    "${contributions.total} in the last year",
                // The bitmap is already built for this box, so Fit scales it barely at all -
                // and Fit rather than FillBounds so cells stay square if the maths is a dp out.
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier.fillMaxSize().padding(top = 4.dp),
            )
        }
    }

    private companion object {
        const val PADDING = 12f

        fun Int.pad(): String = if (this < 10) "0$this" else "$this"

        fun Int.contributions(): String = when (this) {
            0 -> "no contributions yet"
            1 -> "1 contribution"
            else -> "$this contributions"
        }

        /** Draws the heatmap at the size it will actually occupy, or nothing if it cannot fit. */
        fun renderHeatmap(
            counts: Map<Long, Int>,
            endDay: Long,
            widthPx: Int,
            heightPx: Int,
        ): android.graphics.Bitmap? {
            val spec = WidgetHeatmap.specFor(widthPx, heightPx) ?: return null
            return HeatmapBitmap.render(
                counts = counts,
                endDay = endDay,
                weeks = spec.weeks,
                cellPx = spec.cellPx,
                gapPx = spec.gapPx,
                maxCount = WidgetHeatmap.visibleMax(counts, endDay, spec.weeks),
            )
        }
    }
}

class RefreshGithubWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        GithubWidget().updateAll(context)
    }
}
