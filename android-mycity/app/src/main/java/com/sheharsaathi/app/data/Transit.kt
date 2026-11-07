package com.sheharsaathi.app.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class LegMode { WALK, AUTO, METRO, BUS, CYCLE, CAB }

object LineColors {
    const val RED = 0xFFE2231A
    const val BLUE = 0xFF1F5FBF
    const val GREEN = 0xFF2E9E48
    const val YELLOW = 0xFFE8B10E
    const val BUS = 0xFFF58220
    const val AC_BUS = 0xFF0F766E
    const val MMTS = 0xFF0E7490
    const val AUTO = 0xFF14122B
    const val CYCLE = 0xFF00A99D
    const val WALK = 0xFF6B6890
    const val CAB = 0xFF14122B
}

data class Leg(
    val mode: LegMode,
    val minutes: Int,
    val title: String,
    val detail: String,
    val from: String,
    val to: String,
    val priceRupees: Int = 0,
    val lineLabel: String? = null,
    val lineColor: Long = LineColors.WALK,
    val stops: List<String> = emptyList(),
    val distanceM: Int = 0,
    val waitMin: Int = 0,
    val platform: String? = null,
    val towards: String? = null,
)

enum class RouteTab(val label: String) {
    BEST("City Turbo"), METRO("Metro & Feeder"), BUS("Bus"), RIDE("Auto & Cab"), CYCLE("Cycle & Walk")
}

data class RouteOption(
    val id: String,
    val tab: RouteTab,
    val badge: String?,
    val legs: List<Leg>,
    val priceLabel: String,
    val priceSub: String?,
    val departsInMin: Int,
    val tags: List<String>,
    val note: String?,
    val noteGood: Boolean,
    val rideApps: List<String> = emptyList(),
    val distanceKm: Double,
) {
    val totalMinutes: Int get() = legs.sumOf { it.minutes + it.waitMin }
    val walkMinutes: Int get() = legs.filter { it.mode == LegMode.WALK }.sumOf { it.minutes }
    val primaryMode: LegMode
        get() = legs.firstOrNull { it.mode == LegMode.METRO || it.mode == LegMode.BUS }?.mode
            ?: legs.maxByOrNull { it.minutes }?.mode ?: LegMode.WALK
}

enum class PlaceKind { CURRENT, HOME, WORK, METRO, LANDMARK, VENUE }

data class Place(
    val id: String,
    val name: String,
    val area: String,
    val lat: Double,
    val lng: Double,
    val kind: PlaceKind,
)

private data class Station(val name: String, val lat: Double, val lng: Double)
private data class MetroLine(val name: String, val short: String, val color: Long, val stations: List<Station>)

private data class MetroPlan(
    val segments: List<Triple<MetroLine, Int, Int>>, // line, board index, alight index
    val cost: Double,
)

object Transit {

    /** Default location until GPS or a manual pick replaces it. */
    private val hydCurrent = Place("gps", "Madhapur", "HITEC City, Hyderabad", 17.4483, 78.3915, PlaceKind.CURRENT)

    val currentLocation: Place
        get() = CurrentCity.city.let { if (it.isHyderabad) hydCurrent else it.defaultPlace }

    /** Planner presets for the current city. */
    val places: List<Place>
        get() = CurrentCity.city.let { c ->
            if (c.isHyderabad) hydPlaces else {
                val a = c.areas
                val home = a[1 % a.size].let { (n, ll) -> Place("home", "Home", n, ll.first, ll.second, PlaceKind.HOME) }
                val work = a[2 % a.size].let { (n, ll) -> Place("work", "Work", n, ll.first, ll.second, PlaceKind.WORK) }
                listOf(home, work) + c.landmarks + c.areaPlaces().take(4).map { it.copy(id = it.id + "-p") }
            }
        }

    /** Destination behind the one-tap GO button. */
    val quickGo: Place
        get() = if (CurrentCity.city.isHyderabad) hydPlaces.first { it.id == "ameerpet" } else CurrentCity.city.landmarks.first()

