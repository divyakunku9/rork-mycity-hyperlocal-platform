package com.sheharsaathi.app.data.backend

import com.sheharsaathi.app.R
import com.sheharsaathi.app.data.CityAlert
import com.sheharsaathi.app.data.CityEvent
import com.sheharsaathi.app.data.CivicIssue
import com.sheharsaathi.app.data.EventCategory
import com.sheharsaathi.app.data.IssueCategory
import com.sheharsaathi.app.data.IssueStatus
import com.sheharsaathi.app.data.NewsItem
import com.sheharsaathi.app.data.NewsKind
import com.sheharsaathi.app.data.ServiceStatus
import com.sheharsaathi.app.data.Transit
import com.sheharsaathi.app.data.Urgency
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** A ward poll as the Civic screen shows it. [id] is null for the built-in sample poll. */
data class PollUi(
    val id: String?,
    val title: String,
    val detail: String,
    val endsLabel: String,
    val options: List<PollOptionUi>,
)

data class PollOptionUi(val id: Long?, val label: String, val votes: Int)

/** Everything the signed-in user has done that the screens need to show. */
data class MyActivity(
    val upvoted: Set<String> = emptySet(),
    val savedEvents: Set<String> = emptySet(),
    val bookings: Map<String, Int> = emptyMap(),
    val bookingCodes: Map<String, String> = emptyMap(),
    val endorsed: Set<String> = emptySet(),
    val savedNews: Set<String> = emptySet(),
    val claimed: Set<String> = emptySet(),
    val votes: Map<String, Long> = emptyMap(),
    val acked: Set<String> = emptySet(),
)

/** Reads and writes Shehar Saathi content in Supabase and maps rows to the app's models. */
class CityBackend(val sb: Supabase) {

    private val json get() = sb.json

    // ------------------------------------------------------------------ Reading

    suspend fun issues(cityId: String, myId: String?, fromLat: Double, fromLng: Double): Res<List<CivicIssue>> {
        val rows = sb.select(
            "civic_issues",
            "select" to "*,authorities(short_name,x_handle)",
            "city_id" to "eq.$cityId",
            "order" to "created_at.desc",
            "limit" to "200",
        ).decode<List<IssueRow>>()
        if (rows is Res.Err) return rows
        // Latest official note per issue, shown as "Authority update" on the issue screen.
        val notes = sb.select(
            "issue_updates",
            "select" to "issue_id,note,created_at",
            "is_official" to "eq.true",
            "order" to "created_at.desc",
            "limit" to "500",
        ).decode<List<IssueUpdateRow>>().valueOrNull().orEmpty()
            .groupBy { it.issueId }.mapValues { it.value.first().note }
        return Res.Ok((rows as Res.Ok).value.map { it.toIssue(myId, notes[it.id]) })
    }

    suspend fun events(cityId: String, myId: String?, fromLat: Double, fromLng: Double): Res<List<CityEvent>> {
        val now = System.currentTimeMillis()
        val rows = sb.select(
            "events",
            "select" to "*",
            "city_id" to "eq.$cityId",
            "status" to "eq.published",
            "starts_at" to "gte.${Times.iso(now - 3 * 3_600_000L)}",
            "and" to "(starts_at.lt.${Times.iso(now + 30 * 86_400_000L)})",
            "order" to "starts_at.asc",
            "limit" to "200",
        ).decode<List<EventRow>>()
        if (rows is Res.Err) return rows
        return Res.Ok((rows as Res.Ok).value.map { it.toEvent(myId, fromLat, fromLng) })
    }

    /** Latest headlines gathered from city news sites, newest first. */
    suspend fun webNews(cityId: String): Res<List<com.sheharsaathi.app.data.WebStory>> {
        val rows = sb.select(
            "news_articles",
            "select" to "id,url,title,summary,image_url,category,published_at,news_sources(name,site_url)",
            "city_id" to "eq.$cityId",
            "order" to "published_at.desc",
            "limit" to "150",
        ).decode<List<WebNewsRow>>()
        if (rows is Res.Err) return rows
        val seen = HashSet<String>()
        return Res.Ok((rows as Res.Ok).value.mapNotNull { r ->
            // The same story sometimes appears in two of a publisher's feeds under different links.
            val key = r.title.lowercase().filter { it.isLetterOrDigit() }.take(70)
            if (!seen.add(key)) return@mapNotNull null
            com.sheharsaathi.app.data.WebStory(
                id = r.id, url = r.url, title = r.title, summary = r.summary, imageUrl = r.imageUrl,
                category = com.sheharsaathi.app.data.NewsTopic.of(r.category),
                source = r.source?.name ?: hostOf(r.url), siteUrl = r.source?.siteUrl,
                publishedMs = Times.parse(r.publishedAt),
            )
        })
    }

