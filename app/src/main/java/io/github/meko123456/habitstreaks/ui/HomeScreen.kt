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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.habitstreaks.data.Habit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HabitsViewModel = viewModel(factory = HabitsViewModel.Factory)) {
    val items by viewModel.items.collectAsState()
    val dayCounts by viewModel.dayCounts.collectAsState()
    val github by viewModel.github.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Habit?>(null) }

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
                        GithubCard(ready.contributions)
                    }
                }
                item(key = "heatmap") {
                    ActivityCard(dayCounts)
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

@Composable
private fun GithubCard(contributions: io.github.meko123456.habitstreaks.data.github.GithubContributions) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("GitHub · @${contributions.login}", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${contributions.total} contributions in the last year",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ContributionHeatmap(
                counts = contributions.countsByDay,
                endDay = java.time.LocalDate.now().toEpochDay(),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun ActivityCard(dayCounts: Map<Long, Int>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Activity", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Last 20 weeks, all habits",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ContributionHeatmap(
                counts = dayCounts,
                endDay = java.time.LocalDate.now().toEpochDay(),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
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
