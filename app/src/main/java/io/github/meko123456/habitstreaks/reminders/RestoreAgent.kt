package io.github.meko123456.habitstreaks.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.content.Intent
import android.os.ParcelFileDescriptor

/**
 * Auto Backup does the backing up and restoring (fullBackupOnly). This agent is here for the moment
 * a restore finishes, on a new phone or after a reinstall: the habits come back, but the evening
 * reminder doesn't, since WorkManager keeps its queue out of backups, and only opening the app
 * would set it again.
 *
 * The agent runs in a restricted process, where WorkManager can't start, so it doesn't set the
 * reminder itself. It leaves one alarm a few seconds away; when that fires, the app starts as usual
 * and [ReminderClockReceiver] sets the reminder.
 */
class RestoreAgent : BackupAgent() {

    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor?) = Unit

    override fun onRestore(data: BackupDataInput?, appVersionCode: Int, newState: ParcelFileDescriptor?) = Unit

    override fun onRestoreFinished() {
        val alarms = getSystemService(AlarmManager::class.java) ?: return
        val aim = PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, ReminderClockReceiver::class.java).setAction(ReminderClockReceiver.ACTION_RESTORED),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + RESTART_DELAY_MS, aim)
    }

    private companion object {
        /** Long enough for the restricted process to be gone, so the alarm starts a normal one. */
        const val RESTART_DELAY_MS = 10_000L
    }
}
