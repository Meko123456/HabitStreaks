package io.github.meko123456.habitstreaks.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Keeps the GitHub widget current.
 *
 * `updatePeriodMillis` in the widget's XML is the obvious mechanism and not a sufficient one: the
 * system treats it as best-effort, batches it, and suppresses it entirely while the device is dozing
 * — which is most of the night. That matters more than it sounds, because the widget shows *today's*
 * date and *today's* count. Without something that actually runs, the morning after would still read
 * yesterday's date beside yesterday's number, which is worse than showing nothing.
 *
 * WorkManager survives Doze by running in maintenance windows, so the rollover happens within half an
 * hour of midnight rather than whenever the phone next happens to be woken.
 *
 * Scheduled while a widget exists and cancelled when the last one is removed, so an app with no
 * widget on screen never polls GitHub.
 */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        GithubWidget().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val NAME = "github-widget-refresh"
        private const val INTERVAL_MINUTES = 30L

        /**
         * Starts the refresh if it is not already running.
         *
         * KEEP rather than REPLACE so calling this on every app launch does not reset the interval
         * and postpone the next run indefinitely.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
                INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            )
                // No point waking up to ask GitHub anything without a connection.
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Stops it once the last widget is gone. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
