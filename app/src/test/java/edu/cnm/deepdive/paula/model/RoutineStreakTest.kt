package edu.cnm.deepdive.paula.model

import edu.cnm.deepdive.paula.model.entity.Routine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Unit test suite verifying daily habit streak calculation and progression logic.
 */
class RoutineStreakTest {

    private fun calculateNextStreak(
        currentStreak: Int,
        bestStreak: Int,
        lastCompletedDate: LocalDate?,
        completedDate: LocalDate
    ): Pair<Int, Int> {
        val yesterday = completedDate.minusDays(1)
        val newStreak = when (lastCompletedDate) {
            yesterday -> currentStreak + 1
            completedDate -> currentStreak
            else -> 1
        }
        val newBest = maxOf(newStreak, bestStreak)
        return Pair(newStreak, newBest)
    }

    @Test
    fun firstCompletion_setsStreakToOne() {
        val routine = Routine(name = "Make Bed", userId = 1L)
        val today = LocalDate.of(2026, 11, 24)

        val (newStreak, newBest) = calculateNextStreak(
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

        val (newStreak, newBest) = calculateNextStreak(
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

        val (newStreak, newBest) = calculateNextStreak(
            routine.currentStreak,
            routine.bestStreak,
            routine.lastCompletedDate,
            today
        )

        assertEquals(3, newStreak)
        assertEquals(3, newBest)
    }

    @Test
    fun missedDay_resetsActiveStreak_preservesBestStreak() {
        val twoDaysAgo = LocalDate.of(2026, 11, 22)
        val today = LocalDate.of(2026, 11, 24)
        val routine = Routine(
            name = "Evening Tidying",
            userId = 1L,
            currentStreak = 10,
            bestStreak = 10,
            lastCompletedDate = twoDaysAgo
        )

        val (newStreak, newBest) = calculateNextStreak(
            routine.currentStreak,
            routine.bestStreak,
            routine.lastCompletedDate,
            today
        )

        assertEquals(1, newStreak, "Streak must reset to 1 after missing a day")
        assertEquals(10, newBest, "Best streak record must be preserved")
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

        val (newStreak, newBest) = calculateNextStreak(
            routine.currentStreak,
            routine.bestStreak,
            routine.lastCompletedDate,
            today
        )

        assertEquals(6, newStreak)
        assertEquals(6, newBest)
        assertTrue(newBest > 5)
    }
}
