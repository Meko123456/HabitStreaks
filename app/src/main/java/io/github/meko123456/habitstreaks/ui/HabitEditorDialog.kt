package io.github.meko123456.habitstreaks.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
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
    var name by rememberSaveable { mutableStateOf(initialName) }
    var emoji by rememberSaveable { mutableStateOf(initialEmoji) }
    // Delete stands where Cancel usually does, and took the habit and every day checked off with
    // one tap. It asks first now; Cancel goes back to the editor.
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val nameField = remember { FocusRequester() }

    if (onDelete != null && confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete “$initialName”?") },
            text = { Text("Its streak and every day checked off go with it. A deleted habit can't be recovered.") },
            confirmButton = { TextButton(onClick = onDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                // The keyboard's action key goes from the name to the emoji, then saves as Save does.
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.focusRequester(nameField),
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = lastGrapheme(it) },
                    label = { Text("Emoji (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onConfirm(name, emoji) }),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            // A new habit (from + or the Add a habit shortcut) starts with its name, so that field has
            // the keyboard at once; editing one opens on what's there, with no keyboard over it.
            LaunchedEffect(Unit) { if (initialName.isEmpty()) nameField.requestFocus() }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name, emoji) },
            ) { Text("Save") }
        },
        dismissButton = {
            if (onDelete != null) {
                TextButton(onClick = { confirmingDelete = true }) { Text("Delete") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
