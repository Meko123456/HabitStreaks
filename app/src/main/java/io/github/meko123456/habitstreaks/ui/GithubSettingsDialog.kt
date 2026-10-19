package io.github.meko123456.habitstreaks.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Connect / disconnect the GitHub account. The token is a classic PAT with
 * read:user scope; it is stored encrypted via [TokenStore] and never leaves
 * the device except toward api.github.com.
 */
@Composable
fun GithubSettingsDialog(
    state: GithubState,
    token: String,
    onTokenChange: (String) -> Unit,
    onConnect: (token: String) -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GitHub") },
        text = {
            Column {
                when (state) {
                    is GithubState.Ready -> Text(
                        "Connected as @${state.contributions.login} · " +
                            "${contributionCount(state.contributions.total, LocalConfiguration.current.locales[0])} in the last year.",
                    )
                    is GithubState.Error -> Text(
                        "Couldn't connect: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                    )
                    GithubState.Loading -> Text("Connecting…")
                    GithubState.NotConnected -> Text(
                        "Paste a personal access token (read:user scope) to show " +
                            "your real contribution graph. Stored encrypted on-device.",
                    )
                }
                if (state !is GithubState.Ready) {
                    OutlinedTextField(
                        value = token,
                        onValueChange = onTokenChange,
                        label = { Text("Personal access token") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        // Hidden on screen, but typed as plain text the keyboard could learn it and
                        // offer it as a suggestion elsewhere. A password field is kept out of that.
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done,
                        ),
                        // Done connects, as the Connect button does.
                        keyboardActions = KeyboardActions(onDone = { if (token.isNotBlank()) onConnect(token) }),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (state is GithubState.Ready) {
                TextButton(onClick = onDisconnect) { Text("Disconnect") }
            } else {
                TextButton(
                    enabled = token.isNotBlank(),
                    onClick = { onConnect(token) },
                ) { Text("Connect") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
