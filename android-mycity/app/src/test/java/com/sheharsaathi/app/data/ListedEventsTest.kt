package com.sheharsaathi.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListedEventsTest {
    private fun ev(startMs: Long, endMs: Long? = null, hasTime: Boolean = true, priceMin: Double? = null, priceMax: Double? = null, free: Boolean? = null) =
        ListedEvent(1, "https://x", "Show", null, null, startMs, endMs, hasTime, "Venue", null, null, null, priceMin, priceMax, free,
            ListedCategory.MUSIC, "District", "Book")

    @Test fun priceLabels() {
        assertEquals("Free", ev(0, free = true).priceLabel)
        assertEquals("₹999 onwards", ev(0, priceMin = 999.0, priceMax = 4999.0).priceLabel)
        assertEquals("₹249", ev(0, priceMin = 249.0, priceMax = 249.0).priceLabel)
        assertEquals("Price on District", ev(0).priceLabel)
        assertEquals("Book on District", ev(0).actionLabel)
    }

    @Test fun upcomingIncludesRunningShows() {
        val now = 1_794_000_000_000L
        assertTrue(ev(now - 3_600_000L).isUpcoming(now))                      // started an hour ago, still on
        assertFalse(ev(now - 6 * 3_600_000L).isUpcoming(now))                 // timed show, long over
        assertTrue(ev(now - 3 * 86_400_000L, endMs = now + 86_400_000L).isUpcoming(now)) // multi-day festival
    }

    @Test fun whenFilters() {
        val now = System.currentTimeMillis()
        assertTrue(ListedTime.matches(ListedWhen.TODAY, ev(now + 3_600_000L), now) || ListedTime.matches(ListedWhen.TOMORROW, ev(now + 3_600_000L), now))
        assertTrue(ListedTime.matches(ListedWhen.WEEK, ev(now + 3 * 86_400_000L), now))
        assertFalse(ListedTime.matches(ListedWhen.WEEK, ev(now + 10 * 86_400_000L), now))
        assertEquals(ListedCategory.OTHER, ListedCategory.of("nope"))
    }
}
