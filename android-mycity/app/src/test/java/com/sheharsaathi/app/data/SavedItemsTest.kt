package com.sheharsaathi.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedItemsTest {
    private val place = Place("p1", "Cyber Towers", "Landmark • 0.4 km away", 17.450401, 78.381002, PlaceKind.LANDMARK)
    private val story = WebStory(7, "https://example.com/story", "Metro adds late trains", null, null, NewsTopic.TRAFFIC, "City Daily", null, 1_000L)
    private val event = ListedEvent(
        7, "https://example.com/gig", "Indie night", null, null, 1_800_000_000_000L, null, true, "Hard Rock Cafe", null,
        17.42, 78.45, 499.0, null, false, ListedCategory.MUSIC, "District", "Book",
    )

    @Test fun savingAgainRemovesIt() {
        val once = SavedItems.toggle(emptyList(), SavedItems.ofPlace(place, 1))
        assertEquals(1, once.size)
        assertTrue(SavedItems.contains(once, SavedKind.PLACE, SavedItems.placeKey(place.lat, place.lng)))
        assertTrue(SavedItems.toggle(once, SavedItems.ofPlace(place, 2)).isEmpty())
    }

    @Test fun newestSaveComesFirst() {
        var list = SavedItems.toggle(emptyList(), SavedItems.ofStory(story, 1))
        list = SavedItems.toggle(list, SavedItems.ofListed(event, 2))
        assertEquals(listOf(SavedKind.EVENT, SavedKind.STORY), list.map { it.kind })
    }

    @Test fun aStoryAndAnEventWithTheSameIdAreSeparate() {
        var list = SavedItems.toggle(emptyList(), SavedItems.ofStory(story, 1))
        list = SavedItems.toggle(list, SavedItems.ofListed(event, 2))
        assertEquals(2, list.size)
        list = SavedItems.toggle(list, SavedItems.ofStory(story, 3))
        assertEquals(listOf(SavedKind.EVENT), list.map { it.kind })
    }

    @Test fun theSameSpotUnderAnotherNameIsOneSave() {
        val renamed = place.copy(id = "pin-9", name = "Dropped pin", area = "Finding address…")
        val list = SavedItems.toggle(SavedItems.toggle(emptyList(), SavedItems.ofPlace(place, 1)), SavedItems.ofPlace(renamed, 2))
        assertTrue(list.isEmpty())
    }

    @Test fun placeDetailDropsNotesThatGoStale() {
        assertEquals("Landmark", SavedItems.ofPlace(place, 1).subtitle)
        assertEquals("", SavedItems.ofPlace(place.copy(area = "0.4 km away"), 1).subtitle)
        assertEquals("", SavedItems.ofPlace(place.copy(area = "Finding address…"), 1).subtitle)
        assertEquals("Madhapur, Hyderabad", SavedItems.ofPlace(place.copy(area = "Madhapur, Hyderabad"), 1).subtitle)
    }

    @Test fun snapshotsKeepWhatTheProfileNeeds() {
        val e = SavedItems.ofListed(event, 5)
        assertEquals("https://example.com/gig", e.url)
        assertTrue(e.subtitle, e.subtitle.contains("Hard Rock Cafe") && e.subtitle.endsWith("on District"))
        val s = SavedItems.ofStory(story, 5)
        assertEquals("City Daily", s.subtitle)
        assertEquals("https://example.com/story", s.url)
        val p = SavedItems.toPlace(SavedItems.ofPlace(place, 5))!!
        assertEquals("Cyber Towers", p.name)
        assertEquals(place.lat, p.lat, 0.0)
    }

    @Test fun savedListSurvivesBeingStoredAndReadBack() {
        val list = listOf(SavedItems.ofPlace(place, 3), SavedItems.ofListed(event, 2), SavedItems.ofStory(story, 1))
        assertEquals(list, SavedItems.decode(SavedItems.encode(list)))
    }

    @Test fun missingOrDamagedStorageGivesAnEmptyList() {
        assertTrue(SavedItems.decode(null).isEmpty())
        assertTrue(SavedItems.decode("").isEmpty())
        assertTrue(SavedItems.decode("not json").isEmpty())
        assertTrue(SavedItems.decode("""[{"kind":"SOMETHING_NEW","key":"1","title":"x"}]""").isEmpty())
    }

    @Test fun theListIsCapped() {
        var list = emptyList<SavedItem>()
        repeat(SavedItems.MAX + 20) { i -> list = SavedItems.toggle(list, SavedItems.ofStory(story.copy(id = i.toLong()), i.toLong())) }
        assertEquals(SavedItems.MAX, list.size)
        assertEquals((SavedItems.MAX + 19).toString(), list.first().key)
        assertFalse(SavedItems.contains(list, SavedKind.STORY, "0"))
    }
}
