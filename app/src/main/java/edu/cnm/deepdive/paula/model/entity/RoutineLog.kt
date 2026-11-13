package edu.cnm.deepdive.paula.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "routine_log",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["routine_id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routine_id", "completed_date"], unique = true)
    ]
)
data class RoutineLog(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "log_id")
    val id: Long = 0,

    @ColumnInfo(name = "routine_id")
    val routineId: Long,

    @ColumnInfo(name = "completed_date")
    val completedDate: LocalDate = LocalDate.now(),

    @ColumnInfo(name = "completed_at")
    val completedAt: Instant = Instant.now(),

    val notes: String? = null,

    @ColumnInfo(name = "photo_uri")
    val photoUri: String? = null
)