    private val hydPlaces = listOf(
        Place("home", "Home", "Kondapur", 17.4660, 78.3640, PlaceKind.HOME),
        Place("work", "Work", "Mindspace, Raidurg", 17.4410, 78.3810, PlaceKind.WORK),
        Place("ameerpet", "Ameerpet Metro (Interchange)", "Ameerpet", 17.4352, 78.4448, PlaceKind.METRO),
        Place("hitec", "HITEC City Metro", "Cyber Towers, Madhapur", 17.4474, 78.3815, PlaceKind.METRO),
        Place("charminar", "Charminar", "Old City", 17.3616, 78.4747, PlaceKind.LANDMARK),
        Place("secunderabad", "Secunderabad Junction", "Secunderabad", 17.4337, 78.5016, PlaceKind.METRO),
        Place("tankbund", "Hussain Sagar (Tank Bund)", "Tank Bund Rd", 17.4239, 78.4738, PlaceKind.LANDMARK),
        Place("golconda", "Golconda Fort", "Ibrahim Bagh", 17.3833, 78.4011, PlaceKind.LANDMARK),
        Place("airport", "Rajiv Gandhi Int'l Airport (RGIA)", "Shamshabad", 17.2403, 78.4294, PlaceKind.LANDMARK),
        Place("gachibowli", "Gachibowli Stadium", "Gachibowli", 17.4446, 78.3485, PlaceKind.LANDMARK),
        Place("banjara", "Banjara Hills Road No. 12", "Banjara Hills", 17.4130, 78.4390, PlaceKind.LANDMARK),
        Place("uppal", "Uppal Stadium", "Uppal", 17.4065, 78.5505, PlaceKind.VENUE),
        Place("lbnagar", "LB Nagar Metro", "LB Nagar", 17.3496, 78.5480, PlaceKind.METRO),
        Place("kphb", "KPHB Colony", "Kukatpally", 17.4935, 78.4015, PlaceKind.METRO),
        Place("shilparamam", "Shilparamam", "HITEC City", 17.4527, 78.3800, PlaceKind.VENUE),
        Place("inorbit", "Inorbit Mall", "Madhapur", 17.4346, 78.3867, PlaceKind.VENUE),
        Place("thub", "T-Hub", "Knowledge City, Raidurg", 17.4337, 78.3777, PlaceKind.VENUE),
        Place("paradise", "Paradise Biryani", "MG Rd, Secunderabad", 17.4434, 78.4870, PlaceKind.VENUE),
        Place("nampally", "Nampally (Hyderabad Deccan)", "Nampally", 17.3920, 78.4675, PlaceKind.METRO),
        Place("kbr", "KBR National Park", "Jubilee Hills", 17.4239, 78.4230, PlaceKind.LANDMARK),
    )

    val recentIds: List<String>
        get() = CurrentCity.city.let { c -> if (c.isHyderabad) listOf("ameerpet", "shilparamam", "charminar") else c.landmarks.take(2).map { it.id } }

    /** Neighbourhoods offered in the manual location picker. */
    val areas: List<Place> get() = CurrentCity.city.areaPlaces()

    fun place(id: String) = places.firstOrNull { it.id == id } ?: if (id == currentLocation.id) currentLocation else null

    private val red = MetroLine(
        "Red Line", "Red", LineColors.RED,
        listOf(
            Station("Miyapur", 17.4969, 78.3726),
            Station("JNTU College", 17.4985, 78.3888),
            Station("KPHB Colony", 17.4935, 78.4015),
            Station("Kukatpally", 17.4847, 78.4137),
            Station("Balanagar", 17.4775, 78.4240),
            Station("Moosapet", 17.4706, 78.4282),
            Station("Bharat Nagar", 17.4639, 78.4344),
            Station("Erragadda", 17.4571, 78.4372),
            Station("ESI Hospital", 17.4475, 78.4386),
            Station("SR Nagar", 17.4418, 78.4433),
            Station("Ameerpet", 17.4352, 78.4448),
            Station("Punjagutta", 17.4283, 78.4511),
            Station("Irrum Manzil", 17.4199, 78.4573),
            Station("Khairatabad", 17.4115, 78.4615),
            Station("Lakdikapul", 17.4041, 78.4651),
            Station("Assembly", 17.3999, 78.4709),
            Station("Nampally", 17.3913, 78.4720),
            Station("Gandhi Bhavan", 17.3864, 78.4789),
            Station("Osmania Medical College", 17.3812, 78.4843),
            Station("MG Bus Station", 17.3783, 78.4868),
            Station("Malakpet", 17.3762, 78.4980),
            Station("New Market", 17.3712, 78.5086),
            Station("Musarambagh", 17.3704, 78.5181),
            Station("Dilsukhnagar", 17.3687, 78.5262),
            Station("Chaitanyapuri", 17.3670, 78.5368),
            Station("Victoria Memorial", 17.3606, 78.5443),
            Station("LB Nagar", 17.3496, 78.5480),
        ),
    )

