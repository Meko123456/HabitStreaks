package io.github.meko123456.habitstreaks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.habitstreaks.data.Habit
import io.github.meko123456.heatmap.ContributionHeatmap
import io.github.meko123456.heatmap.HeatmapLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HabitsViewModel = viewModel(factory = HabitsViewModel.Factory)) {
    val items by viewModel.items.collectAsState()
    val dayCounts by viewModel.dayCounts.collectAsState()
    val github by viewModel.github.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Habit?>(null) }
    val today = remember { java.time.LocalDate.now().toEpochDay() }
    var habitDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var githubDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var pickingFor by rememberSaveable { mutableStateOf<String?>(null) } // "habits" | "github"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HabitStreaks") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "GitHub settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add habit")
            }
        },
    ) { innerPadding ->
        if (items.isEmpty()) {
            EmptyState(Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                (github as? GithubState.Ready)?.let { ready ->
                    item(key = "github") {
                        GithubCard(
                            contributions = ready.contributions,
                            today = today,
                            onDayClick = { githubDay = it },
                            onPickDay = { pickingFor = "github" },
                        )
                    }
                }
                item(key = "heatmap") {
                    ActivityCard(
                        dayCounts = dayCounts,
                        today = today,
                        onDayClick = { habitDay = it },
                        onPickDay = { pickingFor = "habits" },
                    )
                }
                items(items, key = { it.habit.id }) { item ->
                    HabitCard(
                        item = item,
                        onToggleToday = { viewModel.toggleToday(item) },
                        onEdit = { editing = item.habit },
                    )
                }
            }
        }
    }

    habitDay?.let { day ->
        HabitDaySheet(epochDay = day, viewModel = viewModel, onDismiss = { habitDay = null })
    }
    githubDay?.let { day ->
        (github as? GithubState.Ready)?.contributions?.let { c ->
            GithubDaySheet(
                epochDay = day,
                login = c.login,
                count = c.countsByDay[day] ?: 0,
                onDismiss = { githubDay = null },
            )
        }
    }
    pickingFor?.let { target ->
        PickDayDialog(
            today = today,
            onPick = { day ->
                if (target == "github") githubDay = day else habitDay = day
                pickingFor = null
            },
            onDismiss = { pickingFor = null },
        )
    }

    if (showSettings) {
        GithubSettingsDialog(
            state = github,
            onConnect = { viewModel.connectGithub(it) },
            onDisconnect = { viewModel.disconnectGithub() },
            onDismiss = { showSettings = false },
        )
    }

    if (showCreate) {
        HabitEditorDialog(
            title = "New habit",
            onDismiss = { showCreate = false },
            onConfirm = { name, emoji ->
                viewModel.createHabit(name, emoji)
                showCreate = false
            },
        )
    }

    editing?.let { habit ->
        HabitEditorDialog(
            title = "Edit habit",
            initialName = habit.name,
            initialEmoji = habit.emoji,
            onDismiss = { editing = null },
            onConfirm = { name, emoji ->
                viewModel.renameHabit(habit, name, emoji)
                editing = null
            },
            onDelete = {
                viewModel.deleteHabit(habit)
                editing = null
            },
        )
    }
}

private const val HEATMAP_WEEKS = 20

@Composable
private fun GithubCard(
    contributions: io.github.meko123456.habitstreaks.data.github.GithubContributions,
    today: Long,
    onDayClick: (Long) -> Unit,
    onPickDay: () -> Unit,
) {
    val activeDays = remember(contributions, today) {
        HeatmapLayout.daysWithin(contributions.countsByDay.keys, today, HEATMAP_WEEKS)
    }
    val shownDays = remember(today) { HeatmapLayout.daysShown(today, HEATMAP_WEEKS) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            CardHeader(
                title = "GitHub · @${contributions.login}",
                subtitle = "${contributions.total} contributions in the last year",
                onPickDay = onPickDay,
            )
            ContributionHeatmap(
                counts = contributions.countsByDay,
                endDay = today,
                weeks = HEATMAP_WEEKS,
                modifier = Modifier.padding(top = 12.dp),
                onDayClick = onDayClick,
                contentDescription = "GitHub contributions: active on $activeDays of the last $shownDays days. " +
                    "Use Pick a day to open a date.",
            )
            Text(
                text = "Tap a day for details",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ActivityCard(
    dayCounts: Map<Long, Int>,
    today: Long,
    onDayClick: (Long) -> Unit,
    onPickDay: () -> Unit,
) {
    val activeDays = remember(dayCounts, today) { HeatmapLayout.daysWithin(dayCounts.keys, today, HEATMAP_WEEKS) }
    val shownDays = remember(today) { HeatmapLayout.daysShown(today, HEATMAP_WEEKS) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            CardHeader(
                title = "Activity",
                subtitle = "Last $HEATMAP_WEEKS weeks, all habits",
                onPickDay = onPickDay,
            )
            ContributionHeatmap(
                counts = dayCounts,
                endDay = today,
                weeks = HEATMAP_WEEKS,
                modifier = Modifier.padding(top = 12.dp),
                onDayClick = onDayClick,
                contentDescription = "Habit activity: $activeDays of the last $shownDays days had a check-off. " +
                    "Use Pick a day to open a date.",
            )
            Text(
                text = "Tap a day to see or fix its check-offs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun CardHeader(title: String, subtitle: String, onPickDay: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onPickDay) { Text("Pick a day") }
    }
}

@Composable
private fun HabitCard(
    item: HabitItem,
    onToggleToday: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(item.habit.emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.habit.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "🔥 ${item.currentStreak} day streak · best ${item.longestStreak}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${item.habit.name}")
            }
            Spacer(Modifier.width(4.dp))
            TodayCheck(done = item.doneToday, onClick = onToggleToday)
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No habits yet", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Tap + to create your first habit and start a streak.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
