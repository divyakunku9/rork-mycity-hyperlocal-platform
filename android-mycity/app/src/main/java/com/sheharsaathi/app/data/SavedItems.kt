package com.sheharsaathi.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * Things saved from outside Shehar Saathi's own database: places on the map, events listed on
 * ticketing sites, and headlines from news sites. (Events and posts hosted on Shehar Saathi are
 * saved on the server; see CityViewModel.toggleSaveEvent / toggleSaveNews.)
 *
 * Each save keeps a small snapshot (title, a line of detail, link or position) because listings
 * and headlines drop out of the live feeds after a while and must still show in the Profile.
 */
enum class SavedKind { PLACE, EVENT, STORY }

@Serializable
data class SavedItem(
    val kind: SavedKind,
    /** Identifies the item within its kind: rounded position for places, the feed id otherwise. */
    val key: String,
    val title: String,
    val subtitle: String = "",
    val url: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    /** When an event starts. */
    val whenMs: Long? = null,
    val savedAtMs: Long = 0,
)

object SavedItems {
    /** Oldest saves beyond this are dropped so the list can't grow without limit. */
    const val MAX = 200

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Two pins within about a metre of each other are the same place, whatever they are called. */
    fun placeKey(lat: Double, lng: Double): String = String.format(Locale.US, "%.5f,%.5f", lat, lng)

    fun ofPlace(p: Place, now: Long = System.currentTimeMillis()) = SavedItem(
        kind = SavedKind.PLACE,
        key = placeKey(p.lat, p.lng),
        title = p.name,
        // Drop notes that were only true at the time, like "0.4 km away" or a pending address lookup.
        subtitle = p.area.split(" • ").map { it.trim() }
            .filterNot { it.isBlank() || it.endsWith("km away") || it.startsWith("Finding address") }.joinToString(" • "),
        lat = p.lat, lng = p.lng, savedAtMs = now,
    )

    fun ofListed(e: ListedEvent, now: Long = System.currentTimeMillis()) = SavedItem(
        kind = SavedKind.EVENT,
        key = e.id.toString(),
        title = e.title,
        subtitle = listOfNotNull(ListedTime.whenLabel(e), e.venue?.takeIf { it.isNotBlank() }, "on ${e.source}").joinToString(" · "),
        url = e.url, lat = e.lat, lng = e.lng, whenMs = e.startMs, savedAtMs = now,
    )

    fun ofStory(s: WebStory, now: Long = System.currentTimeMillis()) = SavedItem(
        kind = SavedKind.STORY,
        key = s.id.toString(),
        title = s.title,
        subtitle = s.source,
        url = s.url, savedAtMs = now,
    )

    fun contains(list: List<SavedItem>, kind: SavedKind, key: String): Boolean = list.any { it.kind == kind && it.key == key }

    /** Adds [item] at the front, or removes it if it is already saved. */
    fun toggle(list: List<SavedItem>, item: SavedItem): List<SavedItem> =
        if (contains(list, item.kind, item.key)) list.filterNot { it.kind == item.kind && it.key == item.key }
        else (listOf(item) + list).take(MAX)

    fun encode(list: List<SavedItem>): String = json.encodeToString(ListSerializer(SavedItem.serializer()), list)

    /** Unreadable or missing text gives an empty list rather than an error. */
    fun decode(text: String?): List<SavedItem> =
        if (text.isNullOrBlank()) emptyList()
        else runCatching { json.decodeFromString(ListSerializer(SavedItem.serializer()), text) }.getOrDefault(emptyList())

    fun toPlace(item: SavedItem): Place? {
        val lat = item.lat ?: return null
        val lng = item.lng ?: return null
        return Place("saved-${item.key}", item.title, item.subtitle, lat, lng, PlaceKind.LANDMARK)
    }
}
