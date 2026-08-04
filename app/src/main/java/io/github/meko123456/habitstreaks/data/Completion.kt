package io.github.meko123456.habitstreaks.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** One row per habit per completed day. [epochDay] is LocalDate.toEpochDay(). */
@Entity(
    tableName = "completions",
    primaryKeys = ["habitId", "epochDay"],
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId")],
)
data class Completion(
    val habitId: Long,
    val epochDay: Long,
)
