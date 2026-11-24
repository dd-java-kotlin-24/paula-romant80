package edu.cnm.deepdive.paula.model

import edu.cnm.deepdive.paula.model.entity.VoicePrompt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit test suite verifying VoicePrompt catalog properties and authentic audio markers.
 */
class VoicePromptTest {

    @Test
    fun authenticVoicePrompt_markedCorrectly() {
        val prompt = VoicePrompt(
            id = 1L,
            triggerEvent = "MORNING_ROUTINE",
            title = "Good Morning Greeting",
            transcript = "Good morning! What should we get started on today?",
            audioFileName = "paula_companion_greeting",
            isAuthentic = true
        )

        assertEquals(1L, prompt.id)
        assertEquals("paula_companion_greeting", prompt.audioFileName)
        assertEquals("Good Morning Greeting", prompt.title)
        assertTrue(prompt.isAuthentic, "Authentic voice prompts recorded by Paula must have isAuthentic = true")
        assertEquals("MORNING_ROUTINE", prompt.triggerEvent)
    }

    @Test
    fun voicePrompt_allTasksCelebration_mapping() {
        val prompt = VoicePrompt(
            id = 2L,
            triggerEvent = "ALL_TASKS_COMPLETED",
            title = "Celebration",
            transcript = "Look at that! Everything is done. So proud of you!",
            audioFileName = "paula_voice_look_at_that",
            isAuthentic = true
        )

        assertEquals("ALL_TASKS_COMPLETED", prompt.triggerEvent)
        assertEquals("paula_voice_look_at_that", prompt.audioFileName)
        assertTrue(prompt.transcript.contains("So proud of you!"))
    }
}
