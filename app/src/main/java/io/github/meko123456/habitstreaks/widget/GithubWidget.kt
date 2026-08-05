package io.github.meko123456.habitstreaks.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.meko123456.habitstreaks.MainActivity
import io.github.meko123456.habitstreaks.data.github.GithubClient
import io.github.meko123456.habitstreaks.data.github.TokenStore
import java.time.LocalDate

class GithubWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GithubWidget()
}

/** Home-screen widget rendering the real GitHub contribution calendar. */
class GithubWidget : GlanceAppWidget() {

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
                        .padding(12.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                ) {
                    when {
                        result == null -> Text(
                            text = "Connect GitHub in HabitStreaks to see your contribution graph",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                        )
                        result.isFailure -> Text(
                            text = "Couldn't load contributions — tap ↻ to retry",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                        )
                        else -> {
                            val contributions = result.getOrThrow()
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "@${contributions.login}",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = GlanceTheme.colors.onSurface,
                                    ),
                                )
                                Spacer(GlanceModifier.width(8.dp))
                                Text(
                                    text = "${contributions.total} contributions",
                                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                                )
                            }
                            Image(
                                provider = ImageProvider(
                                    HeatmapBitmap.render(
                                        counts = contributions.countsByDay,
                                        endDay = LocalDate.now().toEpochDay(),
                                    ),
                                ),
                                contentDescription = "GitHub contribution heatmap",
                                modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp),
                            )
                        }
                    }
                    if (result != null && result.isFailure) {
                        Text(
                            text = "↻ Refresh",
                            style = TextStyle(color = GlanceTheme.colors.primary),
                            modifier = GlanceModifier
                                .padding(top = 8.dp)
                                .clickable(actionRunCallback<RefreshGithubWidgetAction>()),
                        )
                    }
                }
            }
        }
    }
}

class RefreshGithubWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        GithubWidget().updateAll(context)
    }
}
