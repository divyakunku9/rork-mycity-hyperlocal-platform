package com.sheharsaathi.app.data

/** A headline gathered from a publisher's site. Tapping it opens the full story there. */
data class WebStory(
    val id: Long,
    val url: String,
    val title: String,
    val summary: String?,
    val imageUrl: String?,
    val category: NewsTopic,
    val source: String,
    val siteUrl: String?,
    val publishedMs: Long,
)

/** Topics the server sorts city news into (see the refresh-news function). */
enum class NewsTopic(val key: String, val label: String) {
    CIVIC("civic", "Civic"),
    TRAFFIC("traffic", "Traffic & Transport"),
    WEATHER("weather", "Weather"),
    CRIME("crime", "Crime & Safety"),
    POLITICS("politics", "Politics"),
    BUSINESS("business", "Business & Tech"),
    EDUCATION("education", "Education"),
    HEALTH("health", "Health"),
    ENTERTAINMENT("entertainment", "Culture & Events"),
    SPORTS("sports", "Sports"),
    CITY("city", "City");

    companion object {
        fun of(key: String?): NewsTopic = entries.firstOrNull { it.key == key } ?: CITY
    }
}
