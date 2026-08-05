package io.github.meko123456.habitstreaks.data.github

import java.time.LocalDate
import kotlinx.serialization.Serializable

// DTOs mirroring the GraphQL response shape (unknown keys ignored).

@Serializable
data class GraphQLResponse(val data: Data? = null, val errors: List<GraphQLError>? = null)

@Serializable
data class GraphQLError(val message: String)

@Serializable
data class Data(val viewer: Viewer? = null)

@Serializable
data class Viewer(val login: String, val contributionsCollection: ContributionsCollection)

@Serializable
data class ContributionsCollection(val contributionCalendar: ContributionCalendar)

@Serializable
data class ContributionCalendar(val totalContributions: Int, val weeks: List<Week>)

@Serializable
data class Week(val contributionDays: List<ContributionDay>)

@Serializable
data class ContributionDay(val date: String, val contributionCount: Int)

/** Domain-facing result: who + total + contributions per epoch day. */
data class GithubContributions(
    val login: String,
    val total: Int,
    val countsByDay: Map<Long, Int>,
)

fun Viewer.toGithubContributions(): GithubContributions {
    val calendar = contributionsCollection.contributionCalendar
    val counts = buildMap {
        for (week in calendar.weeks) {
            for (day in week.contributionDays) {
                if (day.contributionCount > 0) {
                    put(LocalDate.parse(day.date).toEpochDay(), day.contributionCount)
                }
            }
        }
    }
    return GithubContributions(
        login = login,
        total = calendar.totalContributions,
        countsByDay = counts,
    )
}
