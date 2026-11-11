package com.sheharsaathi.app.data

/**
 * Hyderabad's municipal wards after the February 2026 split into three corporations
 * (GHMC 150 wards, Cyberabad 76, Malkajgiri 74). The list lives in Supabase (civic_wards).
 */
data class Ward(
    val no: Int,
    val name: String,
    /** GHMC, CMC or MMC. */
    val corporation: String,
    val zone: String,
    val circle: String,
    val lat: Double?,
    val lng: Double?,
    /** Null while no council is elected (the case for all three corporations since Feb 2026). */
    val corporator: String? = null,
    val corporatorParty: String? = null,
    /** True when the centre is only roughly known (e.g. the circle's centre); not used for "nearest ward". */
    val approx: Boolean = false,
) {
    val label get() = "Ward $no · $name"
    /** What's saved in the profile and shown on the ward poll. */
    val profileLabel get() = "Ward $no, $name"
}

/** One of the three municipal corporations, and who runs it while there is no elected council. */
data class CivicBody(
    val code: String,
    val name: String,
    val headTitle: String,
    val headName: String?,
    val councilNote: String,
    val website: String?,
    val helplineDisplay: String?,
    val helplineDial: String?,
    val specialOfficer: String?,
    val asOf: String?,
) {
    /** "Cyberabad" from "Cyberabad Municipal Corporation"; GHMC stays "GHMC". */
    val shortName get() = if (code == "GHMC") "GHMC" else name.substringBefore(" Municipal").trim()
}

enum class WardSource { LOCATION, CHOSEN }

data class WardMatch(
    val ward: Ward,
    val source: WardSource,
    /** True when the area name itself matched the ward (stronger than "nearest ward centre"). */
    val byName: Boolean = false,
    val km: Double? = null,
)

object WardFinder {
    /** Beyond this from every ward centre, the spot is outside the three corporations. */
    private const val OUTSIDE_KM = 9.0
    /** A same-named ward further than this is a different place with the same name. */
    private const val NAME_KM = 5.0

    fun norm(s: String): String = s.lowercase()
        .replace("hitech", "hitec")
        .replace(Regex("\\(.*?\\)"), "")
        .replace(Regex("[^a-z]"), "")

    /** Names in the place worth comparing with ward names: the area first, then the place name. */
    fun placeNames(p: Place): List<String> =
        (p.area.split(",") + p.name)
            .map { it.replace(Regex("^\\s*Ward\\s*\\d+\\s*", RegexOption.IGNORE_CASE), "").trim() }
            .filter { it.length >= 3 && !it.equals("Hyderabad", true) && !it.equals("Secunderabad", true) }

    private fun aliases(w: Ward): Set<String> {
        val n = norm(w.name)
        return buildSet {
            add(n)
            if (n.endsWith("colony") && n.length > 9) add(n.removeSuffix("colony"))
            // "Ramachandrapuram (RC Puram)": the bracketed short name counts too.
            Regex("\\((.*?)\\)").find(w.name)?.groupValues?.get(1)?.let { add(norm(it)) }
        }
    }

    /** The ward for a spot: an area-name match nearby if there is one, else the nearest ward centre. */
    fun find(p: Place, wards: List<Ward>): WardMatch? {
        val located = wards.filter { it.lat != null && it.lng != null }
        if (located.isEmpty()) return null
        val dist = located.associateWith { Transit.distanceKm(p.lat, p.lng, it.lat!!, it.lng!!) }
        val names = placeNames(p).map(::norm).filter { it.isNotEmpty() }
        for (n in names) {
            located.filter { n in aliases(it) && dist.getValue(it) <= NAME_KM }
                .minByOrNull { dist.getValue(it) }
                ?.let { return WardMatch(it, WardSource.LOCATION, byName = true, km = dist.getValue(it)) }
        }
        // Rough centres would pull spots toward the circle middle, so only well-placed wards count here.
        val nearest = located.filter { !it.approx }.minByOrNull { dist.getValue(it) } ?: return null
        if (dist.getValue(nearest) > OUTSIDE_KM) return null
        return WardMatch(nearest, WardSource.LOCATION, byName = false, km = dist.getValue(nearest))
    }

    /** Ward search for the manual picker: by number, ward name, circle or zone. */
    fun search(q: String, wards: List<Ward>): List<Ward> {
        val t = q.trim()
        if (t.isEmpty()) return wards.sortedBy { it.no }
        t.removePrefix("ward").removePrefix("Ward").trim().toIntOrNull()?.let { n ->
            return wards.filter { it.no.toString().startsWith(n.toString()) }.sortedBy { it.no }
        }
        val k = norm(t)
        fun rank(w: Ward): Int = when {
            norm(w.name).startsWith(k) -> 0
            norm(w.name).contains(k) -> 1
            norm(w.circle).startsWith(k) -> 2
            norm(w.zone).startsWith(k) || norm(w.circle).contains(k) -> 3
            else -> 9
        }
        return wards.map { it to rank(it) }.filter { it.second < 9 }.sortedWith(compareBy({ it.second }, { it.first.no })).map { it.first }
    }
}
