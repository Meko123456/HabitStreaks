package io.github.meko123456.habitstreaks.reminders

import android.content.Context
import androidx.core.content.edit

/**
 * Home's word that the evening reminder can't show. A refused notification prompt, or the
 * reminders switched off in Settings, used to stop it with nothing in the app saying so.
 * "Not now" holds until notifications are back on, so switching them off again later brings
 * the notice back.
 */
object ReminderNotice {

    fun shouldShow(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (ReminderWorker.canPost(context)) {
            if (prefs.contains(KEY_DISMISSED)) prefs.edit { remove(KEY_DISMISSED) }
            return false
        }
        return !prefs.getBoolean(KEY_DISMISSED, false)
    }

    fun dismiss(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_DISMISSED, true) }
    }

    private const val PREFS = "reminders"
    private const val KEY_DISMISSED = "notice_dismissed"
}
