package com.example.expensestracker.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.expensestracker.R
import com.example.expensestracker.util.ReminderWorker
import java.util.concurrent.TimeUnit
import android.util.Log
// Removed Calendar, OneTimeWorkRequestBuilder, ExistingWorkPolicy as they are no longer needed for this simplified scheduling

class SettingsActivity : AppCompatActivity() {

    private lateinit var notificationSwitch: Switch
    private lateinit var workManager: WorkManager
    private val WORK_TAG = "DailyBudgetReminder" // Unique tag for our periodic work

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        notificationSwitch = findViewById(R.id.switch_notification_reminders)
        workManager = WorkManager.getInstance(applicationContext)

        // Load saved preference for the switch state
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val remindersEnabled = sharedPref.getBoolean("reminders_enabled", false)
        notificationSwitch.isChecked = remindersEnabled

        // Set listener for switch changes
        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save preference
            with(sharedPref.edit()) {
                putBoolean("reminders_enabled", isChecked)
                apply()
            }

            if (isChecked) {
                // Check for notification permission before scheduling
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            NOTIFICATION_PERMISSION_REQUEST_CODE
                        )
                        Toast.makeText(this, "Please grant notification permission for reminders.", Toast.LENGTH_LONG).show()
                        Log.w("SettingsActivity", "Notification permission not granted. Requesting it.")
                    }
                }
                scheduleDailyReminder() // Only schedule the daily periodic reminder
                Toast.makeText(this, "Daily reminders enabled!", Toast.LENGTH_SHORT).show()
            } else {
                cancelDailyReminder() // Only cancel the daily periodic reminder
                Toast.makeText(this, "Daily reminders disabled.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scheduleDailyReminder() {
        // Schedule the periodic 24-hour reminder.
        // WorkManager will decide the optimal time to run this within each 24-hour period,
        // providing the flexibility you're looking for.
        val dailyPeriodicWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            24, TimeUnit.HOURS // Repeat every 24 hours
        )
            .addTag(WORK_TAG) // Tag to easily identify and cancel this work
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.REPLACE, // Replace any existing work with the same tag
            dailyPeriodicWorkRequest
        )
        Log.d("SettingsActivity", "Daily 24-hour flexible reminder scheduled with tag: $WORK_TAG")
    }

    private fun cancelDailyReminder() {
        workManager.cancelUniqueWork(WORK_TAG) // Cancels the 24-hour periodic work
        Log.d("SettingsActivity", "Daily flexible reminder cancelled with tag: $WORK_TAG")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (notificationSwitch.isChecked) {
                    scheduleDailyReminder() // Re-schedule if permission granted and switch is on
                    Toast.makeText(this, "Notification permission granted. Reminders enabled!", Toast.LENGTH_SHORT).show()
                }
            } else {
                notificationSwitch.isChecked = false
                Toast.makeText(this, "Notification permission denied. Reminders cannot be enabled.", Toast.LENGTH_LONG).show()
                Log.w("SettingsActivity", "Notification permission denied by user.")
            }
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}
