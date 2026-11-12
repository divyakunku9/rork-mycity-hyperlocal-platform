package com.sheharsaathi.app.data

import com.sheharsaathi.app.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** "Today, 31 Oct" style label [offset] days from now. */
fun dayLabel(offset: Int, word: String? = null): String {
    val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
    val date = SimpleDateFormat("d MMM", Locale.US).format(c.time)
    return "${word ?: SimpleDateFormat("EEE", Locale.US).format(c.time)}, $date"
}

/** Upcoming Saturday (0) or Sunday (1). */
fun weekendLabel(which: Int): String {
    val c = Calendar.getInstance()
    val target = if (which == 0) Calendar.SATURDAY else Calendar.SUNDAY
    repeat(7) { if (c.get(Calendar.DAY_OF_WEEK) != target) c.add(Calendar.DAY_OF_YEAR, 1) }
    return SimpleDateFormat("EEE, d MMM", Locale.US).format(c.time)
}

data class CityContentSet(
    val events: List<CityEvent>,
    val issues: List<CivicIssue>,
    val spots: List<Spot>,
    val alert: CityAlert,
    val news: List<NewsItem>,
)

/** Sample news, events, issues and spots written for each city. */
object CityContent {
    private val cache = HashMap<String, CityContentSet>()

    fun of(c: City): CityContentSet = cache.getOrPut(c.id) {
        if (c.isHyderabad) CityContentSet(HydContent.events, HydContent.issues, HydContent.spots, HydContent.alert, HydContent.news)
        else build(c)
    }

    private fun ll(c: City, i: Int, dLat: Double = 0.0, dLng: Double = 0.0): Pair<Double, Double> {
        val a = c.areas[i % c.areas.size].second
        return (a.first + dLat) to (a.second + dLng)
    }

    private fun area(c: City, i: Int) = c.areas[i % c.areas.size].first

