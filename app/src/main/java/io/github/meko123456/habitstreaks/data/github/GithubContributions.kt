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
    /** The calendar's first day. GitHub sends the last year, so nothing earlier is known. */
    val firstDay: Long,
) {
    /**
     * Contributions on [epochDay], or null before [firstDay]. Zero-count days are left out of
     * [countsByDay], so reading a missing day as 0 called every day before the fetched year one
     * with no contributions.
     */
    fun countOn(epochDay: Long): Int? = if (epochDay < firstDay) null else countsByDay[epochDay] ?: 0
}

fun Viewer.toGithubContributions(): GithubContributions {
    val calendar = contributionsCollection.contributionCalendar
    var firstDay = Long.MAX_VALUE // an empty calendar knows no day at all
    val counts = buildMap {
        for (week in calendar.weeks) {
            for (day in week.contributionDays) {
                val epochDay = LocalDate.parse(day.date).toEpochDay()
                firstDay = minOf(firstDay, epochDay)
                if (day.contributionCount > 0) {
                    put(epochDay, day.contributionCount)
                }
            }
        }
    }
    return GithubContributions(
        login = login,
        total = calendar.totalContributions,
        countsByDay = counts,
        firstDay = firstDay,
    )
}
