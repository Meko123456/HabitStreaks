package io.github.meko123456.habitstreaks.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.meko123456.habitstreaks.MainActivity
import io.github.meko123456.habitstreaks.R
import io.github.meko123456.habitstreaks.data.HabitDatabase
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Evening nudge: if any habit is still unchecked for today, post a reminder.
 * Scheduled daily around [REMINDER_HOUR]; skips silently when everything is
 * done, there are no habits, or notifications are not permitted.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        remindIfAnythingIsOpen()
        // The next one is aimed from the clock, not from this run: a run Android held back an
        // hour must not move tomorrow's reminder an hour too.
        enqueue(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }

    private suspend fun remindIfAnythingIsOpen() {
        val context = applicationContext
        val dao = HabitDatabase.get(context).habitDao()
        val habits = dao.habitsOnce()
        if (habits.isEmpty()) return

        val done = dao.completedHabitIdsOn(LocalDate.now().toEpochDay()).size
        val open = habits.size - done
        if (open <= 0) return

        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val manager = context.getSystemService(NotificationManager::class.java)
        ensureChannel(context)

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Don't break the streak 🔥")
            .setContentText(
                if (open == 1) "1 habit still open today" else "$open habits still open today",
            )
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "daily_reminders"
        private const val NOTIFICATION_ID = 1001
        private const val WORK_NAME = "evening_reminder"

        /** The 24-hour periodic work this replaced, cancelled wherever it is still scheduled. */
        private const val PERIODIC_WORK_NAME = "daily_reminder"
        private const val REMINDER_HOUR = 20

        /**
         * Aims the reminder at the next [REMINDER_HOUR]:00, replacing whatever was set before.
         *
         * It used to be 24-hour periodic work whose first run was aimed at 20:00. Periodic work
         * counts each period from the last run rather than from the clock, so every run Android
         * held back moved all the later ones with it, and a new time zone or a change of summer
         * time moved them for good: on the test emulator the evening nudge had drifted to 11:29 in
         * the morning. A one-time run aimed at the next 20:00, set again after every run and at
         * every launch, can't drift.
         */
        fun schedule(context: Context) {
            ensureChannel(context)
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
            enqueue(context, ExistingWorkPolicy.REPLACE)
        }

        /**
         * The reminders' channel, with a line saying what it sends, for the app's notification
         * settings. It was made by the first reminder, with no description, so until an evening
         * with a habit left open Settings listed no category to look at or turn off.
         */
        private fun ensureChannel(context: Context) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "An evening nudge, on days when a habit is still unchecked."
                },
            )
        }

        /**
         * Whether an evening reminder would show: notifications on for the app (from Android 13 that
         * is the notification permission) and the reminders' channel not switched off.
         */
        fun canPost(context: Context): Boolean {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
            val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL_ID)
            return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
        }

        /** Where to turn them back on: the reminders' own switch when only that one is off. */
        fun settingsIntent(context: Context): Intent {
            val appOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
            val action = if (appOn) Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS else Settings.ACTION_APP_NOTIFICATION_SETTINGS
            return Intent(action)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .apply { if (appOn) putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID) }
        }

        private fun enqueue(context: Context, policy: ExistingWorkPolicy) {
            val delay = ReminderTime.untilNext(ZonedDateTime.now(), REMINDER_HOUR)
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
        }
    }
}