    private val blue = MetroLine(
        "Blue Line", "Blue", LineColors.BLUE,
        listOf(
            Station("Raidurg", 17.4427, 78.3771),
            Station("HITEC City", 17.4474, 78.3815),
            Station("Durgam Cheruvu", 17.4418, 78.3877),
            Station("Madhapur", 17.4373, 78.3956),
            Station("Peddamma Gudi", 17.4304, 78.4086),
            Station("Jubilee Hills Check Post", 17.4291, 78.4148),
            Station("Jubilee Hills Road No. 5", 17.4289, 78.4213),
            Station("Yusufguda", 17.4340, 78.4265),
            Station("Madhura Nagar", 17.4378, 78.4383),
            Station("Ameerpet", 17.4352, 78.4448),
            Station("Begumpet", 17.4372, 78.4573),
            Station("Prakash Nagar", 17.4436, 78.4697),
            Station("Rasoolpura", 17.4446, 78.4778),
            Station("Paradise", 17.4434, 78.4870),
            Station("Parade Ground", 17.4446, 78.4983),
            Station("Secunderabad East", 17.4363, 78.5047),
            Station("Mettuguda", 17.4343, 78.5195),
            Station("Tarnaka", 17.4276, 78.5370),
            Station("Habsiguda", 17.4182, 78.5440),
            Station("NGRI", 17.4120, 78.5519),
            Station("Stadium", 17.4041, 78.5590),
            Station("Uppal", 17.3995, 78.5648),
            Station("Nagole", 17.3901, 78.5600),
        ),
    )

    private val green = MetroLine(
        "Green Line", "Green", LineColors.GREEN,
        listOf(
            Station("Parade Ground", 17.4446, 78.4983),
            Station("Secunderabad West", 17.4362, 78.4975),
            Station("Gandhi Hospital", 17.4250, 78.5018),
            Station("Musheerabad", 17.4155, 78.4995),
            Station("RTC X Roads", 17.4057, 78.4965),
            Station("Chikkadpally", 17.3995, 78.4945),
            Station("Narayanguda", 17.3908, 78.4884),
            Station("Sultan Bazaar", 17.3842, 78.4855),
            Station("MG Bus Station", 17.3783, 78.4868),
        ),
    )

    private val lines = listOf(red, blue, green)

