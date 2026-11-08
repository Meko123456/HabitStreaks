package io.github.meko123456.habitstreaks.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.meko123456.habitstreaks.data.github.GithubContributions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The GitHub dialog's status line is what TalkBack is told as a connection is tried. TalkBack's
 * speech can't be observed from a test, so this checks what it acts on: the line is a polite live
 * region, and each change is a change of its words, on the same node, not a new node appearing.
 */
@RunWith(AndroidJUnit4::class)
class GithubSettingsDialogTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theStatusLineIsOnePoliteLiveRegionWhoseWordsChange() {
        var state by mutableStateOf<GithubState>(GithubState.NotConnected)
        rule.setContent {
            GithubSettingsDialog(
                state = state,
                token = "ghp_example",
                onTokenChange = {},
                onConnect = {},
                onDisconnect = {},
                onDismiss = {},
            )
        }
        val status = rule.onNodeWithTag(GITHUB_STATUS_TAG)
        status.assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        val node = status.fetchSemanticsNode().id

        state = GithubState.Loading
        status.assertTextEquals("Connecting…")
        assertEquals(node, status.fetchSemanticsNode().id)

        state = GithubState.Error("The token was refused.")
        status.assertTextEquals("Couldn't connect: The token was refused.")
        assertEquals(node, status.fetchSemanticsNode().id)

        state = GithubState.Loading
        state = GithubState.Ready(GithubContributions(login = "octo", total = 1234, countsByDay = emptyMap(), firstDay = 0))
        status.assertTextEquals("Connected as @octo · 1,234 contributions in the last year.")
        assertEquals(node, status.fetchSemanticsNode().id)
        status.assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }
}