    private fun hostOf(url: String): String =
        runCatching { java.net.URI(url).host.removePrefix("www.") }.getOrNull() ?: "News"

    /** Upcoming events listed on ticketing/listing sites, soonest first. */
    suspend fun listedEvents(cityId: String): Res<List<com.sheharsaathi.app.data.ListedEvent>> {
        val now = System.currentTimeMillis()
        val rows = sb.select(
            "external_events",
            "select" to "id,url,title,summary,image_url,starts_at,ends_at,has_time,venue,address,lat,lng,price_min,price_max,is_free,category,event_sources(name,book_label)",
            "city_id" to "eq.$cityId",
            // Starting from today, or a multi-day show still running.
            "or" to "(starts_at.gte.${Times.iso(now - 12 * 3_600_000L)},ends_at.gte.${Times.iso(now)})",
            "order" to "starts_at.asc",
            "limit" to "400",
        ).decode<List<ListedEventRow>>()
        if (rows is Res.Err) return rows
        return Res.Ok((rows as Res.Ok).value.map { r ->
            com.sheharsaathi.app.data.ListedEvent(
                id = r.id, url = r.url, title = r.title, summary = r.summary, imageUrl = r.imageUrl,
                startMs = Times.parse(r.startsAt), endMs = r.endsAt?.let { Times.parse(it) }?.takeIf { it > 0 }, hasTime = r.hasTime,
                venue = r.venue, address = r.address, lat = r.lat, lng = r.lng,
                priceMin = r.priceMin, priceMax = r.priceMax, isFree = r.isFree,
                category = com.sheharsaathi.app.data.ListedCategory.of(r.category),
                source = r.source?.name ?: hostOf(r.url), bookLabel = r.source?.bookLabel ?: "View",
            )
        }.filter { it.isUpcoming(now) })
    }

    suspend fun news(cityId: String): Res<List<NewsItem>> {
        val rows = sb.select(
            "news_items",
            "select" to "*",
            "city_id" to "eq.$cityId",
            "is_published" to "eq.true",
            "order" to "published_at.desc.nullslast",
            "limit" to "100",
        ).decode<List<NewsRow>>()
        if (rows is Res.Err) return rows
        return Res.Ok((rows as Res.Ok).value.map { it.toNews() })
    }

    /** The newest alert that is in force now, or null. */
    suspend fun alert(cityId: String): Res<CityAlert?> {
        val now = Times.iso(System.currentTimeMillis())
        val rows = sb.select(
            "city_alerts",
            "select" to "*,authorities(short_name,x_handle)",
            "city_id" to "eq.$cityId",
            "starts_at" to "lte.$now",
            "or" to "(ends_at.is.null,ends_at.gt.$now)",
            "order" to "is_emergency.desc,starts_at.desc",
            "limit" to "1",
        ).decode<List<AlertRow>>()
        if (rows is Res.Err) return rows
        return Res.Ok((rows as Res.Ok).value.firstOrNull()?.let {
            CityAlert(
                id = it.id, authority = it.authorities?.shortName ?: "City", title = it.title, body = it.body,
                window = it.timeWindow.orEmpty(), impact = it.impact.orEmpty(), acknowledgements = 0,
            )
        })
    }

    /** The newest open poll for the user's ward (or a city-wide one). */
    suspend fun poll(cityId: String, ward: String): Res<PollUi?> {
        val now = System.currentTimeMillis()
        val rows = sb.select(
            "polls",
            "select" to "id,question,detail,ward,ends_at,poll_options(id,label,position,vote_count)",
            "city_id" to "eq.$cityId",
            "ends_at" to "gt.${Times.iso(now)}",
            "order" to "created_at.desc",
            "limit" to "10",
        ).decode<List<PollRow>>()
        if (rows is Res.Err) return rows
        val p = (rows as Res.Ok).value.firstOrNull { it.ward.isNullOrBlank() || it.ward.equals(ward, ignoreCase = true) }
            ?: return Res.Ok(null)
        return Res.Ok(
            PollUi(
                id = p.id,
                title = p.question,
                detail = p.detail.orEmpty(),
                endsLabel = Times.endsLabel(Times.parse(p.endsAt), now),
                options = p.options.sortedBy { it.position }.map { PollOptionUi(it.id, it.label, it.voteCount) },
            )
        )
    }

