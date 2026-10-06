package io.github.meko123456.habitstreaks.data.github

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What the client makes of each kind of reply GitHub sends, through the app's real JSON setup.
 * The wording matters: it is what the GitHub settings dialog shows when connecting fails.
 */
class GithubClientTest {

    private fun replying(status: HttpStatusCode, body: String) = GithubClient(
        GithubClient.httpClient(
            MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) },
        ),
    )

    @Test
    fun `a token GitHub refuses is reported as the token, not as an empty response`() = runBlocking {
        // GitHub's actual 401 body for a bad token. Decoded as GraphQL it has neither data nor
        // errors, which used to read as "GitHub: empty response".
        val body = """{"message":"Bad credentials","documentation_url":"https://docs.github.com/graphql","status":"401"}"""
        val failure = replying(HttpStatusCode.Unauthorized, body).fetchContributions("ghp_wrong").exceptionOrNull()
        assertEquals(
            "GitHub didn't accept the token (Bad credentials). Check it hasn't expired and has the read:user scope.",
            failure?.message,
        )
    }

    @Test
    fun `any other refusal names its status, with GitHub's reason when there is one`() = runBlocking {
        assertEquals(
            "GitHub answered HTTP 502",
            replying(HttpStatusCode.BadGateway, "<html>Bad gateway</html>").fetchContributions("t").exceptionOrNull()?.message,
        )
        assertEquals(
            "GitHub answered HTTP 403 (API rate limit exceeded)",
            replying(HttpStatusCode.Forbidden, """{"message":"API rate limit exceeded"}""").fetchContributions("t").exceptionOrNull()?.message,
        )
    }

    @Test
    fun `errors inside a successful GraphQL reply are still reported as before`() = runBlocking {
        val body = """{"errors":[{"message":"Your token has not been granted the required scopes"}]}"""
        assertEquals(
            "GitHub: Your token has not been granted the required scopes",
            replying(HttpStatusCode.OK, body).fetchContributions("t").exceptionOrNull()?.message,
        )
    }

    @Test
    fun `a good reply becomes the contribution calendar`() = runBlocking {
        val body = """{"data":{"viewer":{"login":"meko","contributionsCollection":{"contributionCalendar":
            {"totalContributions":5,"weeks":[{"contributionDays":[
            {"date":"2026-10-04","contributionCount":0},{"date":"2026-10-05","contributionCount":5}]}]}}}}}"""
        val contributions = replying(HttpStatusCode.OK, body).fetchContributions("t").getOrThrow()
        assertEquals("meko", contributions.login)
        assertEquals(mapOf(LocalDate.parse("2026-10-05").toEpochDay() to 5), contributions.countsByDay)
    }
}
