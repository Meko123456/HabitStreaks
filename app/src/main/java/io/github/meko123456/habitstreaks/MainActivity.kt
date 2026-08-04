package io.github.meko123456.habitstreaks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.meko123456.habitstreaks.ui.HomeScreen
import io.github.meko123456.habitstreaks.ui.theme.HabitStreaksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitStreaksTheme {
                HomeScreen()
            }
        }
    }
}
