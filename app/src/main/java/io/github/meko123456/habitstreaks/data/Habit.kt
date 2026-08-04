package io.github.meko123456.habitstreaks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "🔥",
    val createdAtEpochDay: Long,
    val archived: Boolean = false,
)
