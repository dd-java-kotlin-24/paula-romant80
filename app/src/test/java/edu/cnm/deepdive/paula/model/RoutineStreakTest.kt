package edu.cnm.deepdive.paula.model

import edu.cnm.deepdive.paula.model.entity.Routine
import edu.cnm.deepdive.paula.service.RoutineRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Unit test suite verifying daily, weekday, and weekend habit streak calculation logic.
 */
class RoutineStreakTest {

    @Test
    fun firstCompletion_setsStreakToOne() {
        val routine = Routine(name = "Make Bed", userId = 1L)
        val today = LocalDate.of(2026, 11, 24)

        val (newStreak, newBest) = RoutineRepository.calculateNextStreak(
            routine.frequency,
            routine.currentStreak,
            routine.bestStreak,
            routine.lastCompletedDate,
            today
        )

        assertEquals(1, newStreak)
        assertEquals(1, newBest)
    }

    @Test
    fun consecutiveDayCompletion_incrementsStreak() {
        val yesterday = LocalDate.of(2026, 11, 23)
        val today = LocalDate.of(2026, 11, 24)
        val routine = Routine(
            name = "Fold Blankets",
            userId = 1L,
            currentStreak = 4,
            bestStreak = 4,
            lastCompletedDate = yesterday
        )

        val (newStreak, newBest) = RoutineRepository.calculateNextStreak(
            routine.frequency,
            routine.currentStreak,
            routine.bestStreak,
            routine.lastCompletedDate,
            today
        )

        assertEquals(5, newStreak)
        assertEquals(5, newBest)
    }

    @Test
    fun sameDayCompletion_isIdempotent() {
        val today = LocalDate.of(2026, 11, 24)
        val routine = Routine(
            name = "Morning Walk",
            userId = 1L,
            currentStreak = 3,
            bestStreak = 3,
            lastCompletedDate = today
        )

        val (newStreak, newBest) = RoutineRepository.calculateNextStreak(
            routine.frequency,
            routine.currentStreak,
            routine.bestStreak,
            routine.lastCompletedDate,
            today
        )

        assertEquals(3, newStreak)
        assertEquals(3, newBest)
    }

    @Test
    fun missedDay_daily_resetsActiveStreak_preservesBestStreak() {
        val twoDaysAgo = LocalDate.of(2026, 11, 22)
        val today = LocalDate.of(2026, 11, 24)
        val routine = Routine(
            name = "Evening Tidying",
            userId = 1L,
            frequency = "DAILY",
            currentStreak = 10,
            bestStreak = 10,
            lastCompletedDate = twoDaysAgo
        )

        val (newStreak, newBest) = RoutineRepository.calculateNextStreak(
            routine.frequency,
            routine.currentStreak,
            routine.bestStreak,
            routine.lastCompletedDate,
            today
        )

        assertEquals(1, newStreak, "Daily streak must reset to 1 after missing a day")
        assertEquals(10, newBest, "Best streak record must be preserved")
    }

    @Test
    fun weekdaysFrequency_fridayToMonday_preservesAndIncrementsStreak() {
        val friday = LocalDate.of(2026, 11, 20) // Friday
        val monday = LocalDate.of(2026, 11, 23) // Monday
        val routine = Routine(
            name = "Desk Tidy",
            userId = 1L,
            frequency = "WEEKDAYS",
            currentStreak = 5,
            bestStreak = 5,
            lastCompletedDate = friday
        )

        val (newStreak, newBest) = RoutineRepository.calculateNextStreak(
            routine.frequency,
            routine.currentStreak,
            routine.bestStreak,
            routine.lastCompletedDate,
            monday
        )

        assertEquals(6, newStreak, "Weekday habit must bridge Saturday and Sunday")
        assertEquals(6, newBest)
    }

    @Test
    fun weekendsFrequency_sundayToSaturday_preservesAndIncrementsStreak() {
        val sunday = LocalDate.of(2026, 11, 15) // Sunday
        val saturday = LocalDate.of(2026, 11, 21) // Next Saturday
        val routine = Routine(
            name = "Deep Clean Laundry",
            userId = 1L,
            frequency = "WEEKENDS",
            currentStreak = 2,
            bestStreak = 2,
            lastCompletedDate = sunday
        )

        val (newStreak, newBest) = RoutineRepository.calculateNextStreak(
            routine.frequency,
            routine.currentStreak,
            routine.bestStreak,
            routine.lastCompletedDate,
            saturday
        )

        assertEquals(3, newStreak, "Weekend habit must bridge Mon-Fri")
        assertEquals(3, newBest)
    }

    @Test
    fun higherNewStreak_surpassesPreviousBest() {
        val yesterday = LocalDate.of(2026, 11, 23)
        val today = LocalDate.of(2026, 11, 24)
        val routine = Routine(
            name = "Kitchen Clean",
            userId = 1L,
            currentStreak = 5,
            bestStreak = 5,
            lastCompletedDate = yesterday
        )

        val (newStreak, newBest) = RoutineRepository.calculateNextStreak(
            routine.frequency,
            routine.currentStreak,
            routine.bestStreak,
            routine.lastCompletedDate,
            today
        )

        assertEquals(6, newStreak)
        assertEquals(6, newBest)
        assertTrue(newBest > 5)
    }

    @Test
    fun customFrequency_consecutiveScheduledDays_incrementsStreak() {
        val monday = LocalDate.of(2026, 11, 23) // Monday
        val wednesday = LocalDate.of(2026, 11, 25) // Wednesday
        val routine = Routine(
            name = "Water Plants",
            userId = 1L,
            frequency = "Mon, Wed, Fri",
            currentStreak = 2,
            bestStreak = 2,
            lastCompletedDate = monday
        )

        val (newStreak, newBest) = RoutineRepository.calculateNextStreak(
            routine.frequency,
            routine.currentStreak,
            routine.bestStreak,
            routine.lastCompletedDate,
            wednesday
        )

        assertEquals(3, newStreak, "Custom habit (Mon, Wed, Fri) must bridge Tuesday")
        assertEquals(3, newBest)
    }

    @Test
    fun customFrequency_missedScheduledDay_resetsStreak() {
        val lastFriday = LocalDate.of(2026, 11, 20) // Friday
        val wednesday = LocalDate.of(2026, 11, 25) // Wednesday (missed Monday Nov 23)
        val routine = Routine(
            name = "Water Plants",
            userId = 1L,
            frequency = "Mon, Wed, Fri",
            currentStreak = 5,
            bestStreak = 5,
            lastCompletedDate = lastFriday
        )

        val (newStreak, newBest) = RoutineRepository.calculateNextStreak(
            routine.frequency,
            routine.currentStreak,
            routine.bestStreak,
            routine.lastCompletedDate,
            wednesday
        )

        assertEquals(1, newStreak, "Custom habit must reset streak when a scheduled day was skipped")
        assertEquals(5, newBest)
    }
}