    /** Official transit notices posted by staff, most serious first. */
    suspend fun serviceStatus(cityId: String): Res<List<ServiceStatus>> {
        val rows = sb.select(
            "service_status",
            "select" to "title,body,level,transit_lines(code,color_hex)",
            "city_id" to "eq.$cityId",
            "order" to "level.desc,updated_at.desc",
            "limit" to "20",
        ).decode<List<ServiceStatusRow>>()
        if (rows is Res.Err) return rows
        return Res.Ok((rows as Res.Ok).value.map { r ->
            val hex = r.line?.colorHex?.removePrefix("#")?.takeIf { it.length == 6 }
            ServiceStatus(
                title = r.title,
                tag = when (r.level) { 0 -> "Good service"; 1 -> "Minor delays"; else -> "Disrupted" },
                body = r.body.orEmpty(),
                level = r.level,
                color = hex?.toLongOrNull(16)?.let { 0xFF000000L or it } ?: 0xFF5C6BC0L,
            )
        })
    }

    suspend fun myActivity(uid: String): MyActivity = coroutineScope {
        fun get(table: String, cols: String) = async { sb.select(table, "select" to cols, "user_id" to "eq.$uid") }
        val upvotes = get("issue_upvotes", "issue_id")
        val eventSaves = get("event_saves", "event_id")
        val bookings = get("event_bookings", "event_id,quantity,status,booking_code")
        val endorsed = get("news_endorsements", "news_id")
        val newsSaves = get("news_saves", "news_id")
        val claims = get("offer_claims", "news_id")
        val votes = get("poll_votes", "poll_id,option_id")
        val acks = get("alert_acknowledgements", "alert_id")
        val myBookings = bookings.await().decodeList<BookingRow>().orEmpty().filter { it.status == "confirmed" }
        MyActivity(
            upvoted = upvotes.await().decodeList<IssueIdRow>().orEmpty().map { it.issueId }.toSet(),
            savedEvents = eventSaves.await().decodeList<EventIdRow>().orEmpty().map { it.eventId }.toSet(),
            bookings = myBookings.associate { it.eventId to it.quantity },
            bookingCodes = myBookings.filter { it.bookingCode.isNotBlank() }.associate { it.eventId to it.bookingCode },
            endorsed = endorsed.await().decodeList<NewsIdRow>().orEmpty().map { it.newsId }.toSet(),
            savedNews = newsSaves.await().decodeList<NewsIdRow>().orEmpty().map { it.newsId }.toSet(),
            claimed = claims.await().decodeList<NewsIdRow>().orEmpty().map { it.newsId }.toSet(),
            votes = votes.await().decodeList<VoteRow>().orEmpty().associate { it.pollId to it.optionId },
            acked = acks.await().decodeList<AlertIdRow>().orEmpty().map { it.alertId }.toSet(),
        )
    }

    suspend fun profile(uid: String): ProfileRow? =
        sb.select("profiles", "select" to "full_name,phone,email,ward,photo_path", "id" to "eq.$uid")
            .decodeList<ProfileRow>()?.firstOrNull()

    suspend fun notifPrefs(uid: String): NotifRow? =
        sb.select("notification_prefs", "select" to "civic,events,emergency", "user_id" to "eq.$uid")
            .decodeList<NotifRow>()?.firstOrNull()

    // ------------------------------------------------------------------ Writing

    /** Files a report; uploads the photo first when there is one. Returns the photo's public address. */
    suspend fun submitIssue(
        id: String, cityId: String, uid: String, category: IssueCategory, title: String, description: String,
        urgency: Urgency, location: String, lat: Double, lng: Double, photo: ByteArray?, photoIsLive: Boolean,
    ): Res<String?> {
        var path: String? = null
        if (photo != null) {
            when (val up = sb.upload("issue-photos", "$uid/$id.jpg", photo)) {
                is Res.Ok -> path = up.value
                is Res.Err -> return up
            }
        }
        val row = buildJsonObject {
            put("id", id); put("city_id", cityId); put("reporter_id", uid)
            put("category", category.name.lowercase()); put("title", title.take(120))
            put("description", description.take(500)); put("urgency", urgency.name.lowercase())
            put("location_text", location); put("lat", lat); put("lng", lng)
            path?.let { put("photo_path", it) }; put("photo_is_live", photoIsLive)
        }
        return when (val r = sb.insert("civic_issues", row.toString())) {
            is Res.Ok -> Res.Ok(path?.let { sb.publicUrl("issue-photos", it) })
            is Res.Err -> r
        }
    }

