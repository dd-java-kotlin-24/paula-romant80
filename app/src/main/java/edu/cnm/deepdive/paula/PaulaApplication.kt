package edu.cnm.deepdive.paula

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import edu.cnm.deepdive.paula.service.ReminderReceiver

/**
 * Initializes application-level resources that cannot be handled with Hilt dependency injection.
 * This class must be referenced in `AndroidManifest.xml`, or it will not be loaded and used
 * by the Android system.
 */
@HiltAndroidApp
class PaulaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ReminderReceiver.createNotificationChannel(this)
    }
}
