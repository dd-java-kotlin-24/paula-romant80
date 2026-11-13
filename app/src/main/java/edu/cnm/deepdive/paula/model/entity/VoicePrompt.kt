package edu.cnm.deepdive.paula.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "voice_prompt",
    indices = [
        Index(value = ["trigger_event"], unique = true)
    ]
)
data class VoicePrompt(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "prompt_id")
    val id: Long = 0,

    @ColumnInfo(name = "trigger_event")
    val triggerEvent: String,

    val title: String,

    val transcript: String,

    @ColumnInfo(name = "audio_file_name")
    val audioFileName: String,

    @ColumnInfo(name = "is_authentic")
    val isAuthentic: Boolean = false,

    @ColumnInfo(name = "last_played_at")
    val lastPlayedAt: Instant? = null
)