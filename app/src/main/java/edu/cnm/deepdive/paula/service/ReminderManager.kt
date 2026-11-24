package edu.cnm.deepdive.paula.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import edu.cnm.deepdive.paula.model.entity.Routine
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Utility orchestrating alarm scheduling and cancellation for habit reminders.
 */
object ReminderManager {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun schedule(context: Context, routine: Routine) {
        val reminderTimeStr = routine.reminderTime ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        try {
            val localTime = LocalTime.parse(reminderTimeStr, timeFormatter)
            var targetDateTime = LocalDateTime.now()
                .withHour(localTime.hour)
                .withMinute(localTime.minute)
                .withSecond(0)
                .withNano(0)

            if (targetDateTime.isBefore(LocalDateTime.now())) {
                targetDateTime = targetDateTime.plusDays(1)
            }

            val triggerMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_REMINDER
                putExtra(ReminderReceiver.EXTRA_ROUTINE_ID, routine.id)
                putExtra(ReminderReceiver.EXTRA_ROUTINE_NAME, routine.name)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                routine.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (_: Exception) {
            // Gracefully ignore parse errors
        }
    }

    fun cancel(context: Context, routineId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            routineId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
