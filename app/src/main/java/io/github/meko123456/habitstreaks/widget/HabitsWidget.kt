package io.github.meko123456.habitstreaks.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.unit.ColorProvider
import io.github.meko123456.habitstreaks.MainActivity
import io.github.meko123456.habitstreaks.data.Completion
import io.github.meko123456.habitstreaks.data.HabitDatabase
import java.time.LocalDate

class HabitsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitsWidget()
}

/** Home-screen widget: today's habits with tap-to-check, straight into Room. */
class HabitsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = HabitDatabase.get(context).habitDao()
        val habits = dao.habitsOnce()
        val today = LocalDate.now().toEpochDay()
        val doneIds = dao.completedHabitIdsOn(today).toSet()

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(16.dp)
                        .padding(12.dp),
                ) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionStartActivity<MainActivity>()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "HabitStreaks",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface,
                            ),
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            text = "${doneIds.size}/${habits.size} today",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                        )
                    }
                    if (habits.isEmpty()) {
                        Text(
                            text = "No habits yet — tap to add one",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                            modifier = GlanceModifier
                                .padding(top = 8.dp)
                                .clickable(actionStartActivity<MainActivity>()),
                        )
                    }
                    habits.forEach { habit ->
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CheckBox(
                                checked = habit.id in doneIds,
                                onCheckedChange = actionRunCallback<ToggleHabitAction>(
                                    actionParametersOf(ToggleHabitAction.HabitId to habit.id),
                                ),
                            )
                            Text(
                                text = "${habit.emoji} ${habit.name}",
                                style = TextStyle(color = GlanceTheme.colors.onSurface),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[HabitId] ?: return
        val dao = HabitDatabase.get(context).habitDao()
        val today = LocalDate.now().toEpochDay()
        if (habitId in dao.completedHabitIdsOn(today)) {
            dao.removeCompletion(habitId, today)
        } else {
            dao.addCompletion(Completion(habitId, today))
        }
        HabitsWidget().updateAll(context)
    }

    companion object {
        val HabitId = ActionParameters.Key<Long>("habitId")
    }
}