    fun distanceKm(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(bLat - aLat)
        val dLng = Math.toRadians(bLng - aLng)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(aLat)) * cos(Math.toRadians(bLat)) * sin(dLng / 2) * sin(dLng / 2)
        return r * 2 * atan2(sqrt(h), sqrt(1 - h))
    }

    private fun d(a: Place, s: Station) = distanceKm(a.lat, a.lng, s.lat, s.lng)
    private fun ds(a: Station, b: Station) = distanceKm(a.lat, a.lng, b.lat, b.lng)

    private fun round5(v: Double) = (v / 5.0).roundToInt() * 5

    /** Hyderabad auto: ₹30 for the first 1.6 km, then about ₹16/km. */
    fun autoFare(km: Double): Int = max(30, (30 + max(0.0, km - 1.6) * 16).roundToInt())

    /** Hyderabad Metro fare slabs (₹12 – ₹75) by travelled distance. */
    fun metroFare(km: Double): Int = when {
        km <= 2 -> 12
        km <= 4 -> 18
        km <= 6 -> 30
        km <= 9 -> 40
        km <= 12 -> 50
        km <= 15 -> 55
        km <= 18 -> 60
        km <= 21 -> 66
        km <= 24 -> 70
        else -> 75
    }

    /** Road distance with a Hyderabad detour factor. */
    fun roadKm(from: Place, to: Place) = distanceKm(from.lat, from.lng, to.lat, to.lng) * 1.35

    private fun short(p: Place) = p.name.substringBefore(" (").substringBefore(",")

    fun plan(from: Place, to: Place, rainSafe: Boolean, stepFree: Boolean): List<RouteOption> {
        val km = roadKm(from, to)
        if (km < 0.15) return emptyList()
        val out = mutableListOf<RouteOption>()
        val fromShort = short(from)
        val toShort = short(to)

        val city = CurrentCity.city
        val hyd = city.isHyderabad
        // 1. Metro + feeder (Hyderabad's full station model)
        if (hyd) metroOption(from, to, km)?.let { out += it }

        // 2. Direct auto
        val autoMin = (km * 3.2 + 4).roundToInt()
        val autoLow = autoFare(km)
        out += RouteOption(
            id = "auto", tab = RouteTab.RIDE, badge = null,
            legs = listOf(
                Leg(LegMode.AUTO, autoMin, "Direct auto", "Pickup at $fromShort", from.name, to.name,
                    priceRupees = autoLow + 10, lineColor = LineColors.AUTO, distanceM = (km * 1000).roundToInt()),
            ),
            priceLabel = "₹$autoLow–${autoLow + 25}", priceSub = "Direct auto", departsInMin = 2,
            tags = listOf("Door-to-door"),
            note = if (km > 4) "Heavy traffic near ${if (hyd) "Jubilee Hills Check Post" else city.flavor.busyRoad} • allow extra time" else "Light traffic on local roads",
            noteGood = km <= 4, rideApps = listOf("Rapido", "Uber Auto", "Ola"), distanceKm = km,
        )

        // 3. City bus
        val op = city.busOperator
        val airport = to.id.endsWith("airport") || (hyd && to.lat < 17.27)
        val busCode = if (!hyd) {
            val n = 100 + ((abs(to.lat * 1000).roundToInt() + abs(to.lng * 1000).roundToInt()) % 400)
            if (airport) "AIR-$n" else "$n"
        } else when {
            airport -> "Pushpak"
            to.lng > 78.49 -> "10H"
            to.lat < 17.385 -> "127K"
            to.lat > 17.47 -> "216K"
            else -> "47L"
        }
        val ac = airport || busCode == "10H" || (!hyd && busCode.last().digitToIntOrNull()?.let { it % 3 == 0 } == true)
        val busRide = (km * 4.2 + 6).roundToInt()
        val busFare = if (airport) 300 else min(if (ac) 80 else 45, max(10, round5(10 + km * 2.0) * (if (ac) 2 else 1)))
        val busStops = max(2, (km / 0.8).roundToInt())
        val boardStop = "$fromShort Bus Stop"
        out += RouteOption(
            id = "bus", tab = RouteTab.BUS, badge = null,
            legs = listOf(
                Leg(LegMode.WALK, 4, "Walk to stop", "To $boardStop", from.name, boardStop, distanceM = 300),
                Leg(LegMode.BUS, busRide, "$op $busCode",
                    "$busStops stops • ${if (airport) "AC airport service" else if (ac) "AC bus" else "City ordinary"}",
                    boardStop, toShort, priceRupees = busFare, lineLabel = busCode,
                    lineColor = if (ac) LineColors.AC_BUS else LineColors.BUS, waitMin = 5,
                    stops = (if (hyd) listOf(fromShort, "Cyber Towers", "Jubilee Hills Check Post", toShort)
                        else listOf(fromShort, midArea(from, to), toShort)).distinct().take(min(4, busStops + 1)),
                    towards = to.area),
                Leg(LegMode.WALK, 3, "Walk to destination", "Short walk to ${to.name}", "Stop", to.name, distanceM = 220),
            ),
            priceLabel = "₹$busFare", priceSub = if (ac) "AC" else "Non-AC", departsInMin = 5,
            tags = listOf("${85 - (busCode.length * 3)}% seats free"),
            note = "Live GPS bus ${if (hyd) "TS09Z" else "#"}${4100 + busCode.length * 37} • 2 stops away", noteGood = true, distanceKm = km,
        )

        // 4. Cab
        val cabMin = (km * 3.0 + 5).roundToInt()
        val cabLow = (100 + km * 18).roundToInt()
        out += RouteOption(
            id = "cab", tab = RouteTab.RIDE, badge = null,
            legs = listOf(
                Leg(LegMode.CAB, cabMin, "Cab", "AC sedan • pickup in 5 min", from.name, to.name,
                    priceRupees = cabLow + 20, lineColor = LineColors.CAB, waitMin = 5, distanceM = (km * 1000).roundToInt()),
            ),
            priceLabel = "₹$cabLow–${cabLow + 45}", priceSub = "AC cab", departsInMin = 5,
            tags = listOf("Covered", "AC"), note = "Surge 1.0x • No price hike right now", noteGood = true,
            rideApps = listOf("Uber", "Ola", "Rapido Cab"), distanceKm = km,
        )

        // 5. Bike taxi
        if (km < 14 && !rainSafe && !stepFree) {
            val bike = (km * 2.6 + 3).roundToInt()
            val fare = max(25, (20 + km * 7).roundToInt())
            out += RouteOption(
                id = "cycle", tab = RouteTab.CYCLE, badge = null,
                legs = listOf(
                    Leg(LegMode.CYCLE, bike, "Bike taxi", "Pickup at $fromShort • helmet provided", from.name, to.name,
                        priceRupees = fare, lineLabel = "Bike", lineColor = LineColors.CYCLE, waitMin = 3,
                        distanceM = (km * 1000).roundToInt()),
                ),
                priceLabel = "₹$fare", priceSub = "Bike taxi", departsInMin = 3,
                tags = listOf("Beats traffic"), note = "8 bike captains nearby", noteGood = true,
                rideApps = listOf("Rapido", "Uber Moto"), distanceKm = km,
            )
        }

        // 6. Walk
        if (km < 3.2 && !rainSafe) {
            val walk = (km * 12.5).roundToInt()
            out += RouteOption(
                id = "walk", tab = RouteTab.CYCLE, badge = null,
                legs = listOf(Leg(LegMode.WALK, walk, "Walk", "Via main road footpath", from.name, to.name, distanceM = (km * 1000).roundToInt())),
                priceLabel = "Free", priceSub = "${walk * 4} kcal", departsInMin = 0,
                tags = listOf("Zero emission"), note = "Carry water • stay on shaded footpaths", noteGood = true, distanceKm = km,
            )
        }

        val fastest = out.minByOrNull { it.totalMinutes }?.id
        val cheapestPaid = out.filter { it.priceLabel != "Free" }.minByOrNull { firstPrice(it) }?.id
        return out.map { r ->
            val badge = when {
                r.id == "metro" -> "Best multimodal combo"
                r.id == fastest -> "Fastest door-to-door"
                r.id == "bus" && r.id == cheapestPaid -> "Cheapest • ${city.busOperator} Bus"
                r.id == "bus" -> "Direct bus"
                r.id == "cycle" -> "Quickest in traffic"
                r.id == "cab" -> "Most comfortable"
                r.id == "walk" -> "Walk it"
                else -> null
            }
            r.copy(badge = badge)
        }.sortedBy { score(it, rainSafe) }
    }

    private fun midArea(from: Place, to: Place): String {
        val mLat = (from.lat + to.lat) / 2
        val mLng = (from.lng + to.lng) / 2
        return CurrentCity.city.areas.minByOrNull { (_, ll) -> distanceKm(mLat, mLng, ll.first, ll.second) }?.first ?: "City Centre"
    }

    private fun firstPrice(r: RouteOption) =
        r.priceLabel.removePrefix("₹").substringBefore("–").toIntOrNull() ?: 0

    private fun score(r: RouteOption, rainSafe: Boolean): Double {
        val base = r.totalMinutes + firstPrice(r) / 8.0 + r.walkMinutes * (if (rainSafe) 2.0 else 0.4)
        return if (r.id == "metro") base - 6 else base
    }

    private fun nearest(line: MetroLine, p: Place) = line.stations.indices.minBy { d(p, line.stations[it]) }

    private fun bestMetroPlan(from: Place, to: Place): MetroPlan? {
        val plans = mutableListOf<MetroPlan>()
        for (line in lines) {
            val bi = nearest(line, from)
            val ai = nearest(line, to)
            if (bi == ai) continue
            val cost = d(from, line.stations[bi]) * 2.5 + d(to, line.stations[ai]) * 3 + abs(bi - ai) * 0.4
            plans += MetroPlan(listOf(Triple(line, bi, ai)), cost)
        }
        for (a in lines) for (b in lines) {
            if (a === b) continue
            val bi = nearest(a, from)
            val ai = nearest(b, to)
            val xA = a.stations.indexOfFirst { s -> b.stations.any { it.name == s.name } }
            if (xA < 0) continue
            val xB = b.stations.indexOfFirst { it.name == a.stations[xA].name }
            if (bi == xA || ai == xB) continue
            val cost = d(from, a.stations[bi]) * 2.5 + d(to, b.stations[ai]) * 3 +
                (abs(bi - xA) + abs(ai - xB)) * 0.4 + 2.5
            plans += MetroPlan(listOf(Triple(a, bi, xA), Triple(b, xB, ai)), cost)
        }
        return plans.minByOrNull { it.cost }
    }

    private fun metroOption(from: Place, to: Place, km: Double): RouteOption? {
        val plan = bestMetroPlan(from, to) ?: return null
        val first = plan.segments.first()
        val last = plan.segments.last()
        val board = first.first.stations[first.second]
        val alight = last.first.stations[last.third]
        val feederKm = d(from, board) * 1.35
        val lastKm = d(to, alight) * 1.35
        if (feederKm + lastKm > km * 0.75) return null

        val legs = mutableListOf<Leg>()
        var cost = 0
        if (feederKm > 0.9) {
            val fare = autoFare(feederKm)
            cost += fare
            legs += Leg(LegMode.AUTO, (feederKm * 3.0 + 2).roundToInt(), "Rapido / Auto feeder",
                "Drop-off at ${board.name} Station", from.name, "${board.name} Metro", priceRupees = fare,
                lineColor = LineColors.AUTO, distanceM = (feederKm * 1000).roundToInt())
        } else {
            legs += Leg(LegMode.WALK, (feederKm * 12.5).roundToInt().coerceAtLeast(2), "Walk to ${board.name}",
                "Take the skywalk to the concourse", from.name, "${board.name} Metro", distanceM = (feederKm * 1000).roundToInt())
        }

        var metroKm = 0.0
        val lineNames = mutableListOf<String>()
        plan.segments.forEachIndexed { idx, (line, bi, ai) ->
            val stops = if (bi < ai) line.stations.subList(bi, ai + 1) else line.stations.subList(ai, bi + 1).reversed()
            stops.zipWithNext().forEach { (x, y) -> metroKm += ds(x, y) }
            val towards = if (bi < ai) line.stations.last().name else line.stations.first().name
            val count = stops.size - 1
            lineNames += line.name
            legs += Leg(LegMode.METRO, count * 2 + 2, "Hyderabad Metro", "Toward $towards",
                stops.first().name, stops.last().name, lineLabel = line.short, lineColor = line.color,
                stops = stops.map { it.name }, waitMin = if (idx == 0) 3 else 4,
                platform = if (idx > 0) "Change here • Platform ${if (bi < ai) 1 else 2}" else if (bi < ai) "Platform 1" else "Platform 2",
                towards = towards)
        }
        val fare = metroFare(metroKm * 1.1)
        cost += fare
        val firstMetro = legs.indexOfFirst { it.mode == LegMode.METRO }
        legs[firstMetro] = legs[firstMetro].copy(priceRupees = fare)

        if (lastKm > 1.3) {
            val f = autoFare(lastKm)
            cost += f
            legs += Leg(LegMode.AUTO, (lastKm * 3.0 + 2).roundToInt(), "Auto last-mile", "From ${alight.name} exit",
                "${alight.name} Metro", to.name, priceRupees = f, lineColor = LineColors.AUTO,
                distanceM = (lastKm * 1000).roundToInt())
        } else {
            legs += Leg(LegMode.WALK, (lastKm * 12.5).roundToInt().coerceAtLeast(2), "Walk",
                "Exit towards ${short(to)}", "${alight.name} Metro", to.name,
                distanceM = (lastKm * 1000).roundToInt().coerceAtLeast(80))
        }
        val changeTag = if (plan.segments.size > 1) "1 change at ${plan.segments[1].first.stations[plan.segments[1].second].name}" else "No changes"
        return RouteOption(
            id = "metro", tab = RouteTab.METRO, badge = null, legs = legs,
            priceLabel = "₹$cost", priceSub = "Total", departsInMin = 0,
            tags = listOf(changeTag, "-${(km * 110).roundToInt()}g CO₂"),
            note = "${lineNames.joinToString(" + ")} running on time", noteGood = true, distanceKm = km,
        )
    }

    fun clockIn(minutes: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.MINUTE, minutes)
        return SimpleDateFormat("h:mm a", Locale.US).format(c.time)
    }
}

