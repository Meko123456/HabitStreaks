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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.heatmap.ContributionHeatmap
import io.github.meko123456.habitstreaks.reminders.ReminderNotice
import io.github.meko123456.habitstreaks.reminders.ReminderWorker
import io.github.meko123456.heatmap.HeatmapLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HabitsViewModel = viewModel(factory = HabitsViewModel.Factory),
    openCreate: Boolean = false,
    onCreateOpened: () -> Unit = {},
) {
    val items by viewModel.items.collectAsState()
    val dayCounts by viewModel.dayCounts.collectAsState()
    val github by viewModel.github.collectAsState()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(openCreate) {
        if (openCreate) {
            showCreate = true
            onCreateOpened()
        }
    }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    // The id, not the Habit: a saved-state Bundle can hold a Long, and the habit comes from the list.
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editing = items.firstOrNull { it.habit.id == editingId }?.habit
    // From the ViewModel, not remember { now() }: frozen at first composition, this made an app left
    // open overnight keep drawing yesterday's heatmap and yesterday's check marks.
    val today by viewModel.today.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var remindersOff by remember { mutableStateOf(ReminderNotice.shouldShow(context)) }
    LifecycleResumeEffect(viewModel) {
        viewModel.recheckDay()
        // On every return, not once: the way to turn them back on is in Settings.
        val off = ReminderNotice.shouldShow(context)
        // The card goes in above the first item, and a list keeps the item it had at the top in
        // place, so the card came in scrolled out of sight. A list at the top stays at the top.
        if (off && !remindersOff && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
            listState.requestScrollToItem(0)
        }
        remindersOff = off
        onPauseOrDispose { }
    }
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
        val githubReady = github as? GithubState.Ready
        if (items.isEmpty() && githubReady == null) {
            EmptyState(Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                state = listState,
                // 88dp at the bottom: the FAB's 56 and its margins. With 16, the add button sat on the
                // last habit's check, so ticking that habit opened "New habit" instead.
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Only with a habit to be reminded of.
                if (remindersOff && items.isNotEmpty()) {
                    item(key = "reminders-off") {
                        RemindersOffCard(
                            onTurnOn = { context.startActivity(ReminderWorker.settingsIntent(context)) },
                            onNotNow = {
                                ReminderNotice.dismiss(context)
                                remindersOff = false
                            },
                        )
                    }
                }
                githubReady?.let { ready ->
                    item(key = "github") {
                        GithubCard(
                            contributions = ready.contributions,
                            today = today,
                            onDayClick = { githubDay = it },
                            onPickDay = { pickingFor = "github" },
                        )
                    }
                }
                // GitHub connected before the first habit: its card shows above the empty state. The
                // empty state used to replace the whole list, so connecting seemed to do nothing.
                if (items.isEmpty()) {
                    item(key = "empty") { EmptyState() }
                } else {
                    item(key = "heatmap") {
                        ActivityCard(
                            dayCounts = dayCounts,
                            today = today,
                            onDayClick = { habitDay = it },
                            onPickDay = { pickingFor = "habits" },
                        )
                    }
                }
                items(items, key = { it.habit.id }) { item ->
                    HabitCard(
                        item = item,
                        onToggleToday = { viewModel.toggleToday(item) },
                        onEdit = { editingId = item.habit.id },
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
                count = c.countOn(day),
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
            token = viewModel.tokenDraft,
            onTokenChange = { viewModel.tokenDraft = it },
            onConnect = { viewModel.connectGithub(it) },
            onDisconnect = { viewModel.disconnectGithub() },
            onDismiss = {
                viewModel.tokenDraft = ""
                showSettings = false
            },
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
            onDismiss = { editingId = null },
            onConfirm = { name, emoji ->
                viewModel.renameHabit(habit, name, emoji)
                editingId = null
            },
            onDelete = {
                viewModel.deleteHabit(habit)
                editingId = null
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
                subtitle = "${contributionCount(contributions.total, LocalConfiguration.current.locales[0])} in the last year",
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
private fun RemindersOffCard(onTurnOn: () -> Unit, onNotNow: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 8.dp)) {
            Text("Evening reminders are off", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Notifications for daily reminders are turned off, so there's no nudge on " +
                    "evenings when a habit is still open.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, end = 8.dp),
            )
            Row(Modifier.align(Alignment.End)) {
                TextButton(onClick = onNotNow) { Text("Not now") }
                TextButton(onClick = onTurnOn) { Text("Turn on") }
            }
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
