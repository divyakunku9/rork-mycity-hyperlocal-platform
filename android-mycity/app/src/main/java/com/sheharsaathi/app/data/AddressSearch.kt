package com.sheharsaathi.app.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** How exactly a search result pins down what was typed. */
enum class Precision(val label: String) { DOOR("Exact door"), BUILDING("Building"), STREET("Street"), AREA("Area") }

/**
 * Address and place search tuned for Indian addresses, on OpenStreetMap data (Photon).
 *
 * Understands GHMC-style door numbers ("8-2-293/82/A"), flat/plot/H.No prefixes, pin codes and
 * "opposite / near <landmark>" directions. Searches the whole text first, then steps back
 * (without the flat, the landmark, just the street, just the area) only when needed, and says
 * how precise each result is. Runs from the phone so every user searches from their own
 * connection (the free service limits heavy shared users).
 */
object AddressSearch {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client by lazy { HttpClient(Android) }
    private const val UA = "SheharSaathi-Android/1.0 (+https://sheharsaathi.com)"

    data class Parsed(
        val cleaned: String,
        val rest: String,
        val parts: List<String>,
        val door: String?,
        val units: List<String>,
        val landmark: String?,
        val locality: String?,
    )

    private val UNIT = Regex(
        """^\s*(?:flat|apt|apartment|room|shop|office|unit|suite|floor|block|tower|wing|h\.?\s*no|d\.?\s*no|door\s*no|house\s*no|plot(?:\s*no)?|site\s*no|survey\s*no|no)\.?\s*[:#.-]?\s*[\w/-]+\s*,?\s*""",
        RegexOption.IGNORE_CASE,
    )
    private val DOOR = Regex("""\b(\d{1,2}-\d{1,4}(?:-\d{1,5})?(?:/[\w-]+)*)\b|#\s*(\d+[\w/-]*)""", RegexOption.IGNORE_CASE)
    private val LANDMARK = Regex(
        """\b(?:opp(?:osite)?\.?|near|nr\.?|beside|behind|next\s+to|adjacent\s+to|in\s+front\s+of|above|below|besides)\s+(.+?)(?:,|$)""",
        RegexOption.IGNORE_CASE,
    )

    fun tidy(q: String): String = q.replace(Regex("\\s+"), " ")
        .replace(Regex("\\brd\\b\\.?", RegexOption.IGNORE_CASE), "road")
        .replace(Regex("\\bst\\b\\.?", RegexOption.IGNORE_CASE), "street")
        .replace(Regex("\\bx\\s*roads?\\b", RegexOption.IGNORE_CASE), "X Roads")
        .replace(Regex("\\broad\\s*no\\.?\\s*", RegexOption.IGNORE_CASE), "Road No ")
        .replace(Regex("\\bno\\.\\s*", RegexOption.IGNORE_CASE), "No ")
        .replace(Regex("\\bhyd\\b", RegexOption.IGNORE_CASE), "Hyderabad")
        .replace(Regex("\\bsec'?bad\\b", RegexOption.IGNORE_CASE), "Secunderabad")
        .trim()

    fun parse(raw: String): Parsed {
        var rest = tidy(raw)
        val units = mutableListOf<String>()
        // Peel "Flat 302,", "Plot 45,", "H.No 1-2-3," off the front (there can be a few).
        repeat(3) {
            val m = UNIT.find(rest) ?: return@repeat
            if (m.range.first != 0 || m.value.length == rest.length) return@repeat
            units += m.value.trimEnd(',', ' ')
            rest = rest.substring(m.value.length)
        }
        val dm = DOOR.find(units.joinToString(" ") + " " + rest)
        val door = dm?.let { it.groups[1]?.value ?: it.groups[2]?.value }
        if (door != null) rest = rest.replaceFirst(door, "").trimStart(' ', ',', '#', '-')
        val lm = LANDMARK.find(rest)
        val landmark = lm?.groups?.get(1)?.value?.trim()
        val withoutLandmark = if (lm != null) rest.replaceFirst(lm.value, "").trim(' ', ',') else rest
        val parts = withoutLandmark.split(",")
            .map { it.replace(Regex("\\b\\d{6}\\b"), "").replace(Regex("\\b(hyderabad|telangana|india)\\b", RegexOption.IGNORE_CASE), "").replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotEmpty() }
        val locality = when {
            parts.size > 1 -> parts.last()
            lm != null && parts.isNotEmpty() -> parts[0].replace(Regex("^(lane|street|road|gali|cross)\\s+", RegexOption.IGNORE_CASE), "").trim().ifEmpty { null }
            else -> null
        }
        return Parsed(tidy(raw), withoutLandmark, parts, door, units, landmark, locality)
    }

