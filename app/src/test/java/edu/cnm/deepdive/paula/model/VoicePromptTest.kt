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

    @Test
    fun timeOfDay_morningSelectsMorningGreeting() {
        val greeting = VoicePrompt(triggerEvent = "GREETING", title = "Morning Greeting", transcript = "Good morning...", audioFileName = "greeting")
        val ready = VoicePrompt(triggerEvent = "READY_WHEN_YOU_ARE", title = "Ready", transcript = "Ready...", audioFileName = "ready")
        val goodnight = VoicePrompt(triggerEvent = "GOODNIGHT", title = "Goodnight", transcript = "Goodnight...", audioFileName = "goodnight")
        val list = listOf(greeting, ready, goodnight)

        val morningTime = java.time.LocalTime.of(8, 30)
        val isMorning = !morningTime.isBefore(java.time.LocalTime.of(5, 0)) && morningTime.isBefore(java.time.LocalTime.of(12, 0))
        val target = if (isMorning) "GREETING" else "READY_WHEN_YOU_ARE"
        val selected = list.find { it.triggerEvent == target }

        assertEquals("GREETING", selected?.triggerEvent)
    }

    @Test
    fun timeOfDay_afternoonSelectsReadyWhenYouAre() {
        val greeting = VoicePrompt(triggerEvent = "GREETING", title = "Morning Greeting", transcript = "Good morning...", audioFileName = "greeting")
        val ready = VoicePrompt(triggerEvent = "READY_WHEN_YOU_ARE", title = "Ready", transcript = "Ready...", audioFileName = "ready")
        val goodnight = VoicePrompt(triggerEvent = "GOODNIGHT", title = "Goodnight", transcript = "Goodnight...", audioFileName = "goodnight")
        val list = listOf(greeting, ready, goodnight)

        val afternoonTime = java.time.LocalTime.of(14, 0)
        val isMorning = !afternoonTime.isBefore(java.time.LocalTime.of(5, 0)) && afternoonTime.isBefore(java.time.LocalTime.of(12, 0))
        val isNight = !afternoonTime.isBefore(java.time.LocalTime.of(20, 0)) || afternoonTime.isBefore(java.time.LocalTime.of(5, 0))
        val target = when {
            isMorning -> "GREETING"
            isNight -> "GOODNIGHT"
            else -> "READY_WHEN_YOU_ARE"
        }
        val selected = list.find { it.triggerEvent == target }

        assertEquals("READY_WHEN_YOU_ARE", selected?.triggerEvent)
    }

    @Test
    fun timeOfDay_nighttimeSelectsGoodnight() {
        val greeting = VoicePrompt(triggerEvent = "GREETING", title = "Morning Greeting", transcript = "Good morning...", audioFileName = "greeting")
        val ready = VoicePrompt(triggerEvent = "READY_WHEN_YOU_ARE", title = "Ready", transcript = "Ready...", audioFileName = "ready")
        val goodnight = VoicePrompt(triggerEvent = "GOODNIGHT", title = "Goodnight", transcript = "Goodnight...", audioFileName = "goodnight")
        val list = listOf(greeting, ready, goodnight)

        val nightTime = java.time.LocalTime.of(20, 15)
        val isNight = !nightTime.isBefore(java.time.LocalTime.of(20, 0)) || nightTime.isBefore(java.time.LocalTime.of(5, 0))
        val target = if (isNight) "GOODNIGHT" else "GREETING"
        val selected = list.find { it.triggerEvent == target }

        assertEquals("GOODNIGHT", selected?.triggerEvent)
    }
}
