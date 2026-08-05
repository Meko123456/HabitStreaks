package io.github.meko123456.habitstreaks.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY createdAtEpochDay, id")
    fun observeHabits(): Flow<List<Habit>>

    @Insert
    suspend fun insert(habit: Habit): Long

    @Update
    suspend fun update(habit: Habit)

    @Delete
    suspend fun delete(habit: Habit)

    @Query("SELECT * FROM completions")
    fun observeAllCompletions(): Flow<List<Completion>>

    // One-shot variants for non-reactive callers (e.g. the Glance widget).

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY createdAtEpochDay, id")
    suspend fun habitsOnce(): List<Habit>

    @Query("SELECT habitId FROM completions WHERE epochDay = :epochDay")
    suspend fun completedHabitIdsOn(epochDay: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCompletion(completion: Completion)

    @Query("DELETE FROM completions WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun removeCompletion(habitId: Long, epochDay: Long)
}
