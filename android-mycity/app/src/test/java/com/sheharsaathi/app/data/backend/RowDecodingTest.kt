package com.sheharsaathi.app.data.backend

import com.sheharsaathi.app.data.DayBucket
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TimesTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private fun utcMs(y: Int, mo: Int, d: Int, h: Int, mi: Int, s: Int = 0) =
        Calendar.getInstance(utc).apply { clear(); set(y, mo - 1, d, h, mi, s) }.timeInMillis

    @Test fun parsesPostgresTimestamps() {
        val expected = utcMs(2026, 11, 6, 18, 41, 50) + 298
        assertEquals(expected, Times.parse("2026-11-06T18:41:50.298+00:00"))
        assertEquals(expected, Times.parse("2026-11-06T18:41:50.298Z"))
        assertEquals(expected, Times.parse("2026-11-07T00:11:50.298+05:30"))
        assertEquals(utcMs(2026, 11, 6, 18, 41, 50) + 346, Times.parse("2026-11-06T18:41:50.34612+00:00"))
        assertEquals(utcMs(2026, 11, 6, 18, 41), Times.parse("2026-11-06 18:41"))
        assertEquals(0L, Times.parse("not a date"))
        assertEquals(0L, Times.parse(null))
    }

    @Test fun isoRoundTrip() {
        val ms = utcMs(2026, 1, 2, 3, 4, 5)
        assertEquals("2026-01-02T03:04:05Z", Times.iso(ms))
        assertEquals(ms, Times.parse(Times.iso(ms)))
    }

    @Test fun dayDifferencesUseIndiaTime() {
        // 23:00 IST on 6 Nov vs 01:00 IST on 7 Nov: different days in India, same UTC day.
        val late = utcMs(2026, 11, 6, 17, 30)   // 23:00 IST
        val early = utcMs(2026, 11, 6, 19, 30)  // 01:00 IST next day
        assertEquals(1, Times.dayDiff(early, late))
        assertEquals(0, Times.dayDiff(late, late + 30 * 60_000))
    }

    @Test fun eventLabels() {
        val now = utcMs(2026, 11, 6, 6, 30)  // 12:00 IST Friday
        val (d1, t1, b1) = Times.eventLabels(utcMs(2026, 11, 6, 14, 30), now)  // 20:00 IST today
        assertEquals("Today", d1); assertEquals("8:00 PM", t1); assertEquals(DayBucket.TODAY, b1)
        val (d2, _, b2) = Times.eventLabels(utcMs(2026, 11, 7, 4, 30), now)
        assertEquals("Tomorrow", d2); assertEquals(DayBucket.TOMORROW, b2)
        val (d3, _, b3) = Times.eventLabels(utcMs(2026, 11, 8, 4, 30), now)
        assertEquals("Sun, 8 Nov", d3); assertEquals(DayBucket.WEEKEND, b3)
    }

    @Test fun istAtTurnsAPickedDayIntoIndiaTime() {
        val pickedDay = utcMs(2026, 11, 14, 0, 0) // date pickers give UTC midnight
        assertEquals(utcMs(2026, 11, 14, 13, 30), Times.istAt(pickedDay, 19, 0))
    }

    @Test fun agoAndEnds() {
        val now = utcMs(2026, 11, 6, 12, 0)
        assertEquals("Just now", Times.ago(now - 20_000, now))
        assertEquals("5m ago", Times.ago(now - 5 * 60_000, now))
        assertEquals("3h ago", Times.ago(now - 3 * 3_600_000, now))
        assertEquals("", Times.ago(0, now))
        assertEquals("Closed", Times.endsLabel(now - 1, now))
        assertEquals("Ends in 3 days", Times.endsLabel(now + 3 * 86_400_000L, now))
    }
}

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
