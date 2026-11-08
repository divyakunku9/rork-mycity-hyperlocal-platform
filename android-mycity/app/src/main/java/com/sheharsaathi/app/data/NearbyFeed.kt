package com.sheharsaathi.app.data

import com.sheharsaathi.app.data.backend.Times

/** One card in the Explore rows: an event, a listed event, a civic report or a news story. */
sealed class FeedItem {
    abstract val key: String
    abstract val text: String   // for the Explore search box

    data class Event(val e: CityEvent, val km: Double) : FeedItem() {
        override val key get() = "ev-${e.id}"
        override val text get() = "${e.title} ${e.venue} ${e.area} ${e.category.label}"
    }
    data class Listed(val e: ListedEvent, val km: Double?) : FeedItem() {
        override val key get() = "le-${e.id}"
        override val text get() = "${e.title} ${e.venue.orEmpty()} ${e.address.orEmpty()} ${e.category.label} ${e.source}"
    }
    data class Issue(val i: CivicIssue, val km: Double) : FeedItem() {
        override val key get() = "is-${i.id}"
        override val text get() = "${i.title} ${i.location} ${i.category.label}"
    }
    data class News(val s: WebStory, val local: Boolean) : FeedItem() {
        override val key get() = "nw-${s.id}"
        override val text get() = "${s.title} ${s.summary.orEmpty()} ${s.source}"
    }
}

/** The four Explore rows, plus the fuller "near you" lists behind "See all". */
data class NearbyFeed(
    val area: String,
    val happening: List<FeedItem>,
    val issues: List<FeedItem.Issue>,
    val events: List<FeedItem>,
    val news: List<FeedItem.News>,
    val eventsNear: List<FeedItem>,
    val issuesNear: List<FeedItem.Issue>,
    val newsNear: List<FeedItem.News>,
) {
    companion object {
        const val RADIUS_KM = 5.0
        private const val ISSUE_RADIUS_KM = 3.0

        private val GENERIC = setOf("hyderabad", "secunderabad", "telangana", "india", "city", "you are here", "current location", "dropped pin")

        /** Area names to look for in news: "Madhapur", "HITEC City"… from the user's spot. */
        fun areaWords(place: Place, cityName: String): List<String> =
            (listOf(place.name) + place.area.split(","))
                .map { it.replace(Regex("^Ward \\d+\\s*", RegexOption.IGNORE_CASE), "").replace(Regex("\\d+"), "").trim() }
                .filter { it.length >= 4 && it.lowercase() !in GENERIC && !it.equals(cityName, ignoreCase = true) && !it.contains("km") }
                .distinctBy { it.lowercase() }
                .take(3)

        fun mentions(text: String, words: List<String>): Boolean =
            words.any { w -> Regex("\\b${Regex.escape(w)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) }

        fun build(
            place: Place,
            cityName: String,
            /** The area name the app shows at the top ("HITEC City"), when known; better than a street name. */
            areaLabel: String?,
            events: List<CityEvent>,
            listed: List<ListedEvent>,
            issues: List<CivicIssue>,
            news: List<WebStory>,
            now: Long = System.currentTimeMillis(),
        ): NearbyFeed {
            val area = areaLabel?.substringBefore(",")?.trim()?.takeIf { it.isNotBlank() && !it.equals(cityName, true) }
                ?: place.name.substringBefore(",").trim()
            val words = (listOfNotNull(area.takeIf { it.length >= 4 }) + areaWords(place, cityName)).distinctBy { it.lowercase() }.take(4)
            val km = { lat: Double, lng: Double -> Transit.distanceKm(place.lat, place.lng, lat, lng) }

            // Events: what's on soonest first, nearer first on the same day.
            val ev = events.map { FeedItem.Event(it, km(it.lat, it.lng)) }
            val le = listed.filter { it.isUpcoming(now) }.map { e ->
                FeedItem.Listed(e, if (e.lat != null && e.lng != null) km(e.lat, e.lng) else null)
            }
            fun dayOf(f: FeedItem): Int = when (f) {
                is FeedItem.Event -> f.e.bucket.ordinal
                is FeedItem.Listed -> Times.dayDiff(maxOf(f.e.startMs, now), now)
                else -> 99
            }
            fun kmOf(f: FeedItem): Double = when (f) {
                is FeedItem.Event -> f.km
                is FeedItem.Listed -> f.km ?: 50.0
                is FeedItem.Issue -> f.km
                else -> 50.0
            }
            val allEvents: List<FeedItem> = (ev + le).sortedWith(compareBy({ dayOf(it) }, { kmOf(it) }))
            val eventsNear = allEvents.filter { kmOf(it) <= RADIUS_KM || (it is FeedItem.Listed && it.km == null && mentions(it.text, words)) }

            // Civic: open reports, nearest first.
            val open = issues.filter { it.status != IssueStatus.RESOLVED }.map { FeedItem.Issue(it, km(it.lat, it.lng)) }.sortedBy { it.km }
            val issuesNear = open.filter { it.km <= RADIUS_KM }

            // News: stories that name this area first, then the latest from the city.
            val tagged = news.map { FeedItem.News(it, mentions("${it.title} ${it.summary.orEmpty()}", words)) }
            val allNews = tagged.filter { it.local } + tagged.filterNot { it.local }
            val newsNear = tagged.filter { it.local && now - it.s.publishedMs < 4 * 86_400_000L }

            // Happening near: a mix — soonest nearby events, the closest open reports, local news.
            val hEvents = eventsNear.filter { dayOf(it) <= 7 }.take(6)
            val hIssues = open.filter { it.km <= ISSUE_RADIUS_KM }.take(3)
            val hNews = newsNear.take(3)
            // Round-robin: event, report, news, event… so each kind shows early in the row.
            val happening = buildList<FeedItem> {
                val e = hEvents.iterator(); val i = hIssues.iterator(); val n = hNews.iterator()
                while (e.hasNext() || i.hasNext() || n.hasNext()) {
                    if (e.hasNext()) add(e.next())
                    if (i.hasNext()) add(i.next())
                    if (n.hasNext()) add(n.next())
                    if (e.hasNext()) add(e.next())
                }
            }.take(12)

            return NearbyFeed(
                area = area,
                happening = happening,
                issues = open,
                events = allEvents,
                news = allNews,
                eventsNear = eventsNear,
                issuesNear = issuesNear,
                newsNear = newsNear,
            )
        }
    }
}
