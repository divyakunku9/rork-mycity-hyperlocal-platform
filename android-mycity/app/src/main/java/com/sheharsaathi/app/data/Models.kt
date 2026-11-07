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
