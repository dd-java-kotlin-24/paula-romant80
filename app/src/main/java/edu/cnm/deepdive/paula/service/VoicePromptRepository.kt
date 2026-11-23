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
                transcript = "Good morning! What should we get started on today?",
                audioFileName = "paula_companion_greeting",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "GOOD_MORNING",
                title = "Good Morning",
                transcript = "Good morning! Let's make today productive and organized.",
                audioFileName = "paula_good_morning",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "AWESOME_JOB",
                title = "Awesome Job",
                transcript = "Awesome job! You are making great progress.",
                audioFileName = "paula_awesome_job",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "PROUD_OF_YOU",
                title = "Proud of You",
                transcript = "I am so proud of you. Keep up the good work!",
                audioFileName = "paula_proud_of_you",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "NICE_AND_NEAT",
                title = "Nice and Neat",
                transcript = "Nice and neat, everything in its place.",
                audioFileName = "paula_nice_and_neat",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "CLEAR_SPACE",
                title = "Clear Space",
                transcript = "Clear space, clear mind. Let's do this together.",
                audioFileName = "paula_clear_space",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "ONE_STEP",
                title = "One Step at a Time",
                transcript = "One step at a time. You've got this!",
                audioFileName = "paula_one_step_at_a_time",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "STREAK_ROLL",
                title = "On a Roll",
                transcript = "Look at you on a roll! Keep that habit streak going!",
                audioFileName = "paula_on_a_roll",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "READY_WHEN_YOU_ARE",
                title = "Ready When You Are",
                transcript = "Ready when you are!",
                audioFileName = "paula_ready_when_you_are",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "GOODNIGHT",
                title = "Goodnight",
                transcript = "Rest up, you did wonderful today. Goodnight!",
                audioFileName = "paula_goodnight",
                isAuthentic = true
            ),
            VoicePrompt(
                triggerEvent = "MEMORIAL_TRIBUTE",
                title = "Memorial Tribute",
                transcript = "In loving memory of Paula Elizabeth Kleczka.",
                audioFileName = "paula_memorial_tribute",
                isAuthentic = true
            )
        )
        voicePromptDao.insert(*defaults.toTypedArray())
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

}
