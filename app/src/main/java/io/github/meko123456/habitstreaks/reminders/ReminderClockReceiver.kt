package io.github.meko123456.habitstreaks.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-aims the evening reminder when the time zone or the clock changes.
 *
 * The run is set for the instant of the next 20:00, and a new zone moves 20:00 away from that
 * instant. Without this the first reminder after flying from Tbilisi to Tokyo would come at
 * 01:00, and only opening the app would put it right.
 */
class ReminderClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_TIMEZONE_CHANGED || intent.action == Intent.ACTION_TIME_CHANGED) {
            ReminderWorker.schedule(context)
        }
    }
}
