package io.github.meko123456.habitstreaks.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.meko123456.habitstreaks.data.Completion
import io.github.meko123456.habitstreaks.data.Habit
import io.github.meko123456.habitstreaks.data.HabitDao
import io.github.meko123456.habitstreaks.data.HabitDatabase
import io.github.meko123456.habitstreaks.data.github.GithubClient
import io.github.meko123456.habitstreaks.data.github.GithubContributions
import io.github.meko123456.habitstreaks.data.github.TokenStore
import io.github.meko123456.habitstreaks.domain.StreakEngine
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface GithubState {
    data object NotConnected : GithubState
    data object Loading : GithubState
    data class Error(val message: String) : GithubState
    data class Ready(val contributions: GithubContributions) : GithubState
}

data class HabitItem(
    val habit: Habit,
    val doneToday: Boolean,
    val currentStreak: Int,
    val longestStreak: Int,
)

class HabitsViewModel(
    private val dao: HabitDao,
    private val tokenStore: TokenStore? = null,
    private val githubClient: GithubClient? = null,
) : ViewModel() {

    private val _github = MutableStateFlow<GithubState>(GithubState.NotConnected)
    val github: StateFlow<GithubState> = _github.asStateFlow()

    init {
        refreshGithub()
    }

    fun connectGithub(token: String) {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return
        tokenStore?.save(trimmed)
        refreshGithub()
    }

    fun disconnectGithub() {
        tokenStore?.clear()
        _github.value = GithubState.NotConnected
    }

    fun refreshGithub() {
        val store = tokenStore ?: return
        val client = githubClient ?: return
        val token = store.load() ?: run {
            _github.value = GithubState.NotConnected
            return
        }
        _github.value = GithubState.Loading
        viewModelScope.launch {
            _github.value = client.fetchContributions(token).fold(
                onSuccess = { GithubState.Ready(it) },
                onFailure = { GithubState.Error(it.message ?: "Unknown error") },
            )
        }
    }

    val items: StateFlow<List<HabitItem>> =
        combine(dao.observeHabits(), dao.observeAllCompletions()) { habits, completions ->
            val today = LocalDate.now().toEpochDay()
            val byHabit = completions.groupBy({ it.habitId }, { it.epochDay })
            habits.map { habit ->
                val days = byHabit[habit.id].orEmpty().toSet()
                HabitItem(
                    habit = habit,
                    doneToday = today in days,
                    currentStreak = StreakEngine.currentStreak(days, today),
                    longestStreak = StreakEngine.longestStreak(days),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Completions per day across all habits, for the activity heatmap. */
    val dayCounts: StateFlow<Map<Long, Int>> =
        dao.observeAllCompletions()
            .map { completions -> completions.groupingBy { it.epochDay }.eachCount() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun createHabit(name: String, emoji: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            dao.insert(
                Habit(
                    name = trimmed,
                    emoji = emoji.ifBlank { "🔥" },
                    createdAtEpochDay = LocalDate.now().toEpochDay(),
                ),
            )
        }
    }

    fun renameHabit(habit: Habit, name: String, emoji: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            dao.update(habit.copy(name = trimmed, emoji = emoji.ifBlank { habit.emoji }))
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch { dao.delete(habit) }
    }

    fun toggleToday(item: HabitItem) {
        val today = LocalDate.now().toEpochDay()
        viewModelScope.launch {
            if (item.doneToday) {
                dao.removeCompletion(item.habit.id, today)
            } else {
                dao.addCompletion(Completion(item.habit.id, today))
            }
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[APPLICATION_KEY]) as Application
                @Suppress("UNCHECKED_CAST")
                return HabitsViewModel(
                    dao = HabitDatabase.get(app).habitDao(),
                    tokenStore = TokenStore(app),
                    githubClient = GithubClient(),
                ) as T
            }
        }
    }
}
