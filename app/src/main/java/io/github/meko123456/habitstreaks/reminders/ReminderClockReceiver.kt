package io.github.meko123456.habitstreaks.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-aims the evening reminder when the time zone or the clock changes, and sets it after a restore.
 *
 * The run is set for the instant of the next 20:00, and a new zone moves 20:00 away from that
 * instant. Without this the first reminder after flying from Tbilisi to Tokyo would come at
 * 01:00, and only opening the app would put it right.
 *
 * A restore, on a new phone or after a reinstall, brings the habits back but not the reminder:
 * WorkManager keeps its queue out of backups. [RestoreAgent] sends [ACTION_RESTORED] here.
 */
class ReminderClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED, ACTION_RESTORED -> ReminderWorker.schedule(context)
        }
    }

    companion object {
        const val ACTION_RESTORED = "io.github.meko123456.habitstreaks.action.RESTORED"
    }
}