// ---------- Live board ----------

enum class BoardKind {
    METRO, BUS, RAIL;

    val label: String
        get() = when (this) {
            METRO -> "Metro Lines"
            BUS -> "${CurrentCity.city.busOperator} Buses"
            RAIL -> LiveBoard.railName(CurrentCity.city) ?: "Rail"
        }
}

data class Departure(
    val id: String,
    val kind: BoardKind,
    val code: String,
    val name: String,
    val towards: String,
    val color: Long,
    val baseSeconds: Int,
    val headwayMin: Int,
    val status: String,
    val statusGood: Boolean,
    val meta: List<String>,
    val crowd: String,
)

data class ServiceStatus(val title: String, val tag: String, val body: String, val level: Int, val color: Long)

object LiveBoard {
    private val hydStatus = listOf(
        ServiceStatus("Red Line • Minor delay", "+5 min", "Signalling check between Ameerpet and Punjagutta. Normal service resuming shortly.", 1, LineColors.RED),
        ServiceStatus("Blue Line • Good service", "Every 5 min", "Peak frequency between Raidurg and Nagole. First 6:00 AM • last 11:00 PM.", 0, LineColors.BLUE),
        ServiceStatus("TGSRTC Bus Network Alert", "10H • 127K", "Biodiversity flyover lane closure. Expect +10–15 min around Gachibowli.", 2, LineColors.BUS),
    )

