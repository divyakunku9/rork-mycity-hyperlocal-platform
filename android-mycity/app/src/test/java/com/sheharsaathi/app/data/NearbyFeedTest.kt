package com.sheharsaathi.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyFeedTest {
    private val madhapur = Place("p", "Madhapur", "Ward 107 Madhapur, Hyderabad", 17.4483, 78.3915, PlaceKind.CURRENT)
    private fun story(id: Long, title: String, ageH: Int = 2) =
        WebStory(id, "https://x/$id", title, null, null, NewsTopic.CITY, "Src", null, System.currentTimeMillis() - ageH * 3_600_000L)
    private fun listed(id: Long, lat: Double, lng: Double, inDays: Int) =
        ListedEvent(id, "https://x/e$id", "Show $id", null, null, System.currentTimeMillis() + inDays * 86_400_000L + 3_600_000L, null, true,
            "Venue", null, lat, lng, null, null, null, ListedCategory.MUSIC, "District", "Book")

    @Test fun areaWordsSkipCityAndWardNumbers() {
        assertEquals(listOf("Madhapur"), NearbyFeed.areaWords(madhapur, "Hyderabad"))
    }

    @Test fun newsThatNamesTheAreaComesFirst() {
        val f = NearbyFeed.build(madhapur, "Hyderabad", emptyList(), emptyList(), emptyList(),
            listOf(story(1, "Metro fares revised"), story(2, "Madhapur flyover to close for repairs")))
        assertEquals(2L, f.news.first().s.id)
        assertTrue(f.news.first().local)
        assertEquals(listOf(2L), f.newsNear.map { it.s.id })
    }

    @Test fun nearbyEventsAreWithinRadiusAndSoonestFirst() {
        val near = listed(1, 17.45, 78.39, inDays = 2)          // ~0.2 km
        val nearToday = listed(2, 17.44, 78.38, inDays = 0)     // ~1.5 km, today
        val far = listed(3, 17.36, 78.47, inDays = 0)           // Charminar side, ~13 km
        val f = NearbyFeed.build(madhapur, "Hyderabad", emptyList(), listOf(near, nearToday, far), emptyList(), emptyList())
        assertEquals(listOf(2L, 1L), f.eventsNear.map { (it as FeedItem.Listed).e.id })
        assertTrue(f.happening.any { it is FeedItem.Listed && it.e.id == 2L })
        assertFalse(f.happening.any { it is FeedItem.Listed && it.e.id == 3L })
        assertEquals(3, f.events.size)                          // the Events row still has the whole city
    }
}
