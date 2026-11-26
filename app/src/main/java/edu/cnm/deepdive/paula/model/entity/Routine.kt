package edu.cnm.deepdive.paula.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "routine",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["category_id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = VoicePrompt::class,
            parentColumns = ["prompt_id"],
            childColumns = ["voice_prompt_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["category_id"]),
        Index(value = ["voice_prompt_id"])
    ]
)
data class Routine(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "routine_id")
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long = 0,

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    @ColumnInfo(name = "voice_prompt_id")
    val voicePromptId: Long? = null,

    val name: String,

    val description: String? = null,

    val frequency: String = "DAILY",

    @ColumnInfo(name = "reminder_time")
    val reminderTime: String? = null,

    @ColumnInfo(name = "current_streak")
    val currentStreak: Int = 0,

    @ColumnInfo(name = "best_streak")
    val bestStreak: Int = 0,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "last_completed_date")
    val lastCompletedDate: LocalDate? = null,

    @ColumnInfo(name = "photo_uri")
    val photoUri: String? = null
)
