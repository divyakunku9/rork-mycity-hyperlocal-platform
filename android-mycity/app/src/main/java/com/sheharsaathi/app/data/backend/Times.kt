package com.sheharsaathi.app.data.backend

import com.sheharsaathi.app.data.DayBucket
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Date helpers that work on Android 7 (no java.time). All labels use India time. */
object Times {
    val IST: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")
    private val iso = Regex("""(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2})(?:\.(\d+))?)?(Z|[+-]\d{2}(?::?\d{2})?)?""")

    /** Parses timestamps such as "2026-11-06T18:41:50.298+00:00" or "...Z". Returns 0 if unreadable. */
    fun parse(s: String?): Long {
        val m = s?.let { iso.find(it) } ?: return 0L
        val g = m.groupValues
        val cal = Calendar.getInstance(UTC).apply {
            clear()
            set(g[1].toInt(), g[2].toInt() - 1, g[3].toInt(), g[4].toInt(), g[5].toInt(), g[6].ifEmpty { "0" }.toInt())
        }
        val millis = g[7].take(3).padEnd(3, '0').ifEmpty { "000" }.toInt()
        val offsetMin = when (val z = g[8]) {
            "", "Z" -> 0
            else -> {
                val sign = if (z.startsWith("-")) -1 else 1
                val digits = z.drop(1).replace(":", "")
                sign * (digits.take(2).toInt() * 60 + digits.drop(2).ifEmpty { "0" }.toInt())
            }
        }
        return cal.timeInMillis + millis - offsetMin * 60_000L
    }

    /** "2026-11-06T13:10:00Z" for sending to the database. */
    fun iso(ms: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = UTC }.format(Date(ms))

    fun ago(ms: Long, now: Long = System.currentTimeMillis()): String {
        if (ms <= 0) return ""
        val min = (now - ms) / 60_000
        return when {
            min < 1 -> "Just now"
            min < 60 -> "${min}m ago"
            min < 24 * 60 -> "${min / 60}h ago"
            dayDiff(now, ms) == 1 -> "Yesterday"
            min < 7 * 24 * 60 -> "${min / (24 * 60)}d ago"
            else -> fmt("d MMM", ms)
        }
    }

    /** Whole calendar days from [b] to [a] in India time. */
    fun dayDiff(a: Long, b: Long): Int = (dayIndex(a) - dayIndex(b)).toInt()

    private fun dayIndex(ms: Long): Long = (ms + IST.getOffset(ms)) / 86_400_000L

    /** Date label, time label and tab for an event that starts at [ms]. */
    fun eventLabels(ms: Long, now: Long = System.currentTimeMillis()): Triple<String, String, DayBucket> {
        val days = dayDiff(ms, now)
        val time = fmt("h:mm a", ms)
        return when (days) {
            0 -> Triple("Today", time, DayBucket.TODAY)
            1 -> Triple("Tomorrow", time, DayBucket.TOMORROW)
            else -> Triple(fmt("EEE, d MMM", ms), time, DayBucket.WEEKEND)
        }
    }

    fun endsLabel(ms: Long, now: Long = System.currentTimeMillis()): String {
        val days = dayDiff(ms, now)
        return when {
            ms < now -> "Closed"
            days == 0 -> "Ends today"
            days == 1 -> "Ends tomorrow"
            else -> "Ends in $days days"
        }
    }

    /**
     * The instant for a calendar day picked in a date picker (UTC midnight of that day)
     * at [hour]:[minute] India time.
     */
    fun istAt(dayUtcMillis: Long, hour: Int, minute: Int): Long {
        val u = Calendar.getInstance(UTC).apply { timeInMillis = dayUtcMillis }
        return Calendar.getInstance(IST).apply {
            clear()
            set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH), hour, minute, 0)
        }.timeInMillis
    }

    private fun fmt(pattern: String, ms: Long) =
        SimpleDateFormat(pattern, Locale.ENGLISH).apply { timeZone = IST }.format(Date(ms))
}
