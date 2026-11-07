package com.sheharsaathi.app.data.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Rows as the Supabase REST API returns them. Field names follow the database columns.

@Serializable
data class AuthorityRef(
    @SerialName("short_name") val shortName: String = "",
    @SerialName("x_handle") val xHandle: String? = null,
)

@Serializable
data class IssueRow(
    val id: String,
    @SerialName("reporter_id") val reporterId: String? = null,
    val category: String,
    val title: String,
    val description: String = "",
    val urgency: String = "medium",
    val status: String = "reported",
    @SerialName("location_text") val locationText: String? = null,
    val ward: String? = null,
    val lat: Double,
    val lng: Double,
    @SerialName("photo_path") val photoPath: String? = null,
    @SerialName("upvote_count") val upvoteCount: Int = 0,
    @SerialName("created_at") val createdAt: String,
    val authorities: AuthorityRef? = null,
)

@Serializable
data class IssueUpdateRow(
    @SerialName("issue_id") val issueId: String,
    val note: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class EventRow(
    val id: String,
    @SerialName("host_id") val hostId: String? = null,
    val title: String,
    val description: String = "",
    val category: String,
    val venue: String,
    val area: String? = null,
    val lat: Double,
    val lng: Double,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("price_rupees") val priceRupees: Int = 0,
    val capacity: Int? = null,
    @SerialName("image_path") val imagePath: String? = null,
    val featured: Boolean = false,
    @SerialName("booked_count") val bookedCount: Int = 0,
    @SerialName("interested_count") val interestedCount: Int = 0,
)

@Serializable
data class NewsRow(
    val id: String,
    val kind: String,
    val title: String,
    val body: String,
    val source: String,
    val ward: String? = null,
    val tag: String? = null,
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("offer_code") val offerCode: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("endorsement_count") val endorsementCount: Int = 0,
)

@Serializable
data class AlertRow(
    val id: String,
    val title: String,
    val body: String,
    val impact: String? = null,
    @SerialName("time_window") val timeWindow: String? = null,
    val authorities: AuthorityRef? = null,
)

@Serializable
data class PollOptionRow(
    val id: Long,
    val label: String,
    val position: Int,
    @SerialName("vote_count") val voteCount: Int = 0,
)

@Serializable
data class PollRow(
    val id: String,
    val question: String,
    val detail: String? = null,
    val ward: String? = null,
    @SerialName("ends_at") val endsAt: String,
    @SerialName("poll_options") val options: List<PollOptionRow> = emptyList(),
)

@Serializable
data class ProfileRow(
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val ward: String? = null,
    @SerialName("photo_path") val photoPath: String? = null,
)

@Serializable
data class NotifRow(val civic: Boolean = true, val events: Boolean = true, val emergency: Boolean = true)

@Serializable
data class BookingRow(
    @SerialName("event_id") val eventId: String,
    val quantity: Int,
    val status: String = "confirmed",
    @SerialName("booking_code") val bookingCode: String = "",
    @SerialName("total_rupees") val totalRupees: Int = 0,
)

@Serializable data class IssueIdRow(@SerialName("issue_id") val issueId: String)
@Serializable data class EventIdRow(@SerialName("event_id") val eventId: String)
@Serializable data class NewsIdRow(@SerialName("news_id") val newsId: String)
@Serializable data class AlertIdRow(@SerialName("alert_id") val alertId: String)
@Serializable data class VoteRow(@SerialName("poll_id") val pollId: String, @SerialName("option_id") val optionId: Long)
