/*
 *  Copyright 2026 CNM Ingenuity, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.cnm.deepdive.paula.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.cnm.deepdive.paula.R
import edu.cnm.deepdive.paula.model.dao.VoicePromptDao
import edu.cnm.deepdive.paula.model.entity.VoicePrompt
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Service repository providing access to companion audio prompts, video clips, and transcripts.
 */
@Singleton
class VoicePromptRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voicePromptDao: VoicePromptDao
) {

    fun getAll(): Flow<List<VoicePrompt>> =
        voicePromptDao.selectAll()

    fun get(promptId: Long): Flow<VoicePrompt?> =
        voicePromptDao.select(promptId)

    fun getByTrigger(triggerEvent: String): Flow<VoicePrompt?> =
        voicePromptDao.selectByTrigger(triggerEvent)

    suspend fun findByTrigger(triggerEvent: String): VoicePrompt? =
        voicePromptDao.findByTrigger(triggerEvent)

    suspend fun save(voicePrompt: VoicePrompt): VoicePrompt {
        return if (voicePrompt.id == 0L) {
            val id = voicePromptDao.insert(voicePrompt)
            voicePrompt.copy(id = id)
        } else {
            voicePromptDao.update(voicePrompt)
            voicePrompt
        }
    }

    suspend fun markPlayed(voicePrompt: VoicePrompt): VoicePrompt {
        val updated = voicePrompt.copy(lastPlayedAt = Instant.now())
        voicePromptDao.update(updated)
        return updated
    }

    suspend fun ensureDefaultPrompts(): List<VoicePrompt> {
        val defaults = listOf(
            VoicePrompt(
                triggerEvent = "GREETING",
                title = "Morning Greeting",
                transcript = "Good morning. Ready when you are. Let's tackle today's routines together, one step at a time.",
                audioFileName = "paula_companion_greeting",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "AWESOME_JOB",
                title = "Awesome Job",
                transcript = "Awesome job checking that off.",
                audioFileName = "paula_awesome_job",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "PROUD_OF_YOU",
                title = "Proud of You",
                transcript = "Look at that. Everything is done. So proud of you.",
                audioFileName = "paula_proud_of_you",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "NICE_AND_NEAT",
                title = "Nice and Neat",
                transcript = "Let's keep our living space nice and neat.",
                audioFileName = "paula_nice_and_neat",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "CLEAR_SPACE",
                title = "Clear Space",
                transcript = "A clear space gives you a clear mind.",
                audioFileName = "paula_clear_space",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "ONE_STEP",
                title = "One Step at a Time",
                transcript = "One step at a time. You're doing great.",
                audioFileName = "paula_one_step_at_a_time",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "STREAK_ROLL",
                title = "On a Roll",
                transcript = "You're on a roll. Keep that streak going.",
                audioFileName = "paula_on_a_roll",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "READY_WHEN_YOU_ARE",
                title = "Ready When You Are",
                transcript = "Ready when you are. Let's tackle this together.",
                audioFileName = "paula_ready_when_you_are",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "GOODNIGHT",
                title = "Goodnight",
                transcript = "Time to relax and wind down. Good night!",
                audioFileName = "paula_goodnight",
                isAuthentic = true
            )
        )
        voicePromptDao.findByTrigger("MEMORIAL_TRIBUTE")?.let {
            voicePromptDao.delete(it)
        }
        voicePromptDao.findByTrigger("GOOD_MORNING")?.let {
            voicePromptDao.delete(it)
        }
        defaults.forEach { defaultPrompt ->
            val existing = voicePromptDao.findByTrigger(defaultPrompt.triggerEvent)
            if (existing != null) {
                if (existing.transcript != defaultPrompt.transcript || existing.title != defaultPrompt.title) {
                    voicePromptDao.update(existing.copy(transcript = defaultPrompt.transcript, title = defaultPrompt.title))
                }
            } else {
                voicePromptDao.insert(defaultPrompt)
            }
        }
        return defaults
    }

    fun getAudioResourceId(audioFileName: String): Int {
        val resId = context.resources.getIdentifier(audioFileName, "raw", context.packageName)
        return if (resId != 0) resId else R.raw.paula_companion_greeting
    }

    fun getVideoResourceId(triggerEvent: String): Int {
        val resName = when (triggerEvent.uppercase()) {
            "GREETING", "GOOD_MORNING" -> "paula_video_good_morning"
            "AWESOME_JOB" -> "paula_video_awesome_job"
            "PROUD_OF_YOU" -> "paula_video_proud_of_you"
            "NICE_AND_NEAT" -> "paula_video_nice_and_neat"
            "CLEAR_SPACE" -> "paula_video_clear_space"
            "ONE_STEP" -> "paula_video_one_step_at_a_time"
            "STREAK_ROLL" -> "paula_video_streak_on_a_roll"
            "READY_WHEN_YOU_ARE" -> "paula_video_ready_when_you_are"
            "GOODNIGHT" -> "paula_video_goodnight"
            else -> "paula_video_good_morning"
        }
        val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
        return if (resId != 0) resId else R.raw.paula_video_good_morning
    }

    fun getStillImageResourceId(triggerEvent: String): Int {
        val resName = when (triggerEvent.uppercase()) {
            "GREETING", "GOOD_MORNING" -> "paula_still_good_morning"
            "AWESOME_JOB" -> "paula_still_awesome_job"
            "PROUD_OF_YOU" -> "paula_still_proud_of_you"
            "NICE_AND_NEAT" -> "paula_still_nice_and_neat"
            "CLEAR_SPACE" -> "paula_still_clear_space"
            "ONE_STEP" -> "paula_still_one_step_at_a_time"
            "STREAK_ROLL" -> "paula_still_streak_on_a_roll"
            "READY_WHEN_YOU_ARE" -> "paula_still_ready_when_you_are"
            "GOODNIGHT" -> "paula_still_goodnight"
            else -> "paula_still_good_morning"
        }
        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
        return if (resId != 0) resId else R.drawable.paula_avatar_purple_hoodie
    }

}