    private val hydDepartures = listOf(
        Departure("b-ng", BoardKind.METRO, "B", "Blue Line", "Toward Nagole", LineColors.BLUE, 130, 5, "On approach", true, listOf("Madhapur • Platform 1", "AC train"), "Low crowd"),
        Departure("b-rd", BoardKind.METRO, "B", "Blue Line", "Toward Raidurg", LineColors.BLUE, 260, 5, "At Peddamma Gudi", true, listOf("Platform 2"), "Med crowd"),
        Departure("r-lb", BoardKind.METRO, "R", "Red Line", "Toward LB Nagar", LineColors.RED, 420, 5, "Delayed +5", false, listOf("Ameerpet interchange"), "Crowded"),
        Departure("g-mg", BoardKind.METRO, "G", "Green Line", "Toward MG Bus Station", LineColors.GREEN, 640, 8, "Departing Parade Ground", true, listOf("Parade Ground interchange"), "Low crowd"),
        Departure("bus-10h", BoardKind.BUS, "10H", "TGSRTC 10H", "Kondapur → Secunderabad via HITEC City", LineColors.AC_BUS, 300, 12, "Live GPS", true, listOf("TS09Z4172", "Electric AC", "Accessible"), "Med crowd"),
        Departure("bus-127k", BoardKind.BUS, "127K", "TGSRTC 127K", "Kondapur → Koti via Jubilee Hills", LineColors.BUS, 560, 15, "Diverted", false, listOf("Metro Express"), "Crowded"),
        Departure("bus-psh", BoardKind.BUS, "AIR", "Pushpak AC", "HITEC City → RGIA Airport", LineColors.AC_BUS, 900, 30, "Scheduled", true, listOf("Luggage space", "₹300"), "Low crowd"),
        Departure("mmts-lf", BoardKind.RAIL, "M", "MMTS", "Lingampally → Falaknuma", LineColors.MMTS, 840, 30, "On time", true, listOf("Hitech City • Platform 1"), "Low crowd"),
        Departure("mmts-ln", BoardKind.RAIL, "M", "MMTS", "Lingampally → Hyderabad (Nampally)", LineColors.MMTS, 1620, 40, "On time", true, listOf("Platform 2"), "Med crowd"),
    )

