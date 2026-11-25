package edu.cnm.deepdive.paula.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import edu.cnm.deepdive.paula.model.dao.RoutineDao
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reschedules active habit reminders after device reboot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var routineDao: RoutineDao

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                routineDao.findAllWithReminders().forEach { routine ->
                    ReminderManager.schedule(appContext, routine)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