    /** How much of a typed door number matches a mapped one: "8-2-293/82/A" vs "8-2-293/82/A/1125" → 1.0. */
    fun doorMatch(typed: String, mapped: String?): Double {
        if (mapped.isNullOrBlank()) return 0.0
        val a = typed.lowercase().split('-', '/'); val b = mapped.lowercase().split('-', '/')
        var n = 0
        while (n < a.size && n < b.size && a[n] == b[n]) n++
        return n.toDouble() / a.size
    }

    private fun norm(s: String) = s.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    private fun km(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
        val r = { d: Double -> Math.toRadians(d) }
        val x = sin(r(bLat - aLat) / 2).pow(2) + cos(r(aLat)) * cos(r(bLat)) * sin(r(bLng - aLng) / 2).pow(2)
        return 12742 * asin(sqrt(x))
    }

    private class Hit(
        val place: Place, var precision: Precision, var note: String?, var score: Double,
        val house: String?,
    )

    private data class Try(val q: String, val weight: Double, val why: String)

    private suspend fun photon(q: String, city: City, biasLat: Double, biasLng: Double): List<JsonObject>? {
        if (q.isBlank() || q.length < 2) return emptyList()
        repeat(2) {
            val res: HttpResponse? = withTimeoutOrNull(8000) {
                runCatching {
                    client.get("https://photon.komoot.io/api/") {
                        header("User-Agent", UA)
                        parameter("q", q)
                        parameter("limit", 10)
                        parameter("lang", "en")
                        parameter("lat", biasLat); parameter("lon", biasLng)
                        parameter("bbox", "${city.west},${city.south},${city.east},${city.north}")
                    }
                }.getOrNull()
            }
            when {
                res == null -> delay(400)                                   // no connection / timeout: retry once
                res.status.value == 429 || res.status.value >= 500 -> delay(700) // busy: wait, retry once
                res.status.value != 200 -> return null
                else -> return runCatching {
                    json.parseToJsonElement(res.bodyAsText()).jsonObject["features"]?.jsonArray?.map { it.jsonObject }
                }.getOrNull() ?: emptyList()
            }
        }
        return null
    }

