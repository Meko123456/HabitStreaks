package io.github.meko123456.habitstreaks.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Round check button that marks today's completion for a habit. */
@Composable
fun TodayCheck(
    done: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (done) {
        FilledIconButton(
            onClick = onClick,
            modifier = modifier.size(44.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(Icons.Default.Check, contentDescription = "Done today — tap to undo")
        }
    } else {
        OutlinedIconButton(
            onClick = onClick,
            modifier = modifier.size(44.dp),
        ) {
            Icon(Icons.Default.Check, contentDescription = "Mark done today")
        }
    }
}
