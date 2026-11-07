package com.sheharsaathi.app

import com.sheharsaathi.app.data.DayBucket
import com.sheharsaathi.app.data.backend.Supabase
import com.sheharsaathi.app.data.backend.Times
import org.junit.Assert.assertEquals
import org.junit.Test

class TimesTest {
    private val base = Times.parse("2026-11-07T06:30:00Z") // 12:00 noon in India

    @Test fun parsesDatabaseTimestamps() {
        assertEquals(1794033000000L, base)
        assertEquals(base, Times.parse("2026-11-07T06:30:00+00:00"))
        assertEquals(base, Times.parse("2026-11-07T12:00:00+05:30"))
        assertEquals(base + 298, Times.parse("2026-11-07T06:30:00.298123+00:00"))
        assertEquals(0L, Times.parse("not a date"))
    }

    @Test fun formatsIsoForTheDatabase() = assertEquals("2026-11-07T06:30:00Z", Times.iso(base))

    @Test fun countsDaysInIndiaTime() {
        // 23:59 and 00:01 India time on consecutive days are one day apart.
        val lateNight = Times.parse("2026-11-07T18:29:00Z")
        val justAfterMidnight = Times.parse("2026-11-07T18:31:00Z")
        assertEquals(1, Times.dayDiff(justAfterMidnight, lateNight))
        assertEquals(0, Times.dayDiff(lateNight, base))
    }

    @Test fun labelsEvents() {
        assertEquals(DayBucket.TODAY, Times.eventLabels(base + 3_600_000, base).third)
        assertEquals("1:00 PM", Times.eventLabels(base + 3_600_000, base).second)
        assertEquals("Tomorrow", Times.eventLabels(base + 86_400_000, base).first)
        assertEquals(DayBucket.WEEKEND, Times.eventLabels(base + 3 * 86_400_000L, base).third)
    }

    @Test fun relativeTimes() {
        assertEquals("Just now", Times.ago(base, base + 10_000))
        assertEquals("5m ago", Times.ago(base, base + 5 * 60_000))
        assertEquals("3h ago", Times.ago(base, base + 3 * 3_600_000L))
        assertEquals("Yesterday", Times.ago(base, base + 26 * 3_600_000L))
    }

    @Test fun hostedEventTimeIsIndiaTime() {
        val pickedDay = Times.parse("2026-11-14T00:00:00Z") // date picker gives UTC midnight
        assertEquals(Times.parse("2026-11-14T19:30:00+05:30"), Times.istAt(pickedDay, 19, 30))
    }

    @Test fun pollEndsLabel() {
        assertEquals("Ends in 3 days", Times.endsLabel(base + 3 * 86_400_000L, base))
        assertEquals("Closed", Times.endsLabel(base - 1, base))
    }

    @Test fun jsonStringsAreEscaped() {
        assertEquals("\"a\\\"b\"", Supabase.q("a\"b"))
    }
}
