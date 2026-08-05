package io.github.meko123456.habitstreaks.data.github

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Minimal GitHub GraphQL client — just enough to read the viewer's contribution calendar. */
class GithubClient(private val http: HttpClient = defaultHttpClient()) {

    @Serializable
    private data class GraphQLRequest(val query: String)

    suspend fun fetchContributions(token: String): Result<GithubContributions> = runCatching {
        val response: GraphQLResponse = http.post("https://api.github.com/graphql") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(GraphQLRequest(CONTRIBUTIONS_QUERY))
        }.body()

        response.errors?.firstOrNull()?.let { error("GitHub: ${it.message}") }
        val viewer = response.data?.viewer ?: error("GitHub: empty response")
        viewer.toGithubContributions()
    }

    companion object {
        val CONTRIBUTIONS_QUERY = """
            query {
              viewer {
                login
                contributionsCollection {
                  contributionCalendar {
                    totalContributions
                    weeks { contributionDays { date contributionCount } }
                  }
                }
              }
            }
        """.trimIndent()

        fun defaultHttpClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}
