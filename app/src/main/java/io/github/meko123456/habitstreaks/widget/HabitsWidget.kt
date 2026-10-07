package io.github.meko123456.habitstreaks.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
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
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
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
import io.github.meko123456.habitstreaks.data.Habit
import io.github.meko123456.habitstreaks.data.HabitDatabase
import java.time.LocalDate

class HabitsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitsWidget()

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        MidnightRefresh.cancel(context)
    }
}

/** Home-screen widget: today's habits with tap-to-check, straight into Room. */
class HabitsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Here as well as in refreshAll: after a reboot, which clears alarms, the system's own
        // update of the widget is the first chance to set it again.
        MidnightRefresh.schedule(context)
        val first = TodayHabits.load(context)
        provideContent {
            // Read here, again for every version refreshAll writes. While a widget's session is
            // open (about 45 s after each update), update() recomposes this content instead of
            // calling provideGlance again, so data read above provideContent never changed: a box
            // ticked on the widget was saved, but the widget went on showing the old state.
            val version = currentState(VERSION) ?: 0
            val today by produceState(first, version) { value = TodayHabits.load(context) }
            Content(today.habits, today.doneIds)
        }
    }

    companion object {
        val VERSION = intPreferencesKey("version")

        /** Redraws every placed habits widget from the database, and keeps the midnight alarm set. */
        suspend fun refreshAll(context: Context) {
            val widget = HabitsWidget()
            val ids = GlanceAppWidgetManager(context).getGlanceIds(HabitsWidget::class.java)
            ids.forEach { id ->
                updateAppWidgetState(context, id) { it[VERSION] = (it[VERSION] ?: 0) + 1 }
                widget.update(context, id)
            }
            if (ids.isNotEmpty()) MidnightRefresh.schedule(context)
        }
    }
}

/** What the widget shows: every habit, and which of them are done today. */
private data class TodayHabits(val habits: List<Habit>, val doneIds: Set<Long>) {
    companion object {
        suspend fun load(context: Context): TodayHabits {
            val dao = HabitDatabase.get(context).habitDao()
            return TodayHabits(dao.habitsOnce(), dao.completedHabitIdsOn(LocalDate.now().toEpochDay()).toSet())
        }
    }
}

@Composable
private fun Content(habits: List<Habit>, doneIds: Set<Long>) {
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
        HabitsWidget.refreshAll(context)
    }

    companion object {
        val HabitId = ActionParameters.Key<Long>("habitId")
    }
}
