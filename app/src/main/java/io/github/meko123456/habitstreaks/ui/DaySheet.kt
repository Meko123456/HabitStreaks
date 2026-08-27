package io.github.meko123456.habitstreaks.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.meko123456.habitstreaks.data.Habit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.core.net.toUri

private const val MILLIS_PER_DAY = 86_400_000L

/** "Thu, 27 Aug 2026" in the device locale. */
internal fun formatDay(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()))

/** Relative label for the sheet titles: Today / Yesterday / N days ago. */
internal fun relativeDay(epochDay: Long, today: Long): String = when (today - epochDay) {
    0L -> "Today"
    1L -> "Yesterday"
    else -> "${today - epochDay} days ago"
}

/**
 * Every habit with a checkbox for whether it was done on [epochDay]. Toggling writes the
 * completion for that day, so a forgotten check-off can be fixed after the fact.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDaySheet(
    epochDay: Long,
    viewModel: HabitsViewModel,
    onDismiss: () -> Unit,
) {
    val today = remember { LocalDate.now().toEpochDay() }
    val dayFlow = remember(epochDay) { viewModel.observeDay(epochDay) }
    val habits by dayFlow.collectAsState(initial = emptyList())
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(formatDay(epochDay), style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${relativeDay(epochDay, today)} · ${habits.count { it.done }} of ${habits.size} done",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            habits.forEach { entry ->
                DayHabitRow(
                    habit = entry.habit,
                    done = entry.done,
                    onToggle = { viewModel.toggleOn(entry.habit, epochDay, entry.done) },
                )
            }
            if (habits.isEmpty()) {
                Text("No habits yet.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DayHabitRow(habit: Habit, done: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${habit.name}: ${if (done) "done" else "not done"} this day"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(habit.emoji, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(12.dp))
        Text(habit.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Checkbox(checked = done, onCheckedChange = { onToggle() })
    }
}

/** The GitHub contribution count for one day, with a link to that day on github.com. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GithubDaySheet(
    epochDay: Long,
    login: String,
    count: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now().toEpochDay() }
    val date = LocalDate.ofEpochDay(epochDay).toString()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(formatDay(epochDay), style = MaterialTheme.typography.titleLarge)
            Text(
                text = relativeDay(epochDay, today),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = when (count) {
                    0 -> "No contributions"
                    1 -> "1 contribution"
                    else -> "$count contributions"
                },
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = {
                    val url = "https://github.com/$login?tab=overview&from=$date&to=$date"
                    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                }) {
                    Text("Open on GitHub")
                }
            }
        }
    }
}

/** Material date picker limited to days up to [today]; confirming returns the epoch day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickDayDialog(
    today: Long,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectable = remember(today) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                Math.floorDiv(utcTimeMillis, MILLIS_PER_DAY) <= today
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = today * MILLIS_PER_DAY,
        selectableDates = selectable,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            val selected = state.selectedDateMillis
            TextButton(
                enabled = selected != null,
                onClick = { selected?.let { onPick(Math.floorDiv(it, MILLIS_PER_DAY)) } },
            ) { Text("Open") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}
