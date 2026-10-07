package io.github.meko123456.habitstreaks.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.BreakIterator

/**
 * What the emoji field keeps of what was typed: the last whole character as a person sees it (one
 * grapheme cluster), so picking a new emoji replaces the old one.
 *
 * Whole matters because many emoji are several code points: 🧑‍💻 is 🧑, a zero-width joiner and
 * 💻, five UTF-16 chars. The field used to keep the first four chars, which cut it after half of
 * 💻 and saved "🧑" plus a broken character as the habit's emoji.
 */
internal fun lastGrapheme(typed: String): String {
    val text = typed.trim()
    if (text.isEmpty()) return ""
    val clusters = BreakIterator.getCharacterInstance()
    clusters.setText(text)
    val end = clusters.last()
    return text.substring(clusters.previous(), end)
}

@Composable
fun HabitEditorDialog(
    title: String,
    initialName: String = "",
    initialEmoji: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = lastGrapheme(it) },
                    label = { Text("Emoji (optional)") },
                    singleLine = true,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name, emoji) },
            ) { Text("Save") }
        },
        dismissButton = {
            if (onDelete != null) {
                TextButton(onClick = onDelete) { Text("Delete") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
