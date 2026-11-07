package com.sheharsaathi.app.data

import com.sheharsaathi.app.data.backend.Times
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * An event listed on a ticketing or listing site (District, Eventbrite, Meetup, AllEvents).
 * Booking happens on that site; the app only shows the listing and links out.
 * Separate from [CityEvent], which are events hosted through Shehar Saathi itself.
 */
data class ListedEvent(
    val id: Long,
    val url: String,
    val title: String,
    val summary: String?,
    val imageUrl: String?,
    val startMs: Long,
    val endMs: Long?,
    val hasTime: Boolean,
    val venue: String?,
    val address: String?,
    val lat: Double?,
    val lng: Double?,
    val priceMin: Double?,
    val priceMax: Double?,
    val isFree: Boolean?,
    val category: ListedCategory,
    val source: String,
    val bookLabel: String,   // "Book", "RSVP", "View"
) {
    /** "Book on District", "RSVP on Meetup", "View on AllEvents". */
    val actionLabel: String get() = "$bookLabel on $source"

    val priceLabel: String get() = when {
        isFree == true -> "Free"
        priceMin != null && priceMin > 0 -> "₹${priceMin.toInt()}" + if (priceMax != null && priceMax > priceMin) " onwards" else ""
        else -> "Price on ${source}"
    }

    /** Still running (multi-day shows, festivals) or starting later. */
    fun isUpcoming(now: Long = System.currentTimeMillis()): Boolean =
        (endMs ?: (startMs + if (hasTime) 4 * 3_600_000L else 86_400_000L)) >= now
}

enum class ListedCategory(val key: String, val label: String) {
    MUSIC("music", "Music"),
    COMEDY("comedy", "Comedy"),
    THEATRE("theatre", "Theatre & Film"),
    TECH("tech", "Tech & Business"),
    WORKSHOP("workshop", "Workshops"),
    FESTIVAL("festival", "Fests & Expos"),
    FOOD("food", "Food & Drink"),
    NIGHTLIFE("nightlife", "Nightlife"),
    SPORTS("sports", "Sports & Fitness"),
    ART("art", "Art & Exhibitions"),
    KIDS("kids", "Kids & Family"),
    SPIRITUAL("spiritual", "Spiritual"),
    OTHER("other", "More");

    companion object {
        fun of(key: String?): ListedCategory = entries.firstOrNull { it.key == key } ?: OTHER
    }
}

/** When-filters for the listings. */
enum class ListedWhen(val label: String) { ANY("Any day"), TODAY("Today"), TOMORROW("Tomorrow"), WEEKEND("This weekend"), WEEK("Next 7 days") }

object ListedTime {
    private fun f(p: String) = SimpleDateFormat(p, Locale.ENGLISH).apply { timeZone = Times.IST }
    private fun cal(ms: Long) = Calendar.getInstance(Times.IST, Locale.ENGLISH).apply { timeInMillis = ms }

    /** "Today", "Tomorrow", "Sat 14 Nov". */
    fun dayHeading(ms: Long, now: Long = System.currentTimeMillis()): String = when (Times.dayDiff(ms, now)) {
        0 -> "Today · ${f("EEE d MMM").format(Date(ms))}"
        1 -> "Tomorrow · ${f("EEE d MMM").format(Date(ms))}"
        else -> f("EEEE, d MMM").format(Date(ms))
    }

    /** "Sat 14 Nov · 7:00 PM", or just the date when the site gives no time. */
    fun whenLabel(e: ListedEvent): String {
        val d = f("EEE d MMM").format(Date(e.startMs))
        val t = if (e.hasTime) " · " + f("h:mm a").format(Date(e.startMs)) else ""
        val multi = e.endMs?.let { end -> if (Times.dayDiff(end, e.startMs) >= 1) " – " + f("d MMM").format(Date(end)) else "" } ?: ""
        return d + multi + t
    }

    fun dayKey(ms: Long): Long = (ms + Times.IST.getOffset(ms)) / 86_400_000L

    fun matches(w: ListedWhen, e: ListedEvent, now: Long = System.currentTimeMillis()): Boolean {
        val start = maxOf(e.startMs, now)            // a festival already running counts from today
        val diff = Times.dayDiff(start, now)
        return when (w) {
            ListedWhen.ANY -> true
            ListedWhen.TODAY -> diff == 0
            ListedWhen.TOMORROW -> diff == 1 || (e.startMs <= now + 86_400_000L && (e.endMs ?: 0) >= now + 86_400_000L)
            ListedWhen.WEEK -> diff in 0..6
            ListedWhen.WEEKEND -> {
                // The coming Saturday–Sunday (or today, if it is the weekend).
                val c = cal(now); val dow = c.get(Calendar.DAY_OF_WEEK)
                val toSat = when (dow) { Calendar.SATURDAY -> 0; Calendar.SUNDAY -> -1; else -> Calendar.SATURDAY - dow }
                val sat = toSat
                val endDiff = e.endMs?.let { Times.dayDiff(maxOf(it, start), now) } ?: diff
                diff <= sat + 1 && endDiff >= sat
            }
        }
    }
}