    private fun JsonObject.str(k: String): String? = (this[k] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun toHit(f: JsonObject, biasLat: Double, biasLng: Double, cityName: String): Hit? {
        val p = f["properties"]?.jsonObject ?: return null
        val coords = (f["geometry"]?.jsonObject?.get("coordinates") as? JsonArray) ?: return null
        val lng = coords.getOrNull(0)?.jsonPrimitive?.doubleOrNull ?: return null
        val lat = coords.getOrNull(1)?.jsonPrimitive?.doubleOrNull ?: return null
        val area = (p.str("district") ?: p.str("locality") ?: p.str("suburb") ?: p.str("city") ?: "").replace(Regex("^Ward \\d+\\s*", RegexOption.IGNORE_CASE), "")
        val type = p.str("type"); val key = p.str("osm_key"); val value = p.str("osm_value") ?: ""
        val isStreet = type == "street"
        val isArea = type in setOf("district", "locality", "city", "county", "state") || key == "place"
        val house = p.str("housenumber")
        val street = p.str("street")
        val name = p.str("name") ?: street?.let { s -> house?.let { "$it, $s" } ?: s } ?: area.ifBlank { return null }
        val sub = listOfNotNull(
            house.takeIf { p.str("name") != null },
            street?.takeIf { it != name },
            area.takeIf { it.isNotBlank() && it != name },
        ).joinToString(", ").ifBlank { cityName }
        val kind = when {
            Regex("station|subway|halt|bus_station").containsMatchIn(value) -> PlaceKind.METRO
            key in setOf("amenity", "shop", "tourism", "leisure", "office", "craft", "healthcare") -> PlaceKind.VENUE
            else -> PlaceKind.LANDMARK
        }
        val id = "osm-${p.str("osm_type") ?: "x"}${p.str("osm_id") ?: (lat * 1e6).toLong().toString()}"
        val precision = when { house != null -> Precision.BUILDING; isStreet -> Precision.STREET; isArea -> Precision.AREA; else -> Precision.BUILDING }
        return Hit(Place(id, name, sub, lat, lng, kind), precision, null, -km(biasLat, biasLng, lat, lng) * 0.02, house)
    }

    /** null means the map service couldn't be reached at all (so the caller can fall back). */
    suspend fun search(raw: String, city: City, biasLat: Double, biasLng: Double): List<Place>? = withContext(Dispatchers.IO) {
        val a = parse(raw)
        val tries = buildList {
            add(Try(a.cleaned, 3.0, "full"))
            if (a.door != null) add(Try(listOfNotNull(a.door, a.locality ?: a.parts.takeLast(2).joinToString(" ").ifBlank { null }).joinToString(" "), 3.0, "door"))
            if (a.landmark != null) add(Try(listOfNotNull(a.landmark, a.locality).joinToString(" "), 2.0, "landmark"))
            if (a.rest.isNotBlank() && norm(a.rest) != norm(a.cleaned)) add(Try(a.rest, 2.0, "no-unit"))
            if (a.parts.size > 2) add(Try(a.parts.take(2).joinToString(" "), 1.6, "first-two"))
            for (i in 1 until a.parts.size) add(Try(a.parts.drop(i).joinToString(", "), 1.5 - i * 0.2, "broader"))
        }.distinctBy { norm(it.q) }.filter { norm(it.q).isNotEmpty() }.toMutableList()

        val want = norm(a.parts.joinToString(" ")).split(" ").filter { it.length > 2 }
        val hits = mutableListOf<Hit>()
        var reached = false
        fun good() = hits.any { h ->
            val hay = norm("${h.place.name} ${h.place.area}")
            h.precision == Precision.DOOR || (want.isNotEmpty() && want.count { hay.contains(it) }.toDouble() / want.size >= 0.6) || (want.isEmpty() && hits.isNotEmpty())
        }
        fun take(t: Try, feats: List<JsonObject>) {
            feats.forEachIndexed { i, f ->
                val h = toHit(f, biasLat, biasLng, city.name) ?: return@forEachIndexed
                h.score += t.weight - i * 0.15
                val hay = norm("${h.place.name} ${h.place.area}")
                h.score += want.count { hay.contains(it) } * 0.4   // more of the typed words = better
                if (a.door != null) {
                    val m = doorMatch(a.door, h.house ?: h.place.name.takeIf { Regex("\\d+-\\d+").containsMatchIn(it) })
                    if (m == 1.0) { h.precision = Precision.DOOR; h.score += 6 }
                    else if (m >= 0.5) { h.score += 3 * m; h.note = "Door no. ${a.door} isn't mapped exactly; this is the nearest numbered building (${h.house ?: h.place.name})." }
                }
                if (t.why == "landmark" && a.landmark != null) h.note = "Near ${a.landmark}: the exact spot isn't named on the map. Long-press the map to drop a pin there."
                hits += h
            }
        }

        // First round: the whole text, plus the door number with its area.
        val first = tries.take(if (a.door != null) 2 else 1)
        tries.removeAll(first)
        coroutineScope { first.map { t -> async { t to photon(t.q, city, biasLat, biasLng) } }.awaitAll() }.forEach { (t, f) ->
            if (f != null) { reached = true; take(t, f) }
        }
        // Then step back, one request at a time, only until something clearly matches (4 requests at most).
        var calls = first.size
        while (tries.isNotEmpty() && calls < 4 && !good()) {
            val t = tries.removeAt(0)
            photon(t.q, city, biasLat, biasLng)?.let { reached = true; take(t, it) }
            calls++
        }
        if (!reached) return@withContext null

        // One entry per place: long roads are stored in pieces; keep the best piece.
        val best = LinkedHashMap<String, Hit>()
        for (h in hits) {
            val k = if (h.precision == Precision.STREET) "street|${norm(h.place.name)}|${norm(h.place.area)}"
            else "${norm(h.place.name)}|${(h.place.lat * 400).toInt()}|${(h.place.lng * 400).toInt()}"
            val cur = best[k]
            if (cur == null || h.score > cur.score) { if (cur != null && h.note == null) h.note = cur.note; best[k] = h }
        }
        val asked = a.door ?: a.units.joinToString(" ").ifBlank { null }
        best.values.sortedByDescending { it.score }.take(12).map { h ->
            val note = h.note ?: if (asked != null && h.precision != Precision.DOOR) when (h.precision) {
                Precision.STREET -> "$asked isn't on the map. This takes you to the street."
                Precision.AREA -> "$asked isn't on the map. This is the area."
                else -> "$asked isn't on the map. This is the building or nearest match."
            } else null
            h.place.copy(precision = h.precision, note = note)
        }
    }
}
