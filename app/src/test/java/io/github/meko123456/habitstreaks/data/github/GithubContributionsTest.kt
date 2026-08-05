package io.github.meko123456.habitstreaks.data.github

import java.time.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GithubContributionsTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val sample = """
        {
          "data": {
            "viewer": {
              "login": "Meko123456",
              "someFutureField": true,
              "contributionsCollection": {
                "contributionCalendar": {
                  "totalContributions": 42,
                  "weeks": [
                    { "contributionDays": [
                        { "date": "2026-08-03", "contributionCount": 25 },
                        { "date": "2026-08-04", "contributionCount": 10 },
                        { "date": "2026-08-05", "contributionCount": 0 }
                    ]}
                  ]
                }
              }
            }
          }
        }
    """.trimIndent()

    @Test
    fun `parses response and maps to epoch-day counts`() {
        val viewer = json.decodeFromString<GraphQLResponse>(sample).data!!.viewer!!
        val result = viewer.toGithubContributions()

        assertEquals("Meko123456", result.login)
        assertEquals(42, result.total)
        assertEquals(2, result.countsByDay.size)
        assertEquals(25, result.countsByDay[LocalDate.parse("2026-08-03").toEpochDay()])
        assertEquals(10, result.countsByDay[LocalDate.parse("2026-08-04").toEpochDay()])
        // zero-count days are omitted entirely
        assertNull(result.countsByDay[LocalDate.parse("2026-08-05").toEpochDay()])
    }

    @Test
    fun `parses GraphQL errors`() {
        val errorJson = """{ "errors": [{ "message": "Bad credentials" }] }"""
        val response = json.decodeFromString<GraphQLResponse>(errorJson)
        assertEquals("Bad credentials", response.errors!!.single().message)
        assertNull(response.data)
    }
}
