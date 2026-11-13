package edu.cnm.deepdive.paula.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import edu.cnm.deepdive.paula.model.entity.VoicePrompt
import kotlinx.coroutines.flow.Flow

@Dao
interface VoicePromptDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(voicePrompt: VoicePrompt): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg voicePrompts: VoicePrompt): List<Long>

    @Update
    suspend fun update(voicePrompt: VoicePrompt): Int

    @Delete
    suspend fun delete(voicePrompt: VoicePrompt): Int

    @Query("SELECT * FROM voice_prompt ORDER BY trigger_event ASC")
    fun selectAll(): Flow<List<VoicePrompt>>

    @Query("SELECT * FROM voice_prompt WHERE prompt_id = :promptId")
    fun select(promptId: Long): Flow<VoicePrompt?>

    @Query("SELECT * FROM voice_prompt WHERE trigger_event = :triggerEvent")
    fun selectByTrigger(triggerEvent: String): Flow<VoicePrompt?>

    @Query("SELECT * FROM voice_prompt WHERE trigger_event = :triggerEvent")
    suspend fun findByTrigger(triggerEvent: String): VoicePrompt?

    @Query("SELECT * FROM voice_prompt WHERE is_authentic = 1 ORDER BY title ASC")
    fun selectAuthentic(): Flow<List<VoicePrompt>>

}
