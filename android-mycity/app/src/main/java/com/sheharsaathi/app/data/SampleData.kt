package com.sheharsaathi.app.data

import com.sheharsaathi.app.R

object SampleData {

    val authorities: List<Authority> get() = CurrentCity.city.authorities

    fun defaultAuthorityFor(category: IssueCategory): String = CurrentCity.city.let { c ->
        when (category) {
            IssueCategory.POTHOLE -> c.municipal.handle
            IssueCategory.STREETLIGHT -> c.municipal.handle // streetlights are run by the municipal corporation
            IssueCategory.GARBAGE -> c.municipal.handle
            IssueCategory.WATER -> c.water.handle
        }
    }

    val events: List<CityEvent> get() = CityContent.of(CurrentCity.city).events
    val issues: List<CivicIssue> get() = CityContent.of(CurrentCity.city).issues
    val spots: List<Spot> get() = CityContent.of(CurrentCity.city).spots
    val alert: CityAlert get() = CityContent.of(CurrentCity.city).alert
    val news: List<NewsItem> get() = CityContent.of(CurrentCity.city).news
}

/** Hyderabad's hand-written content. */
internal object HydContent {

    val events = listOf(
        CityEvent(
            id = "rooftop-jazz",
            title = "Deccan Rooftop Jazz Night",
            category = EventCategory.MUSIC,
            dateLabel = weekendLabel(0),
            timeLabel = "8:00 PM",
            venue = "The Moonshine Project",
            area = "Jubilee Hills",
            priceRupees = 599,
            imageRes = R.drawable.img_rooftop_jazz,
            interested = 2140,
            distanceKm = 2.6,
            description = "An open-air evening of live jazz above the city lights. The Charminar Collective plays standards and Hindustani-jazz originals, with a guest saxophone set at 9:30 PM. Food and drinks on the terrace.",
            bucket = DayBucket.WEEKEND,
            featured = true,
            fastFilling = true,
            lat = 17.4326, lng = 78.4071,
            pinLabel = "Rooftop Jazz 8PM",
        ),
        CityEvent(
            id = "indie-fest",
            title = "Hyderabad Indie Music Fest",
            category = EventCategory.MUSIC,
            dateLabel = dayLabel(0, "Tonight"),
            timeLabel = "6:00 PM onwards",
            venue = "Shilpakala Vedika",
            area = "HITEC City",
            priceRupees = 499,
            imageRes = R.drawable.img_indie_fest,
            interested = 1200,
            distanceKm = 0.8,
            description = "Live sets by city indie bands, acoustic jams in the courtyard and a food court of Irani chai, haleem and Osmania biscuits. Doors open at 5:30 PM. Entry 18+ with ID.",
            bucket = DayBucket.TODAY,
            fastFilling = true,
            lat = 17.4527, lng = 78.3790,
            pinLabel = "Indie Fest 6PM",
        ),
        CityEvent(
            id = "silent-reading",
            title = "KBR Park Silent Reading",
            category = EventCategory.CULTURE,
            dateLabel = dayLabel(0, "Today"),
            timeLabel = "7:00 AM – 9:00 AM",
            venue = "Main Gate Lawns, KBR National Park",
            area = "Jubilee Hills",
            priceRupees = 0,
            imageRes = R.drawable.img_park_reading,
            interested = 1800,
            distanceKm = 3.4,
            description = "Bring a book, a mat and some quiet. Two hours of shared silent reading under the peacock trees, followed by an optional book swap at the gate.",
            bucket = DayBucket.TODAY,
            lat = 17.4239, lng = 78.4230,
            pinLabel = "Silent Reading 7AM",
        ),
        CityEvent(
            id = "tech-demo",
            title = "Startup Demo Day at T-Hub",
            category = EventCategory.TECH,
            dateLabel = dayLabel(1, "Tomorrow"),
            timeLabel = "2:00 PM – 6:00 PM",
            venue = "T-Hub 2.0",
            area = "Raidurg",
            priceRupees = 299,
            imageRes = R.drawable.img_tech_demo,
            interested = 640,
            distanceKm = 1.6,
            description = "Twelve early-stage Hyderabad startups demo live to founders and angels. Networking chai break at 4 PM. Passes include a printed founder directory.",
            bucket = DayBucket.TOMORROW,
            lat = 17.4337, lng = 78.3770,
            pinLabel = "Demo Day 2PM",
        ),
        CityEvent(
            id = "flea-market",
            title = "Lamakaan Flea & Farmers Market",
            category = EventCategory.FLEA,
            dateLabel = weekendLabel(0),
            timeLabel = "8:00 AM – 2:00 PM",
            venue = "Lamakaan",
            area = "Banjara Hills",
            priceRupees = 0,
            imageRes = R.drawable.img_flea_market,
            interested = 3400,
            distanceKm = 5.2,
            description = "Over 45 organic farm collectives from across Telangana, small-batch coffee roasters and live buskers in the Lamakaan courtyard. Open to all.",
            bucket = DayBucket.WEEKEND,
            lat = 17.4179, lng = 78.4410,
            pinLabel = "Flea Market 8AM",
        ),
        CityEvent(
            id = "pottery",
            title = "Sunday Pottery Pop-up",
            category = EventCategory.ART,
            dateLabel = weekendLabel(1),
            timeLabel = "11:00 AM – 4:00 PM",
            venue = "Kalakriti Art Gallery",
            area = "Banjara Hills",
            priceRupees = 450,
            imageRes = R.drawable.img_pottery,
            interested = 320,
            distanceKm = 4.6,
            description = "Hands-on wheel throwing for beginners. Includes clay kit and kiln firing — collect your finished piece a week later. Small batches per slot.",
            bucket = DayBucket.WEEKEND,
            lat = 17.4156, lng = 78.4347,
            pinLabel = "Pottery 11AM",
        ),
    )

