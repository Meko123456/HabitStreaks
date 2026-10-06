package io.github.meko123456.habitstreaks.data.github

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Minimal GitHub GraphQL client — just enough to read the viewer's contribution calendar. */
class GithubClient(private val http: HttpClient = defaultHttpClient()) {

    @Serializable
    private data class GraphQLRequest(val query: String)

    /** What GitHub sends instead of a GraphQL response when it refuses the request outright. */
    @Serializable
    private data class RestError(val message: String? = null)

    suspend fun fetchContributions(token: String): Result<GithubContributions> = runCatching {
        val reply = http.post("https://api.github.com/graphql") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(GraphQLRequest(CONTRIBUTIONS_QUERY))
        }
        // A token GitHub won't take comes back as HTTP 401 with {"message": "Bad credentials"}.
        // Decoded as a GraphQL response, that has neither data nor errors, and read as "empty
        // response", which told nobody that the token was the problem.
        if (!reply.status.isSuccess()) {
            val why = runCatching { reply.body<RestError>().message }.getOrNull()?.let { " ($it)" }.orEmpty()
            error(
                if (reply.status == HttpStatusCode.Unauthorized) {
                    "GitHub didn't accept the token$why. Check it hasn't expired and has the read:user scope."
                } else {
                    "GitHub answered HTTP ${reply.status.value}$why"
                },
            )
        }
        val response: GraphQLResponse = reply.body()

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

        fun defaultHttpClient(): HttpClient = httpClient(OkHttp.create())

        /** The client's setup on any engine, so the tests run the same JSON configuration. */
        internal fun httpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}
