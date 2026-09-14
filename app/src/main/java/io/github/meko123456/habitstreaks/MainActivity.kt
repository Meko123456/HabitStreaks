package io.github.meko123456.habitstreaks

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import io.github.meko123456.habitstreaks.reminders.ReminderWorker
import io.github.meko123456.habitstreaks.widget.WidgetRefreshWorker
import io.github.meko123456.habitstreaks.ui.HomeScreen
import io.github.meko123456.habitstreaks.ui.theme.HabitStreaksTheme

class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* optional */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderWorker.schedule(this)
        // Also here, not only from the widget receiver's onEnabled: that fires when the *first*
        // widget is added, so a widget already on the home screen before this code shipped would
        // never have had a refresh scheduled. KEEP makes repeating it harmless.
        WidgetRefreshWorker.schedule(this)
        askForNotificationsIfNeeded()
        setContent {
            HabitStreaksTheme {
                HomeScreen()
            }
        }
    }

    private fun askForNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
