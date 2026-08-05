package io.github.meko123456.habitstreaks.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.meko123456.habitstreaks.MainActivity
import io.github.meko123456.habitstreaks.R
import io.github.meko123456.habitstreaks.data.HabitDatabase
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
        val context = applicationContext
        val dao = HabitDatabase.get(context).habitDao()
        val habits = dao.habitsOnce()
        if (habits.isEmpty()) return Result.success()

        val done = dao.completedHabitIdsOn(LocalDate.now().toEpochDay()).size
        val open = habits.size - done
        if (open <= 0) return Result.success()

        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return Result.success()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Daily reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )

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
        return Result.success()
    }

    companion object {
        private const val CHANNEL_ID = "daily_reminders"
        private const val NOTIFICATION_ID = 1001
        private const val WORK_NAME = "daily_reminder"
        private const val REMINDER_HOUR = 20

        /** Enqueue the daily check, first run at the next [REMINDER_HOUR]:00. */
        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(LocalTime.of(REMINDER_HOUR, 0))
            if (!next.isAfter(now)) next = next.plusDays(1)
            val initialDelay = Duration.between(now, next)

            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