    /** Suburban rail brand per city, or null where there is no city rail service. */
    fun railName(c: City): String? = when (c.id) {
        "hyderabad" -> "MMTS Rail"
        "mumbai" -> "Mumbai Local"
        "chennai" -> "Suburban & MRTS"
        "kolkata" -> "Suburban Rail"
        "pune" -> "Pune–Lonavala Local"
        else -> null
    }

    private fun railRoutes(c: City): List<Pair<String, String>> = when (c.id) {
        "mumbai" -> listOf("Western • Churchgate → Virar Fast" to "Bandra • Platform 3", "Central • CSMT → Kalyan Slow" to "Dadar • Platform 1")
        "chennai" -> listOf("Beach → Tambaram" to "Guindy • Platform 2", "MRTS • Beach → Velachery" to "Thiruvanmiyur • Platform 1")
        "kolkata" -> listOf("Sealdah → Barrackpore" to "Dum Dum • Platform 3", "Howrah → Bandel" to "Howrah • Platform 9")
        "pune" -> listOf("Pune → Lonavala" to "Shivajinagar • Platform 1", "Lonavala → Pune" to "Pune • Platform 4")
        else -> emptyList()
    }

    private val cache = HashMap<String, Pair<List<ServiceStatus>, List<Departure>>>()