    suspend fun addEvent(
        id: String, cityId: String, uid: String, title: String, category: EventCategory, venue: String, area: String,
        lat: Double, lng: Double, startsAtMs: Long, priceRupees: Int, description: String, photo: ByteArray?,
    ): Res<String?> {
        var path: String? = null
        if (photo != null) {
            when (val up = sb.upload("event-images", "$uid/$id.jpg", photo)) {
                is Res.Ok -> path = up.value
                is Res.Err -> return up
            }
        }
        val row = buildJsonObject {
            put("id", id); put("city_id", cityId); put("host_id", uid)
            put("title", title.take(120)); put("description", description.take(2000))
            put("category", category.name.lowercase()); put("venue", venue); put("area", area)
            put("lat", lat); put("lng", lng); put("starts_at", Times.iso(startsAtMs))
            put("price_rupees", priceRupees); path?.let { put("image_path", it) }
        }
        return when (val r = sb.insert("events", row.toString())) {
            is Res.Ok -> Res.Ok(path?.let { sb.publicUrl("event-images", it) })
            is Res.Err -> r
        }
    }

    suspend fun book(eventId: String, qty: Int): Res<BookingRow> {
        val r = sb.rpc("book_event", """{"p_event_id":${Supabase.q(eventId)},"p_quantity":$qty}""")
        if (r !is Res.Ok) return r as Res.Err
        return runCatching { Res.Ok(json.decodeFromString<BookingRow>(r.value)) as Res<BookingRow> }
            .getOrElse { Res.Err("Booked, but the confirmation couldn't be read. Pull to refresh.") }
    }

    /** Adds or removes a "mine" row such as an upvote, a save or an acknowledgement. */
    suspend fun setMine(table: String, keyColumn: String, key: String, uid: String, on: Boolean): Res<String> =
        if (on) sb.insert(table, """{${Supabase.q(keyColumn)}:${Supabase.q(key)},"user_id":${Supabase.q(uid)}}""", upsert = true)
        else sb.delete(table, keyColumn to "eq.$key", "user_id" to "eq.$uid")

    suspend fun vote(pollId: String, optionId: Long, uid: String): Res<String> =
        sb.insert("poll_votes", """{"poll_id":${Supabase.q(pollId)},"option_id":$optionId,"user_id":${Supabase.q(uid)}}""")

    suspend fun updateProfile(uid: String, name: String, ward: String, phone: String = "", email: String = ""): Res<String> {
        val digits = phone.filter { it.isDigit() }.takeLast(10)
        val body = buildJsonObject {
            put("full_name", name.trim().take(80).ifBlank { null })
            put("ward", ward.trim().take(60).ifBlank { null })
            put("phone", if (digits.length == 10) "+91$digits" else null)
            put("email", email.trim().lowercase().ifBlank { null })
        }
        return sb.update("profiles", body.toString(), "id" to "eq.$uid")
    }

    /** Uploads a new profile photo (or clears it when [photo] is null). Returns its public address. */
    suspend fun setAvatar(uid: String, photo: ByteArray?): Res<String?> {
        if (photo == null) {
            return when (val r = sb.update("profiles", """{"photo_path":null}""", "id" to "eq.$uid")) {
                is Res.Ok -> Res.Ok(null)
                is Res.Err -> r
            }
        }
        val path = "$uid/avatar-${System.currentTimeMillis()}.jpg"
        val up = sb.upload("avatars", path, photo)
        if (up is Res.Err) return up
        return when (val r = sb.update("profiles", """{"photo_path":${Supabase.q(path)}}""", "id" to "eq.$uid")) {
            is Res.Ok -> Res.Ok(sb.publicUrl("avatars", path))
            is Res.Err -> r
        }
    }

    suspend fun setNotif(uid: String, key: String, on: Boolean): Res<String> =
        sb.update("notification_prefs", """{${Supabase.q(key)}:$on}""", "user_id" to "eq.$uid")

    fun avatarUrl(path: String?) = path?.let { sb.publicUrl("avatars", it) }

    // ------------------------------------------------------------------ Mapping

