package edu.cnm.deepdive.paula.service

import android.content.Context
import androidx.preference.PreferenceManager
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shared helper for evaluating and formatting user-configurable Quiet Hours windows.
 */
object QuietHoursHelper {

    const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
    const val KEY_QUIET_HOURS_START = "quiet_hours_start"
    const val KEY_QUIET_HOURS_END = "quiet_hours_end"
    const val DEFAULT_START = "22:00"
    const val DEFAULT_END = "07:00"

    private val storageFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val displayFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    fun isQuietHoursActive(context: Context, now: LocalTime = LocalTime.now()): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (!prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, true)) {
            return false
        }
        val start = parseTime(prefs.getString(KEY_QUIET_HOURS_START, DEFAULT_START), LocalTime.of(22, 0))
        val end = parseTime(prefs.getString(KEY_QUIET_HOURS_END, DEFAULT_END), LocalTime.of(7, 0))
        return isWithinQuietHours(now, start, end)
    }

    fun isWithinQuietHours(now: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        if (start == end) {
            return false
        }
        return if (start.isBefore(end)) {
            !now.isBefore(start) && now.isBefore(end)
        } else {
            !now.isBefore(start) || now.isBefore(end)
        }
    }

    fun parseTime(timeStr: String?, fallback: LocalTime): LocalTime {
        if (timeStr.isNullOrBlank()) return fallback
        return try {
            LocalTime.parse(timeStr, storageFormatter)
        } catch (_: Exception) {
            fallback
        }
    }

    fun formatForStorage(hour: Int, minute: Int): String =
        String.format(Locale.US, "%02d:%02d", hour, minute)

    fun formatForDisplay(timeStr: String?, fallback: LocalTime): String =
        parseTime(timeStr, fallback).format(displayFormatter)
}