    private fun build(c: City): Pair<List<ServiceStatus>, List<Departure>> = cache.getOrPut(c.id) {
        if (c.isHyderabad) return@getOrPut hydStatus to hydDepartures
        val area0 = c.areas[0].first
        val area1 = c.areas[1].first
        val deps = mutableListOf<Departure>()
        c.metroLines.forEachIndexed { i, l ->
            deps += Departure("m-$i-a", BoardKind.METRO, l.short, l.name, "Toward ${l.to}", l.color, 120 + i * 170, 5 + i, if (i == 1) "Delayed +4" else "On approach", i != 1, listOf("Platform 1", "AC train"), listOf("Low crowd", "Crowded", "Med crowd", "Low crowd")[i % 4])
            deps += Departure("m-$i-b", BoardKind.METRO, l.short, l.name, "Toward ${l.from}", l.color, 260 + i * 190, 5 + i, "Scheduled", true, listOf("Platform 2"), "Med crowd")
        }
        val op = c.busOperator
        val codes = listOf("${100 + c.name.length * 17}", "${200 + c.name.length * 11}A", "AIR-${300 + c.name.length}")
        deps += Departure("bus-0", BoardKind.BUS, codes[0], "$op ${codes[0]}", "$area0 → ${c.areas[3].first}", LineColors.AC_BUS, 300, 12, "Live GPS", true, listOf("AC", "Accessible"), "Med crowd")
        deps += Departure("bus-1", BoardKind.BUS, codes[1], "$op ${codes[1]}", "$area1 → ${c.areas[5].first} via ${c.areas[2].first}", LineColors.BUS, 560, 15, "Diverted", false, listOf("Ordinary"), "Crowded")
        deps += Departure("bus-2", BoardKind.BUS, "AIR", "$op Airport", "$area0 → Airport", LineColors.AC_BUS, 900, 30, "Scheduled", true, listOf("Luggage space"), "Low crowd")
        railRoutes(c).forEachIndexed { i, (route, meta) ->
            deps += Departure("rail-$i", BoardKind.RAIL, "R", railName(c) ?: "Rail", route, LineColors.MMTS, 840 + i * 700, 15 + i * 5, "On time", true, listOf(meta), if (i == 0) "Crowded" else "Med crowd")
        }
        val status = buildList {
            c.metroLines.firstOrNull()?.let { add(ServiceStatus("${it.name} • Good service", "Every 5 min", "${it.from} ↔ ${it.to}. First 6:00 AM • last 11:00 PM.", 0, it.color)) }
            c.metroLines.getOrNull(1)?.let { add(ServiceStatus("${it.name} • Minor delay", "+4 min", "Signal check near ${it.to}. Normal service resuming shortly.", 1, it.color)) }
            add(ServiceStatus("$op Bus Network Alert", codes[1], "Lane closure at ${c.flavor.busyRoad}. Expect +10–15 min; buses use ${c.flavor.altRoute}.", 2, LineColors.BUS))
        }
        status to deps
    }

    val status: List<ServiceStatus> get() = build(CurrentCity.city).first
    val departures: List<Departure> get() = build(CurrentCity.city).second

    val kinds: List<BoardKind>
        get() = BoardKind.entries.filter { k -> departures.any { it.kind == k } }
}
