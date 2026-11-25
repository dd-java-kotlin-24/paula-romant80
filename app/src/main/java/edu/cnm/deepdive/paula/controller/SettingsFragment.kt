package edu.cnm.deepdive.paula.controller

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import edu.cnm.deepdive.paula.R
import edu.cnm.deepdive.paula.service.QuietHoursHelper
import java.time.LocalTime

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        setupQuietHoursPreference(
            key = QuietHoursHelper.KEY_QUIET_HOURS_START,
            fallback = LocalTime.of(22, 0),
            titleRes = R.string.pref_quiet_hours_start_title
        )
        setupQuietHoursPreference(
            key = QuietHoursHelper.KEY_QUIET_HOURS_END,
            fallback = LocalTime.of(7, 0),
            titleRes = R.string.pref_quiet_hours_end_title
        )
    }

    private fun setupQuietHoursPreference(key: String, fallback: LocalTime, titleRes: Int) {
        val pref = findPreference<Preference>(key) ?: return
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        pref.summary = QuietHoursHelper.formatForDisplay(prefs.getString(key, null), fallback)

        pref.setOnPreferenceClickListener {
            val current = QuietHoursHelper.parseTime(prefs.getString(key, null), fallback)
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(current.hour)
                .setMinute(current.minute)
                .setTitleText(titleRes)
                .build()

            picker.addOnPositiveButtonClickListener {
                val stored = QuietHoursHelper.formatForStorage(picker.hour, picker.minute)
                prefs.edit().putString(key, stored).apply()
                pref.summary = QuietHoursHelper.formatForDisplay(stored, fallback)
            }
            picker.show(parentFragmentManager, "picker_$key")
            true
        }
    }
}
