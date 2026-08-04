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
import io.github.meko123456.habitstreaks.domain.StreakEngine
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitItem(
    val habit: Habit,
    val doneToday: Boolean,
    val currentStreak: Int,
    val longestStreak: Int,
)

class HabitsViewModel(private val dao: HabitDao) : ViewModel() {

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
                return HabitsViewModel(HabitDatabase.get(app).habitDao()) as T
            }
        }
    }
}
