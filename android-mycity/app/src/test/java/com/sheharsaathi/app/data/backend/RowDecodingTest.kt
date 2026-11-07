package com.sheharsaathi.app.data.backend

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Decodes replies captured from the live Supabase project. */
class RowDecodingTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    @Test fun issueWithEmbeddedAuthority() {
        val text = """[{"id":"aaaaaaaa-0000-4000-8000-000000000001","city_id":"hyderabad","reporter_id":"a617a897-67c4-4939-8a05-f0e1c37453f8",
            "category":"pothole","title":"Pothole reported • Madhapur","description":"Test","urgency":"critical","status":"reported",
            "location_text":"Madhapur, Hyderabad","ward":null,"lat":17.4483,"lng":78.3915,"photo_path":"a/b.jpg","photo_is_live":false,
            "authority_id":"ghmc","upvote_count":0,"created_at":"2026-11-06T19:31:36.34612+00:00","updated_at":"2026-11-06T19:31:36.34612+00:00",
            "resolved_at":null,"authorities":{"x_handle":"@GHMCOnline","short_name":"GHMC"}}]"""
        val rows = json.decodeFromString<List<IssueRow>>(text)
        assertEquals(1, rows.size)
        assertEquals("@GHMCOnline", rows[0].authorities?.xHandle)
        assertNull(rows[0].ward)
    }

    @Test fun webNewsWithEmbeddedSource() {
        val row = json.decodeFromString<List<WebNewsRow>>(
            """[{"id":7,"url":"https://example.com/a","title":"GHMC fills potholes","summary":null,"image_url":null,
               "category":"civic","published_at":"2026-11-07T10:37:33+00:00","news_sources":{"name":"The Hindu","site_url":"https://www.thehindu.com"}}]"""
        ).single()
        assertEquals("The Hindu", row.source?.name)
        assertNull(row.imageUrl)
        assertEquals(com.sheharsaathi.app.data.NewsTopic.CIVIC, com.sheharsaathi.app.data.NewsTopic.of(row.category))
        assertEquals(com.sheharsaathi.app.data.NewsTopic.CITY, com.sheharsaathi.app.data.NewsTopic.of("unknown"))
    }

    @Test fun bookingReply() {
        val text = """{"id":"f770cb5c-5908-4695-8400-370630be10cf","event_id":"bbbbbbbb-0000-4000-8000-000000000001",
            "user_id":"a617a897-67c4-4939-8a05-f0e1c37453f8","quantity":2,"status":"confirmed","booking_code":"MC-683D2A3D",
            "total_rupees":404,"created_at":"2026-11-06T19:31:36.34612+00:00"}"""
        val b = json.decodeFromString<BookingRow>(text)
        assertEquals(2, b.quantity); assertEquals(404, b.totalRupees)
    }

    @Test fun pollWithOptions() {
        val text = """[{"id":"33333333-3333-3333-3333-333333333333","question":"Next ward project","detail":null,"ward":"Ward 104",
            "ends_at":"2026-11-09T19:00:00+00:00","poll_options":[{"id":2,"label":"B","position":2,"vote_count":1},
            {"id":1,"label":"A","position":1,"vote_count":3}]}]"""
        val p = json.decodeFromString<List<PollRow>>(text).single()
        assertEquals(listOf(1L, 2L), p.options.sortedBy { it.position }.map { it.id })
    }

    @Test fun emptyListsAndMissingFields() {
        assertTrue(json.decodeFromString<List<NewsRow>>("[]").isEmpty())
        val e = json.decodeFromString<EventRow>(
            """{"id":"e","title":"T","category":"flea","venue":"V","lat":1.0,"lng":2.0,"starts_at":"2026-11-08T10:00:00+00:00"}"""
        )
        assertEquals(0, e.priceRupees); assertNull(e.capacity)
    }

    @Test fun jsonStringQuoting() {
        assertEquals("\"a\\\"b\"", Supabase.q("a\"b"))
    }
}