    private fun IssueRow.toIssue(myId: String?, note: String?): CivicIssue = CivicIssue(
        id = id,
        title = title,
        category = when (category) {
            "pothole" -> IssueCategory.POTHOLE
            "streetlight" -> IssueCategory.STREETLIGHT
            "garbage" -> IssueCategory.GARBAGE
            else -> IssueCategory.WATER
        },
        location = locationText ?: ward ?: "",
        description = description,
        authorities = listOf(authorities?.xHandle ?: authorities?.shortName?.let { "@$it" } ?: "@SheharSaathi"),
        urgency = when (urgency) {
            "low" -> Urgency.LOW
            "critical" -> Urgency.CRITICAL
            else -> Urgency.MEDIUM
        },
        status = when (status) {
            "under_review" -> IssueStatus.UNDER_REVIEW
            "in_progress" -> IssueStatus.IN_PROGRESS
            "resolved", "rejected" -> IssueStatus.RESOLVED
            else -> IssueStatus.REPORTED
        },
        upvotes = upvoteCount,
        imageUri = photoPath?.let { sb.publicUrl("issue-photos", it) },
        reportedAgo = Times.ago(Times.parse(createdAt)),
        isMine = myId != null && reporterId == myId,
        lat = lat,
        lng = lng,
        authorityNote = note ?: if (status == "rejected") "Closed by the authority without action." else null,
    )

    private fun EventRow.toEvent(myId: String?, fromLat: Double, fromLng: Double): CityEvent {
        val cat = EventCategory.entries.firstOrNull { it.name.equals(category, ignoreCase = true) } ?: EventCategory.CULTURE
        val start = Times.parse(startsAt)
        val (dateLabel, timeLabel, bucket) = Times.eventLabels(start)
        return CityEvent(
            id = id,
            title = title,
            category = cat,
            dateLabel = dateLabel,
            timeLabel = timeLabel,
            venue = venue,
            area = area.orEmpty(),
            priceRupees = priceRupees,
            imageRes = fallbackImage(cat),
            interested = interestedCount,
            distanceKm = Transit.distanceKm(fromLat, fromLng, lat, lng),
            description = description,
            bucket = bucket,
            featured = featured,
            fastFilling = capacity != null && bookedCount * 5 >= capacity * 4,
            lat = lat,
            lng = lng,
            pinLabel = title.take(18),
            imageUri = imagePath?.let { sb.publicUrl("event-images", it) },
            hostedByYou = myId != null && hostId == myId,
        )
    }

    private fun NewsRow.toNews(): NewsItem {
        val k = when (kind) {
            "bulletin" -> NewsKind.BULLETIN
            "civic_win" -> NewsKind.CIVIC_WIN
            "offer" -> NewsKind.OFFER
            else -> NewsKind.STORY
        }
        return NewsItem(
            id = id,
            kind = k,
            tag = tag ?: when (k) {
                NewsKind.BULLETIN -> "BULLETIN"
                NewsKind.CIVIC_WIN -> "CIVIC WIN"
                NewsKind.OFFER -> "LOCAL OFFER"
                NewsKind.STORY -> "STORY"
            },
            ward = ward.orEmpty(),
            ago = Times.ago(Times.parse(publishedAt ?: createdAt)),
            title = title,
            body = body,
            imageRes = null,
            endorsements = endorsementCount,
            comments = 0,
            source = source,
            offerCode = offerCode,
        )
    }

    // ------------------------------------------------------------------ JSON

    private inline fun <reified T> Res<String>.decode(): Res<T> = when (this) {
        is Res.Ok -> runCatching { Res.Ok(json.decodeFromString<T>(value)) as Res<T> }.getOrElse { Res.Err("Unexpected reply from the server") }
        is Res.Err -> this
    }

    private inline fun <reified T> Res<String>.decodeList(): List<T>? =
        (this as? Res.Ok)?.let { runCatching { json.decodeFromString<List<T>>(it.value) }.getOrNull() }

    companion object {
        fun fallbackImage(c: EventCategory): Int = when (c) {
            EventCategory.MUSIC -> R.drawable.img_indie_fest
            EventCategory.WORKSHOP, EventCategory.ART -> R.drawable.img_pottery
            EventCategory.FLEA -> R.drawable.img_flea_market
            EventCategory.TECH -> R.drawable.img_tech_demo
            EventCategory.CULTURE -> R.drawable.img_park_reading
        }
    }
}
