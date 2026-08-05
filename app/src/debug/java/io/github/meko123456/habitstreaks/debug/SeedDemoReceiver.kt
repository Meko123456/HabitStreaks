package io.github.meko123456.habitstreaks.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.meko123456.habitstreaks.data.Completion
import io.github.meko123456.habitstreaks.data.Habit
import io.github.meko123456.habitstreaks.data.HabitDatabase
import java.time.LocalDate
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Debug-only: populate the database with demo habits and plausible history. */
class SeedDemoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = HabitDatabase.get(context).habitDao()
                val today = LocalDate.now().toEpochDay()
                val random = Random(42)
                val habits = listOf(
                    Habit(name = "Code every day", emoji = "💻", createdAtEpochDay = today - 120),
                    Habit(name = "Morning run", emoji = "🏃", createdAtEpochDay = today - 90),
                    Habit(name = "Read 20 pages", emoji = "📚", createdAtEpochDay = today - 60),
                )
                habits.forEach { habit ->
                    val id = dao.insert(habit)
                    for (day in habit.createdAtEpochDay..today) {
                        // Denser recent history so streaks and heatmap look alive.
                        val p = if (day > today - 14) 0.9 else 0.6
                        if (random.nextDouble() < p) dao.addCompletion(Completion(id, day))
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
