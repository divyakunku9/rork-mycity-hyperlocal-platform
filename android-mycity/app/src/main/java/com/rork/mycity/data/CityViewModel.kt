package com.rork.mycity.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CityViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = Prefs(app)
    val otp: OtpProvider = DemoOtpProvider()

    init {
        Cities.byId(prefs.cityId)?.let { CurrentCity.city = it }
    }

    // ---------- Session & profile ----------
    var signedIn by mutableStateOf(prefs.signedIn)
        private set
    var hasCity by mutableStateOf(prefs.signedIn && Cities.byId(prefs.cityId) != null)
        private set
    var profile by mutableStateOf(prefs.profile)
        private set

    val city: City get() = CurrentCity.city

    /** True while the city picker is open from the profile or header. */
    var pickingCity by mutableStateOf(false)

    fun completeSignIn(target: OtpTarget) {
        val p = profile
        profile = if (target.channel == OtpChannel.PHONE) p.copy(phone = target.value) else p.copy(email = target.value)
        prefs.profile = profile
        prefs.signedIn = true
        signedIn = true
    }

    fun updateProfile(p: UserProfile) {
        profile = p
        prefs.profile = p
    }

    fun signOut() {
        prefs.clearSession()
        signedIn = false
        hasCity = false
    }

    val notif = mutableStateMapOf(
        "civic" to prefs.flag("civic"), "events" to prefs.flag("events"), "emergency" to prefs.flag("emergency"),
    )

    fun setNotif(key: String, on: Boolean) {
        notif[key] = on
        prefs.setFlag(key, on)
    }

    // ---------- City content ----------
    val events = mutableStateListOf<CityEvent>().apply { addAll(SampleData.events) }
    var spots by mutableStateOf(SampleData.spots)
        private set
    var news by mutableStateOf(SampleData.news)
        private set
    var alert by mutableStateOf(SampleData.alert)
        private set

    val issues = mutableStateListOf<CivicIssue>().apply { addAll(SampleData.issues) }

    /** Events the user hosts, kept per city. */
    private val hostedByCity = HashMap<String, MutableList<CityEvent>>()
    private val reportsByCity = HashMap<String, MutableList<CivicIssue>>()

    fun selectCity(c: City) {
        // Keep the user's own reports and events with the city they belong to.
        reportsByCity[city.id] = issues.filter { it.isMine }.toMutableList()
        CurrentCity.city = c
        prefs.cityId = c.id
        hasCity = true
        pickingCity = false
        events.clear(); events.addAll(hostedByCity[c.id].orEmpty() + SampleData.events)
        issues.clear(); issues.addAll(reportsByCity[c.id].orEmpty() + SampleData.issues)
        spots = SampleData.spots
        news = SampleData.news
        alert = SampleData.alert
        alertAcknowledged = false
        locationNotice = null
        val start = Transit.currentLocation
        userPlace = start
        locationSource = LocationSource.DEFAULT
        tripFrom = start
        tripTo = null
        activeRoute = null
        recentPlaces.clear(); recentPlaces.addAll(Transit.recentIds.mapNotNull { Transit.place(it) })
        weather = null
        recenterTick++
        refreshWeather()
        refreshTrending()
    }

    fun addEvent(
        title: String, category: EventCategory, dateLabel: String, timeLabel: String, bucket: DayBucket,
        venue: Place, priceRupees: Int, imageUri: String?, description: String,
    ): String {
        val id = "mine-${System.currentTimeMillis()}"
        val fallback = when (category) {
            EventCategory.MUSIC -> com.rork.mycity.R.drawable.img_indie_fest
            EventCategory.WORKSHOP, EventCategory.ART -> com.rork.mycity.R.drawable.img_pottery
            EventCategory.FLEA -> com.rork.mycity.R.drawable.img_flea_market
            EventCategory.TECH -> com.rork.mycity.R.drawable.img_tech_demo
            EventCategory.CULTURE -> com.rork.mycity.R.drawable.img_park_reading
        }
        val e = CityEvent(
            id = id, title = title.trim(), category = category, dateLabel = dateLabel, timeLabel = timeLabel,
            venue = venue.name, area = venue.area.substringBefore(",").ifBlank { city.name }, priceRupees = priceRupees,
            imageRes = fallback, interested = 1,
            distanceKm = Transit.distanceKm(userPlace.lat, userPlace.lng, venue.lat, venue.lng),
            description = description.trim().ifBlank { "Hosted by ${profile.name.ifBlank { "a MyCity neighbour" }}." },
            bucket = bucket, lat = venue.lat, lng = venue.lng, pinLabel = title.trim().take(18),
            imageUri = imageUri, hostedByYou = true,
        )
        hostedByCity.getOrPut(city.id) { mutableListOf() }.add(0, e)
        events.add(0, e)
        savedEvents[id] = true
        return id
    }

    val myEvents: List<CityEvent> get() = events.filter { it.hostedByYou }

    // ---------- Live weather & trending ----------
    var weather by mutableStateOf<Weather?>(null)
        private set
    var trending by mutableStateOf<List<TrendingPlace>>(emptyList())
        private set
    var trendingLoading by mutableStateOf(false)
        private set
    private var weatherJob: Job? = null
    private var trendingJob: Job? = null

    fun refreshWeather() {
        weatherJob?.cancel()
        weatherJob = viewModelScope.launch {
            while (isActive) {
                val p = userPlace
                LiveData.weather(p.lat, p.lng)?.let { weather = it }
                delay(15 * 60_000L)
            }
        }
    }

    fun refreshTrending() {
        trendingJob?.cancel()
        trendingJob = viewModelScope.launch {
            trendingLoading = true
            delay(400)
            val p = userPlace
            val list = LiveData.trending(p.lat, p.lng)
            if (list.isNotEmpty() || trending.isEmpty()) trending = list
            trendingLoading = false
        }
    }


    val savedPlaces = mutableStateListOf<Place>()
    fun toggleSavedPlace(p: Place) {
        val i = savedPlaces.indexOfFirst { it.name == p.name && it.lat == p.lat }
        if (i >= 0) savedPlaces.removeAt(i) else savedPlaces.add(0, p)
    }
    fun isPlaceSaved(p: Place) = savedPlaces.any { it.name == p.name && it.lat == p.lat }

    val savedEvents = mutableStateMapOf<String, Boolean>()
    val bookings = mutableStateMapOf<String, Int>()
    val upvotedIssues = mutableStateMapOf<String, Boolean>()
    val endorsedNews = mutableStateMapOf<String, Boolean>()
    val savedNews = mutableStateMapOf<String, Boolean>()
    val claimedOffers = mutableStateMapOf<String, Boolean>()

    var alertAcknowledged by mutableStateOf(false)
        private set

    var exploreFilter by mutableStateOf(ExploreFilter.ALL)
    var exploreQuery by mutableStateOf("")

    // Where the user is: GPS, manual pick, or the city's default neighbourhood
    var userPlace by mutableStateOf(Transit.currentLocation)
        private set
    var locationSource by mutableStateOf(LocationSource.DEFAULT)
        private set
    var locating by mutableStateOf(false)
    var locationNotice by mutableStateOf<String?>(null)
    var showLocationPicker by mutableStateOf(false)
    var locationBannerDismissed by mutableStateOf(false)
    /** Bumped whenever the map should fly to [userPlace]. */
    var recenterTick by mutableIntStateOf(0)
        private set

    val locationLabel: String
        get() = if (locationSource == LocationSource.DEFAULT) "${userPlace.name}, ${city.name}" else Geocoder.headerLabel(userPlace)

    private fun setUserPlace(p: Place, source: LocationSource) {
        val followTrip = tripFrom.kind == PlaceKind.CURRENT
        userPlace = p.copy(id = "gps", kind = PlaceKind.CURRENT)
        locationSource = source
        if (followTrip) tripFrom = userPlace
        recenterTick++
        refreshWeather()
        refreshTrending()
    }

    fun setManualLocation(p: Place) {
        locationNotice = null
        setUserPlace(p.copy(area = if (p.area.contains(city.name)) p.area else "${p.area}, ${city.name}"), LocationSource.MANUAL)
        showLocationPicker = false
    }

    /** Applies a GPS fix; outside the chosen city we keep the current place and ask for a manual pick. */
    fun applyGpsFix(lat: Double, lng: Double) {
        if (!city.contains(lat, lng)) {
            locating = false
            val near = Cities.nearest(lat, lng)
            locationNotice = if (near.contains(lat, lng) && near.id != city.id)
                "Your GPS shows you in ${near.name}. Switch city from your profile, or pick a ${city.name} area."
            else "Your GPS shows you outside ${city.name}. Pick your area manually."
            showLocationPicker = true
            return
        }
        locationNotice = null
        val provisional = Place("gps", "Current location", city.name, lat, lng, PlaceKind.CURRENT)
        setUserPlace(provisional, LocationSource.GPS)
        viewModelScope.launch {
            val named = Geocoder.reverse(lat, lng)
            if (named != null && locationSource == LocationSource.GPS && userPlace.lat == lat) {
                setUserPlace(named.copy(lat = lat, lng = lng), LocationSource.GPS)
            }
            locating = false
        }
    }

    fun gpsFailed(message: String) {
        locating = false
        locationNotice = message
    }

    fun recenter() { recenterTick++ }

    // Journey planner (Citymapper-style)
    var tripFrom by mutableStateOf(Transit.currentLocation)
    var tripTo by mutableStateOf<Place?>(null)
    var departOffsetMin by mutableIntStateOf(0)
    var rainSafe by mutableStateOf(false)
    var stepFree by mutableStateOf(false)
    var routeTab by mutableStateOf(RouteTab.BEST)
    var activeRoute by mutableStateOf<RouteOption?>(null)
    val favoriteLines = mutableStateMapOf<String, Boolean>().apply { put("b-ng", true) }
    val recentPlaces = mutableStateListOf<Place>().apply { addAll(Transit.recentIds.mapNotNull { Transit.place(it) }) }

    val routes: List<RouteOption>
        get() = tripTo?.let { Transit.plan(tripFrom, it, rainSafe, stepFree) } ?: emptyList()

    fun planTo(place: Place) {
        tripTo = place
        routeTab = RouteTab.BEST
        if (place.kind != PlaceKind.HOME && place.kind != PlaceKind.WORK) {
            recentPlaces.removeAll { it.id == place.id }
            recentPlaces.add(0, place)
            if (recentPlaces.size > 5) recentPlaces.removeAt(recentPlaces.lastIndex)
        }
    }

    fun swapTrip() {
        val to = tripTo ?: return
        tripTo = tripFrom
        tripFrom = to
    }

    fun toggleFavoriteLine(id: String) {
        if (favoriteLines[id] == true) favoriteLines.remove(id) else favoriteLines[id] = true
    }

    // Ward infra vote
    var pollChoice by mutableStateOf<Int?>(null)
        private set
    private var pollVotes = intArrayOf(948, 534)
    var pollTotal by mutableIntStateOf(pollVotes.sum())
        private set

    fun pollPercent(index: Int): Int {
        val total = pollVotes.sum().coerceAtLeast(1)
        return Math.round(pollVotes[index] * 100f / total)
    }

    fun vote(index: Int) {
        if (pollChoice != null) return
        pollVotes[index] += 1
        pollTotal = pollVotes.sum()
        pollChoice = index
    }

    val reportsValidated: Int get() = 54 + issues.count { it.isMine }
    val myIssues: List<CivicIssue> get() = issues.filter { it.isMine }

    fun event(id: String) = events.firstOrNull { it.id == id }
    fun issue(id: String) = issues.firstOrNull { it.id == id }

    fun toggleSaveEvent(id: String) {
        if (savedEvents[id] == true) savedEvents.remove(id) else savedEvents[id] = true
    }

    fun book(id: String, qty: Int) {
        bookings[id] = (bookings[id] ?: 0) + qty
    }

    fun toggleUpvote(id: String) {
        val idx = issues.indexOfFirst { it.id == id }
        if (idx < 0) return
        val already = upvotedIssues[id] == true
        val item = issues[idx]
        issues[idx] = item.copy(upvotes = item.upvotes + if (already) -1 else 1)
        if (already) upvotedIssues.remove(id) else upvotedIssues[id] = true
    }

    fun toggleEndorse(id: String) {
        if (endorsedNews[id] == true) endorsedNews.remove(id) else endorsedNews[id] = true
    }

    fun toggleSaveNews(id: String) {
        if (savedNews[id] == true) savedNews.remove(id) else savedNews[id] = true
    }

    fun claimOffer(id: String) { claimedOffers[id] = true }

    fun acknowledgeAlert() { alertAcknowledged = !alertAcknowledged }

    fun submitReport(
        category: IssueCategory,
        description: String,
        authorities: List<String>,
        urgency: Urgency,
        imageUri: String?,
        location: String,
        lat: Double? = null,
        lng: Double? = null,
    ): String {
        val id = "report-${System.currentTimeMillis()}"
        val title = when (category) {
            IssueCategory.POTHOLE -> "Pothole reported"
            IssueCategory.STREETLIGHT -> "Streetlight outage"
            IssueCategory.GARBAGE -> "Garbage dump reported"
            IssueCategory.WATER -> "Water / drainage issue"
        } + " • " + location.substringBefore(",")
        val jitter = ((issues.size % 5) - 2) * 0.0009
        issues.add(
            CivicIssue(
                id = id,
                title = title,
                category = category,
                location = location,
                description = description.trim(),
                authorities = authorities,
                urgency = urgency,
                status = IssueStatus.REPORTED,
                upvotes = 1,
                imageUri = imageUri,
                reportedAgo = "Just now",
                isMine = true,
                lat = lat ?: (userPlace.lat + jitter),
                lng = lng ?: (userPlace.lng - jitter),
            )
        )
        upvotedIssues[id] = true
        return id
    }

    init {
        if (hasCity) {
            refreshWeather()
            refreshTrending()
        }
    }
}