    val issues = listOf(
        CivicIssue(
            id = "pothole-ayyappa",
            title = "Severe pothole in Ayyappa Society",
            category = IssueCategory.POTHOLE,
            location = "100 Feet Rd, Ayyappa Society, Madhapur",
            description = "Large water-filled crater causing two-wheelers to skid during peak hours.",
            authorities = listOf("@GHMCOnline", "@HYDTP"),
            urgency = Urgency.CRITICAL,
            status = IssueStatus.IN_PROGRESS,
            upvotes = 42, responses = ResponseCounts(seen = 42),
            imageRes = R.drawable.img_pothole,
            reportedAgo = "2 days ago",
            lat = 17.4445, lng = 78.3935,
            authorityNote = "GHMC Serilingampally Zone crew assigned. Bitumen patch scheduled tonight after 11 PM.",
        ),
        CivicIssue(
            id = "streetlight-kavuri",
            title = "Streetlights restored, Kavuri Hills",
            category = IssueCategory.STREETLIGHT,
            location = "Kavuri Hills Phase 2, Madhapur",
            description = "Three streetlights out near the park entrance.",
            authorities = listOf("@TGSPDCL"),
            urgency = Urgency.MEDIUM,
            status = IssueStatus.RESOLVED,
            upvotes = 27, responses = ResponseCounts(seen = 27),
            reportedAgo = "6 days ago",
            lat = 17.4405, lng = 78.4010,
            authorityNote = "All three fixtures replaced with LED units.",
        ),
        CivicIssue(
            id = "garbage-metro",
            title = "Garbage pile-up near Madhapur Metro",
            category = IssueCategory.GARBAGE,
            location = "Below Madhapur Metro Station, Madhapur",
            description = "Mixed waste dumped beside the bus stop for three days.",
            authorities = listOf("@GHMCOnline"),
            urgency = Urgency.MEDIUM,
            status = IssueStatus.UNDER_REVIEW,
            upvotes = 18, responses = ResponseCounts(seen = 18),
            reportedAgo = "Yesterday",
            lat = 17.4368, lng = 78.3965,
        ),
    )

