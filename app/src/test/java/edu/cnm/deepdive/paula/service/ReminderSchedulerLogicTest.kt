package edu.cnm.deepdive.paula.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Unit test suite verifying reminder time parsing, rollover calculation, and Quiet Hours logic.
 */
class ReminderSchedulerLogicTest {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private fun isQuietHours(hour: Int): Boolean {
        // Quiet hours: 10:00 PM (22:00) to 7:00 AM (07:00)
        return hour >= 22 || hour < 7
    }

    private fun calculateNextTargetDateTime(now: LocalDateTime, reminderTime: String): LocalDateTime {
        val localTime = LocalTime.parse(reminderTime, timeFormatter)
        var target = now
            .withHour(localTime.hour)
            .withMinute(localTime.minute)
            .withSecond(0)
            .withNano(0)

        if (target.isBefore(now)) {
            target = target.plusDays(1)
        }
        return target
    }

    @ParameterizedTest
    @ValueSource(ints = [22, 23, 0, 1, 2, 3, 4, 5, 6])
    fun quietHours_nighttimeHours_areMuted(hour: Int) {
        assertTrue(isQuietHours(hour), "Hour $hour should be recognized as quiet hours")
    }

    @ParameterizedTest
    @ValueSource(ints = [7, 8, 9, 12, 15, 18, 20, 21])
    fun activeHours_daytimeHours_areAllowed(hour: Int) {
        assertFalse(isQuietHours(hour), "Hour $hour should not be muted as quiet hours")
    }

    @Test
    fun timeInFuture_schedulesToday() {
        val now = LocalDateTime.of(2026, 11, 24, 7, 0)
        val target = calculateNextTargetDateTime(now, "08:30")

        assertEquals(24, target.dayOfMonth)
        assertEquals(8, target.hour)
        assertEquals(30, target.minute)
    }

    @Test
    fun timeInPast_rollsOverToTomorrow() {
        val now = LocalDateTime.of(2026, 11, 24, 14, 0)
        val target = calculateNextTargetDateTime(now, "08:30")

        assertEquals(25, target.dayOfMonth, "Should roll over to tomorrow")
        assertEquals(8, target.hour)
        assertEquals(30, target.minute)
    }
}
