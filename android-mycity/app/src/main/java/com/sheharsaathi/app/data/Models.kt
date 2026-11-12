package com.sheharsaathi.app.data

import androidx.annotation.DrawableRes

enum class ExploreFilter(val label: String) {
    ALL("All"), EVENTS("Music & Events"), CIVIC("Civic Alerts"), FOOD("Food & Drinks")
}

enum class EventCategory(val label: String) {
    MUSIC("Live Music & Gigs"), WORKSHOP("Workshops"), FLEA("Flea Markets"), TECH("Tech & Startups"), CULTURE("Books & Culture"), ART("Art & Crafts")
}

enum class DayBucket(val label: String) { TODAY("Today"), TOMORROW("Tomorrow"), WEEKEND("This Weekend") }

data class CityEvent(
    val id: String,
    val title: String,
    val category: EventCategory,
    val dateLabel: String,
    val timeLabel: String,
    val venue: String,
    val area: String,
    val priceRupees: Int,
    @DrawableRes val imageRes: Int,
    val interested: Int,
    val distanceKm: Double,
    val description: String,
    val bucket: DayBucket,
    val featured: Boolean = false,
    val fastFilling: Boolean = false,
    val lat: Double,
    val lng: Double,
    val pinLabel: String = title,
    val imageUri: String? = null,
    val hostedByYou: Boolean = false,
)

enum class IssueCategory(val label: String) {
    POTHOLE("Pothole & Roads"), STREETLIGHT("Streetlight"), GARBAGE("Garbage"), WATER("Water & Drainage")
}

enum class Urgency(val label: String, val sub: String, val sla: String) {
    LOW("Low", "Minor issue", "7–10 days"),
    MEDIUM("Medium", "Needs attention", "48–72 hours"),
    CRITICAL("Critical / Hazard", "Immediate risk", "24h escalation")
}

enum class IssueStatus(val label: String) {
    REPORTED("Reported"), UNDER_REVIEW("Under Review"), IN_PROGRESS("In Progress"), RESOLVED("Resolved")
}

data class Authority(val handle: String, val name: String, val verified: Boolean)

/**
 * What a resident says about a report they are looking at. One response per person per report;
 * it can be changed or removed. Responses are public as totals only and never change the
 * report's official status.
 */
enum class IssueResponse(val key: String, val label: String) {
    SEEN("seen", "I see this too"),
    STILL_THERE("still_there", "Still not fixed"),
    WORSE("worse", "It's getting worse"),
    FIXED("fixed", "It's fixed now");

    /** Says the problem is there right now, so it counts towards the report's priority. */
    val confirms: Boolean get() = this != FIXED

    companion object {
        fun of(key: String?): IssueResponse? = entries.firstOrNull { it.key == key }
    }
}

/** How many residents currently give each response to a report. */
data class ResponseCounts(val seen: Int = 0, val stillThere: Int = 0, val worse: Int = 0, val fixed: Int = 0) {
    operator fun get(r: IssueResponse): Int = when (r) {
        IssueResponse.SEEN -> seen
        IssueResponse.STILL_THERE -> stillThere
        IssueResponse.WORSE -> worse
        IssueResponse.FIXED -> fixed
    }

    fun plus(r: IssueResponse, by: Int): ResponseCounts = when (r) {
        IssueResponse.SEEN -> copy(seen = (seen + by).coerceAtLeast(0))
        IssueResponse.STILL_THERE -> copy(stillThere = (stillThere + by).coerceAtLeast(0))
        IssueResponse.WORSE -> copy(worse = (worse + by).coerceAtLeast(0))
        IssueResponse.FIXED -> copy(fixed = (fixed + by).coerceAtLeast(0))
    }

    /** Residents who say it has not been fixed. */
    val notFixed: Int get() = stillThere + worse
    val total: Int get() = seen + stillThere + worse + fixed
}

data class CivicIssue(
    val id: String,
    val title: String,
    val category: IssueCategory,
    val location: String,
    val description: String,
    val authorities: List<String>,
    val urgency: Urgency,
    val status: IssueStatus,
    val upvotes: Int,
    @DrawableRes val imageRes: Int? = null,
    val imageUri: String? = null,
    val reportedAgo: String,
    val isMine: Boolean = false,
    val lat: Double,
    val lng: Double,
    val authorityNote: String? = null,
    /** Public responses from residents; see [IssueResponse]. */
    val responses: ResponseCounts = ResponseCounts(),
    val lastResponse: IssueResponse? = null,
    val lastResponseAgo: String? = null,
)

enum class NewsKind { STORY, CIVIC_WIN, OFFER, BULLETIN }

data class NewsItem(
    val id: String,
    val kind: NewsKind,
    val tag: String,
    val ward: String,
    val ago: String,
    val title: String,
    val body: String,
    @DrawableRes val imageRes: Int? = null,
    val endorsements: Int,
    val comments: Int,
    val source: String,
    val offerCode: String? = null,
)

data class Spot(
    val id: String,
    val name: String,
    val sub: String,
    val rating: Double,
    @DrawableRes val imageRes: Int,
    val tag: String,
    val openNote: String,
    val lat: Double,
    val lng: Double,
)

data class CityAlert(
    val id: String,
    val authority: String,
    val title: String,
    val body: String,
    val window: String,
    val impact: String,
    val acknowledgements: Int,
)
