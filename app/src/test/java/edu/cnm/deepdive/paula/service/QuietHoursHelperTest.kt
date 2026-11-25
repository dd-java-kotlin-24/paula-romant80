package edu.cnm.deepdive.paula.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime

/**
 * Unit test suite verifying custom Quiet Hours window evaluation.
 */
class QuietHoursHelperTest {

    @Test
    fun overnightWindow_matchesNightAndEarlyMorning() {
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(7, 0)

        assertTrue(QuietHoursHelper.isWithinQuietHours(LocalTime.of(22, 0), start, end))
        assertTrue(QuietHoursHelper.isWithinQuietHours(LocalTime.of(23, 30), start, end))
        assertTrue(QuietHoursHelper.isWithinQuietHours(LocalTime.of(3, 15), start, end))
        assertFalse(QuietHoursHelper.isWithinQuietHours(LocalTime.of(7, 0), start, end))
        assertFalse(QuietHoursHelper.isWithinQuietHours(LocalTime.of(14, 0), start, end))
    }

    @Test
    fun daytimeWindow_supportsNightShiftSleepSchedule() {
        val start = LocalTime.of(9, 0)
        val end = LocalTime.of(17, 0)

        assertTrue(QuietHoursHelper.isWithinQuietHours(LocalTime.of(9, 0), start, end))
        assertTrue(QuietHoursHelper.isWithinQuietHours(LocalTime.of(12, 30), start, end))
        assertFalse(QuietHoursHelper.isWithinQuietHours(LocalTime.of(17, 0), start, end))
        assertFalse(QuietHoursHelper.isWithinQuietHours(LocalTime.of(22, 0), start, end))
    }

    @Test
    fun equalStartAndEnd_returnsFalse() {
        val time = LocalTime.of(8, 0)
        assertFalse(QuietHoursHelper.isWithinQuietHours(time, time, time))
    }
}