    val spots = listOf(
        Spot("chutneys", "Chutneys", "South Indian tiffins • Jubilee Hills", 4.5, R.drawable.img_dosa, "TIFFINS", "Open now", 17.4316, 78.4074),
        Spot("third-wave", "Third Wave Coffee", "Madhapur • Ayyappa Society", 4.3, R.drawable.img_coffee, "CAFE", "Closes 11 PM", 17.4430, 78.3920),
        Spot("inorbit", "Inorbit Mall", "Shopping • Madhapur", 4.4, R.drawable.img_shopping_street, "SHOPPING", "Open till 10 PM", 17.4346, 78.3867),
    )

    val alert = CityAlert(
        id = "hmwssb-pipeline",
        authority = "@HMWSSBOnline",
        title = "Pipeline Maintenance in Madhapur & Kondapur",
        body = "Scheduled valve replacement on the Manjeera trunk line. Low pressure expected across Madhapur, Kondapur and Gachibowli. Please store potable water in advance.",
        window = "Tomorrow 6 AM – 6 PM",
        impact = "Impacts ~12,000 households",
        acknowledgements = 184,
    )

    val news = listOf(
        NewsItem(
            id = "htp-diversion", kind = NewsKind.BULLETIN, tag = "Urgent dispatch", ward = "Gachibowli",
            ago = "35m ago", title = "Biodiversity flyover lane closure until 6 AM tomorrow",
            body = "Hyderabad Traffic Police is diverting traffic via Old Mumbai Highway and Mindspace Junction for expansion-joint repairs. Expect 10–15 minutes extra.",
            endorsements = 512, comments = 64, source = "@HYDTP",
        ),
        NewsItem(
            id = "flea-returns", kind = NewsKind.STORY, tag = "Banjara Hills", ward = "Banjara Hills",
            ago = "2h ago", title = "Lamakaan Flea & Farmers Market returns this Saturday",
            body = "Over 45 organic farm collectives from across Telangana, small-batch coffee roasters and live buskers. Entry free for all residents.",
            imageRes = R.drawable.img_flea_market, endorsements = 342, comments = 128, source = "Shehar Saathi Desk",
        ),
        NewsItem(
            id = "waste-milestone", kind = NewsKind.CIVIC_WIN, tag = "Civic win", ward = "Kondapur",
            ago = "4h ago", title = "Kondapur gated communities hit 100% waste segregation",
            body = "GHMC recognised 420 households in Serilingampally Zone for zero-landfill compliance over 90 consecutive days. 1.4 tonnes of compost diverted monthly.",
            endorsements = 890, comments = 41, source = "@GHMCOnline",
        ),
        NewsItem(
            id = "coffee-offer", kind = NewsKind.OFFER, tag = "Local business partner", ward = "Madhapur",
            ago = "Ends in 8h", title = "Flash 20% off at Third Wave Coffee for Shehar Saathi residents",
            body = "Show your Shehar Saathi code at checkout across Madhapur and Jubilee Hills outlets. Valid on single-origin roasts and cold brews.",
            imageRes = R.drawable.img_coffee, endorsements = 214, comments = 12, source = "Third Wave Coffee", offerCode = "MYCITY20HYD",
        ),
        NewsItem(
            id = "shilparamam-food", kind = NewsKind.STORY, tag = "Food & culture", ward = "HITEC City",
            ago = "6h ago", title = "Shilparamam night bazaar adds 12 new food stalls",
            body = "From Hyderabadi haleem to Andhra pesarattu, the festive food lane at Shilparamam now runs till 11 PM through Sunday.",
            imageRes = R.drawable.img_dosa, endorsements = 276, comments = 39, source = "Shehar Saathi Desk",
        ),
    )
}