    private fun build(c: City): CityContentSet {
        val f = c.flavor
        val p = c.id.take(3)
        val e1 = ll(c, 1, 0.003, -0.002)
        val e2 = ll(c, 0, -0.002, 0.003)
        val e3 = ll(c, 5, 0.002, 0.002)
        val e4 = ll(c, 2, -0.003, -0.001)
        val e5 = ll(c, 3, 0.001, 0.004)
        val e6 = ll(c, 4, -0.002, -0.003)
        val events = listOf(
            CityEvent(
                id = "$p-rooftop-jazz", title = "${c.name} Rooftop Jazz Night", category = EventCategory.MUSIC,
                dateLabel = weekendLabel(0), timeLabel = "8:00 PM", venue = f.rooftop, area = area(c, 1),
                priceRupees = 699, imageRes = R.drawable.img_rooftop_jazz, interested = 1980, distanceKm = 2.4,
                description = "An open-air evening of live jazz above the ${c.name} skyline. A local quartet plays standards and Indo-jazz originals, with a guest saxophone set at 9:30 PM. Food and drinks on the terrace.",
                bucket = DayBucket.WEEKEND, featured = true, fastFilling = true, lat = e1.first, lng = e1.second, pinLabel = "Rooftop Jazz 8PM",
            ),
            CityEvent(
                id = "$p-indie-fest", title = "${c.name} Indie Music Fest", category = EventCategory.MUSIC,
                dateLabel = dayLabel(0, "Tonight"), timeLabel = "6:00 PM onwards", venue = f.culturalVenue, area = area(c, 0),
                priceRupees = 499, imageRes = R.drawable.img_indie_fest, interested = 1260, distanceKm = 0.9,
                description = "Live sets by ${c.name}'s indie bands, acoustic jams in the courtyard and a food court of ${f.food}. Doors open at 5:30 PM. Entry 18+ with ID.",
                bucket = DayBucket.TODAY, fastFilling = true, lat = e2.first, lng = e2.second, pinLabel = "Indie Fest 6PM",
            ),
            CityEvent(
                id = "$p-silent-reading", title = "${f.park} Silent Reading", category = EventCategory.CULTURE,
                dateLabel = dayLabel(0, "Today"), timeLabel = "7:00 AM – 9:00 AM", venue = "Main lawns, ${f.park}", area = area(c, 5),
                priceRupees = 0, imageRes = R.drawable.img_park_reading, interested = 1500, distanceKm = 3.1,
                description = "Bring a book, a mat and some quiet. Two hours of shared silent reading under the trees, followed by an optional book swap at the gate.",
                bucket = DayBucket.TODAY, lat = e3.first, lng = e3.second, pinLabel = "Silent Reading 7AM",
            ),
            CityEvent(
                id = "$p-tech-demo", title = "Startup Demo Day at ${f.techHub}", category = EventCategory.TECH,
                dateLabel = dayLabel(1, "Tomorrow"), timeLabel = "2:00 PM – 6:00 PM", venue = f.techHub, area = area(c, 2),
                priceRupees = 299, imageRes = R.drawable.img_tech_demo, interested = 610, distanceKm = 1.8,
                description = "Twelve early-stage ${c.name} startups demo live to founders and angels. Networking chai break at 4 PM. Passes include a printed founder directory.",
                bucket = DayBucket.TOMORROW, lat = e4.first, lng = e4.second, pinLabel = "Demo Day 2PM",
            ),
            CityEvent(
                id = "$p-flea-market", title = "${f.market} Flea & Farmers Market", category = EventCategory.FLEA,
                dateLabel = weekendLabel(0), timeLabel = "8:00 AM – 2:00 PM", venue = f.market, area = area(c, 3),
                priceRupees = 0, imageRes = R.drawable.img_flea_market, interested = 3100, distanceKm = 4.8,
                description = "Over 40 organic farm collectives from across ${c.state}, small-batch coffee roasters and live buskers. Open to all.",
                bucket = DayBucket.WEEKEND, lat = e5.first, lng = e5.second, pinLabel = "Flea Market 8AM",
            ),
            CityEvent(
                id = "$p-pottery", title = "Sunday Pottery Pop-up", category = EventCategory.ART,
                dateLabel = weekendLabel(1), timeLabel = "11:00 AM – 4:00 PM", venue = f.artVenue, area = area(c, 4),
                priceRupees = 450, imageRes = R.drawable.img_pottery, interested = 300, distanceKm = 4.2,
                description = "Hands-on wheel throwing for beginners. Includes clay kit and kiln firing; collect your finished piece a week later. Small batches per slot.",
                bucket = DayBucket.WEEKEND, lat = e6.first, lng = e6.second, pinLabel = "Pottery 11AM",
            ),
        )

        val m = c.municipal.handle
        val i1 = ll(c, 0, 0.002, 0.002)
        val i2 = ll(c, 0, -0.003, 0.001)
        val i3 = ll(c, 0, 0.001, -0.003)
        val issues = listOf(
            CivicIssue(
                id = "$p-pothole", title = "Severe pothole on ${area(c, 0)} main road", category = IssueCategory.POTHOLE,
                location = "Main Road, ${area(c, 0)}", description = "Large water-filled crater causing two-wheelers to skid during peak hours.",
                authorities = listOf(m, c.traffic.handle), urgency = Urgency.CRITICAL, status = IssueStatus.IN_PROGRESS, upvotes = 39, responses = ResponseCounts(seen = 39),
                imageRes = R.drawable.img_pothole, reportedAgo = "2 days ago", lat = i1.first, lng = i1.second,
                authorityNote = "${c.municipalShort} ward crew assigned. Patch work scheduled tonight after 11 PM.",
            ),
            CivicIssue(
                id = "$p-streetlight", title = "Streetlights restored, ${area(c, 0)}", category = IssueCategory.STREETLIGHT,
                location = "Park entrance, ${area(c, 0)}", description = "Three streetlights out near the park entrance.",
                authorities = listOf(c.power.handle), urgency = Urgency.MEDIUM, status = IssueStatus.RESOLVED, upvotes = 24, responses = ResponseCounts(seen = 24),
                reportedAgo = "6 days ago", lat = i2.first, lng = i2.second, authorityNote = "All three fixtures replaced with LED units.",
            ),
            CivicIssue(
                id = "$p-garbage", title = "Garbage pile-up near ${area(c, 0)} bus stop", category = IssueCategory.GARBAGE,
                location = "Bus stop, ${area(c, 0)}", description = "Mixed waste dumped beside the bus stop for three days.",
                authorities = listOf(m), urgency = Urgency.MEDIUM, status = IssueStatus.UNDER_REVIEW, upvotes = 16, responses = ResponseCounts(seen = 16),
                reportedAgo = "Yesterday", lat = i3.first, lng = i3.second,
            ),
        )

        val s1 = ll(c, 1, 0.001, 0.001)
        val s2 = ll(c, 0, 0.0015, -0.001)
        val s3 = ll(c, 2, -0.001, 0.0015)
        val spots = listOf(
            Spot("$p-tiffin", f.tiffin, "Local favourite • ${area(c, 1)}", 4.5, R.drawable.img_dosa, "FOOD", "Open now", s1.first, s1.second),
            Spot("$p-cafe", f.cafe, "Coffee • ${area(c, 0)}", 4.3, R.drawable.img_coffee, "CAFE", "Closes 11 PM", s2.first, s2.second),
            Spot("$p-mall", f.mall, "Shopping • ${area(c, 2)}", 4.4, R.drawable.img_shopping_street, "SHOPPING", "Open till 10 PM", s3.first, s3.second),
        )

        val alert = CityAlert(
            id = "$p-water-maint", authority = c.water.handle,
            title = "Pipeline maintenance in ${area(c, 0)} & ${area(c, 1)}",
            body = "Scheduled valve replacement on the trunk main. Low pressure expected across ${area(c, 0)}, ${area(c, 1)} and ${area(c, 2)}. Please store drinking water in advance.",
            window = "Tomorrow 6 AM – 6 PM", impact = "Impacts ~10,000 households", acknowledgements = 162,
        )

        val news = listOf(
            NewsItem(
                id = "$p-diversion", kind = NewsKind.BULLETIN, tag = "Urgent dispatch", ward = area(c, 2), ago = "35m ago",
                title = "${f.busyRoad}: lane closure until 6 AM tomorrow",
                body = "${c.traffic.name} is diverting traffic via ${f.altRoute} for road repairs. Expect 10–15 minutes extra.",
                endorsements = 488, comments = 57, source = c.traffic.handle,
            ),
            NewsItem(
                id = "$p-flea-returns", kind = NewsKind.STORY, tag = area(c, 3), ward = area(c, 3), ago = "2h ago",
                title = "${f.market} Flea & Farmers Market returns this Saturday",
                body = "Over 40 organic farm collectives from across ${c.state}, small-batch coffee roasters and live buskers. Entry free for all residents.",
                imageRes = R.drawable.img_flea_market, endorsements = 318, comments = 112, source = "Shehar Saathi Desk",
            ),
            NewsItem(
                id = "$p-waste-win", kind = NewsKind.CIVIC_WIN, tag = "Civic win", ward = area(c, 1), ago = "4h ago",
                title = "${area(c, 1)} housing societies hit 100% waste segregation",
                body = "${c.municipalShort} recognised 380 households in ${area(c, 1)} for zero-landfill compliance over 90 consecutive days. 1.2 tonnes of compost diverted monthly.",
                endorsements = 842, comments = 38, source = m,
            ),
            NewsItem(
                id = "$p-coffee-offer", kind = NewsKind.OFFER, tag = "Local business partner", ward = area(c, 0), ago = "Ends in 8h",
                title = "Flash 20% off at ${f.cafe} for Shehar Saathi residents",
                body = "Show your Shehar Saathi code at checkout across ${area(c, 0)} and ${area(c, 1)} outlets. Valid on single-origin roasts and cold brews.",
                imageRes = R.drawable.img_coffee, endorsements = 201, comments = 11, source = f.cafe,
                offerCode = "MYCITY20${c.id.take(3).uppercase()}",
            ),
            NewsItem(
                id = "$p-food", kind = NewsKind.STORY, tag = "Food & culture", ward = area(c, 4), ago = "6h ago",
                title = "Night food lane in ${area(c, 4)} adds 12 new stalls",
                body = "From ${f.food}, the festive food lane now runs till 11 PM through Sunday.",
                imageRes = R.drawable.img_dosa, endorsements = 254, comments = 33, source = "Shehar Saathi Desk",
            ),
        )
        return CityContentSet(events, issues, spots, alert, news)
    }
}
