package edu.cnm.deepdive.paula.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "task",
    foreignKeys = [
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
        Index(value = ["category_id"]),
        Index(value = ["voice_prompt_id"]),
        Index(value = ["is_completed", "due_date"])
    ]
)
data class Task(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "task_id")
    val id: Long = 0,

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    @ColumnInfo(name = "voice_prompt_id")
    val voicePromptId: Long? = null,

    val title: String,

    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "due_date")
    val dueDate: Instant? = null,

    val priority: Int = 1,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "completed_at")
    val completedAt: Instant? = null,

    @ColumnInfo(name = "photo_uri")
    val photoUri: String? = null,

    @ColumnInfo(name = "tutorial_url")
    val tutorialUrl: String? = null
)
