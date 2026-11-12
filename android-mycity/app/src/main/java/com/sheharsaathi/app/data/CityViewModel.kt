package com.sheharsaathi.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sheharsaathi.app.data.backend.CityBackend
import com.sheharsaathi.app.data.backend.PollOptionUi
import com.sheharsaathi.app.data.backend.PollUi
import com.sheharsaathi.app.data.backend.Res
import com.sheharsaathi.app.data.backend.Supabase
import com.sheharsaathi.app.data.backend.SupabaseConfig
import com.sheharsaathi.app.data.backend.SupabaseOtpProvider
import com.sheharsaathi.app.data.backend.Times
import com.sheharsaathi.app.data.backend.photoForUpload
import com.sheharsaathi.app.data.backend.valueOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class CityViewModel(private val app: Application) : AndroidViewModel(app) {

    val prefs = Prefs(app)
    private val sb = Supabase(app)
    private val backend = CityBackend(sb)
    private val supabaseOtp = SupabaseOtpProvider(sb)
    val otp: OtpProvider = supabaseOtp

    init {
        Cities.byId(prefs.cityId)?.let { CurrentCity.city = it }
        sb.onSessionLost = {
            viewModelScope.launch {
                if (signedIn) {
                    toast("Your session has expired. Please sign in again.")
                    prefs.clearSession(); profile = UserProfile(); signedIn = false; hasCity = false; clearActivity(); savedItems.clear()
                }
            }
        }
    }

    // ---------- Session & profile ----------
    // A sign-in from the old demo build has no server session, so it asks to sign in again.
    var signedIn by mutableStateOf(prefs.signedIn && sb.signedIn)
        private set
    var hasCity by mutableStateOf(signedIn && Cities.byId(prefs.cityId) != null)
        private set
    var profile by mutableStateOf(prefs.profile)
        private set

    val city: City get() = CurrentCity.city

    /** True when this city's content comes from the Shehar Saathi backend rather than built-in samples. */
    val isLive: Boolean get() = city.id in SupabaseConfig.liveCities

    /** True while live content is being fetched. */
    var loading by mutableStateOf(false)
        private set

    /** Why the last code check failed, for the sign-in screen. */
    val lastSignInError: String? get() = supabaseOtp.lastError

    /** True while the city picker is open from the profile or header. */
    var pickingCity by mutableStateOf(false)

    fun completeSignIn(target: OtpTarget) {
        val p = profile
        profile = if (target.channel == OtpChannel.PHONE) p.copy(phone = target.value) else p.copy(email = target.value)
        prefs.profile = profile
        prefs.signInChannel = target.channel.name
        prefs.signedIn = true
        signedIn = true
        loadSaved()
        syncAccount()
        if (hasCity) loadContent()
    }

    fun updateProfile(p: UserProfile) {
        val before = profile
        profile = p
        prefs.profile = p
        val uid = sb.userId ?: return
        viewModelScope.launch {
            if (p.name != before.name || p.ward != before.ward || p.phone != before.phone || p.email != before.email) {
                (backend.updateProfile(uid, p.name, p.ward, p.phone, p.email) as? Res.Err)?.let { toast("Profile not saved: ${it.message}") }
            }
            if (p.photoUri != before.photoUri) {
                val bytes = photoForUpload(app, p.photoUri, maxSide = 600)
                if (p.photoUri == null || bytes != null) {
                    (backend.setAvatar(uid, bytes) as? Res.Err)?.let { toast("Photo not saved: ${it.message}") }
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { sb.signOut() }
        prefs.clearSession()
        profile = UserProfile()
        signedIn = false
        hasCity = false
        clearActivity()
        savedItems.clear()  // off the screen only; they stay stored for this account
    }

    /** How the user signed in; that contact is their login and can't be edited. */
    val signInChannel: OtpChannel? get() = prefs.signInChannel?.let { runCatching { OtpChannel.valueOf(it) }.getOrNull() }

    val notif = mutableStateMapOf(
        "civic" to prefs.flag("civic"), "events" to prefs.flag("events"), "emergency" to prefs.flag("emergency"),
    )

    fun setNotif(key: String, on: Boolean) {
        notif[key] = on
        prefs.setFlag(key, on)
        val uid = sb.userId ?: return
        viewModelScope.launch { backend.setNotif(uid, key, on) }
    }

    /** After sign-in or app start: pulls the profile and settings saved on the server. */
    private fun syncAccount() {
        val uid = sb.userId ?: return
        viewModelScope.launch {
            backend.profile(uid)?.let { row ->
                val local = profile
                val merged = local.copy(
                    name = row.fullName?.takeIf { it.isNotBlank() } ?: local.name,
                    ward = row.ward?.takeIf { it.isNotBlank() } ?: local.ward,
                    phone = row.phone?.removePrefix("+91")?.takeIf { it.isNotBlank() } ?: local.phone,
                    email = row.email?.takeIf { it.isNotBlank() } ?: local.email,
                    photoUri = local.photoUri ?: backend.avatarUrl(row.photoPath),
                )
                profile = merged
                prefs.profile = merged
                // A name typed on the sign-in screen before the account existed on the server.
                if (row.fullName.isNullOrBlank() && merged.name.isNotBlank()) backend.updateProfile(uid, merged.name, merged.ward, merged.phone, merged.email)
            }
            backend.notifPrefs(uid)?.let { n ->
                mapOf("civic" to n.civic, "events" to n.events, "emergency" to n.emergency).forEach { (k, v) ->
                    notif[k] = v; prefs.setFlag(k, v)
                }
            }
        }
    }

    // ---------- City content ----------
    val events = mutableStateListOf<CityEvent>()
    var spots by mutableStateOf(SampleData.spots)
        private set
    var news by mutableStateOf<List<NewsItem>>(emptyList())
        private set
    /** The city's current authority alert, or null when there is none in force. */
    var alert by mutableStateOf<CityAlert?>(null)
        private set
    /** Official transit notices for live cities (from the backend), shown on the departures board. */
    var serviceStatus by mutableStateOf<List<ServiceStatus>>(emptyList())
        private set

    /** Events listed on ticketing sites (District, Eventbrite, Meetup, AllEvents); live cities only. */
    var listedEvents by mutableStateOf<List<ListedEvent>>(emptyList())
        private set
    var listedEventsLoading by mutableStateOf(false)
        private set
    private var listedEventsJob: Job? = null
    private var listedEventsCity: String? = null

    fun refreshListedEvents() {
        if (!isLive) { listedEvents = emptyList(); return }
        val cityId = city.id
        if (listedEventsCity != cityId) listedEvents = emptyList()
        listedEventsJob?.cancel()
        listedEventsJob = viewModelScope.launch {
            listedEventsLoading = true
            when (val r = backend.listedEvents(cityId)) {
                is Res.Ok -> { listedEvents = r.value; listedEventsCity = cityId }
                is Res.Err -> if (listedEvents.isEmpty()) toast("Couldn't load city events. Pull down to try again.")
            }
            listedEventsLoading = false
        }
    }

    // ---------- Wards (Hyderabad: GHMC, Cyberabad and Malkajgiri corporations) ----------
    var wards by mutableStateOf<List<Ward>>(emptyList())
        private set
    var civicBodies by mutableStateOf<Map<String, CivicBody>>(emptyMap())
        private set
    var wardsLoading by mutableStateOf(false)
        private set
    /** Ward number the user picked by hand; null means "follow my location". */
    var chosenWardNo by mutableStateOf(prefs.chosenWard)
        private set
    /** True while the Civic Hub's ward picker is open. */
    var showWardPicker by mutableStateOf(false)
    private var wardsJob: Job? = null

    /** True for cities whose wards we have (Hyderabad only for now). */
    val hasWards: Boolean get() = city.id == "hyderabad" && isLive

    fun refreshWards() {
        if (!hasWards || wards.isNotEmpty() || wardsJob?.isActive == true) return
        wardsJob = viewModelScope.launch {
            wardsLoading = true
            when (val r = backend.wards()) {
                is Res.Ok -> { wards = r.value.first; civicBodies = r.value.second.associateBy { it.code }; saveWardToProfile() }
                is Res.Err -> toast("Couldn't load ward list. Pull down to try again.")
            }
            wardsLoading = false
        }
    }

    /**
     * The user's ward: the one they picked, else the one for where they are (GPS or a spot they chose).
     * Null until we know where they are, or when they're outside the three corporations.
     */
    val myWard: WardMatch?
        get() {
            if (!hasWards) return null
            chosenWardNo?.let { no -> wards.firstOrNull { it.no == no }?.let { return WardMatch(it, WardSource.CHOSEN) } }
            if (locationSource == LocationSource.DEFAULT) return null
            return WardFinder.find(userPlace, wards)
        }

    fun civicBodyFor(w: Ward?): CivicBody? = w?.let { civicBodies[it.corporation] }

    fun chooseWard(no: Int) {
        chosenWardNo = no
        prefs.chosenWard = no
        showWardPicker = false
        saveWardToProfile()
    }

    /** Go back to working the ward out from the user's location. */
    fun followLocationForWard() {
        chosenWardNo = null
        prefs.chosenWard = null
        saveWardToProfile()
    }

    /** Keeps the profile's ward (used for ward polls) in step with the Civic Hub. */
    private fun saveWardToProfile() {
        val label = myWard?.ward?.profileLabel ?: return
        if (profile.ward != label) updateProfile(profile.copy(ward = label))
    }

    /** Headlines from city news sites (live cities only). */
    var webNews by mutableStateOf<List<WebStory>>(emptyList())
    var webNewsLoading by mutableStateOf(false)
    var webNewsUpdatedMs by mutableStateOf(0L)
    private var webNewsJob: Job? = null
    private var webNewsCity: String? = null

    /** Fetches the latest city headlines. Called on city load and on pull-to-refresh. */
    fun refreshWebNews() {
        if (!isLive) { webNews = emptyList(); return }
        val cityId = city.id
        webNewsJob?.cancel()
        webNewsJob = viewModelScope.launch {
            webNewsLoading = true
            when (val r = backend.webNews(cityId)) {
                is Res.Ok -> { webNews = r.value; webNewsCity = cityId; webNewsUpdatedMs = System.currentTimeMillis() }
                is Res.Err -> if (webNews.isEmpty()) toast("Couldn't load city news. Pull down to try again.")
            }
            webNewsLoading = false
        }
    }

    /** The ward poll to show, or null when there is no open poll. */
    var poll by mutableStateOf<PollUi?>(null)
        private set

    val issues = mutableStateListOf<CivicIssue>()

    /** Sample-mode only: events and reports the user made, kept per city. */
    private val hostedByCity = HashMap<String, MutableList<CityEvent>>()
    private val reportsByCity = HashMap<String, MutableList<CivicIssue>>()

    fun selectCity(c: City) {
        if (!isLive) reportsByCity[city.id] = issues.filter { it.isMine }.toMutableList()
        CurrentCity.city = c
        prefs.cityId = c.id
        hasCity = true
        pickingCity = false
        spots = SampleData.spots
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
        weatherDetail = null
        recenterTick++
        loadContent()
        refreshWeather()
        refreshTrending()
    }

    private var contentJob: Job? = null

    /** Fills events, reports, news, the alert and the poll for the current city. */
    fun loadContent() {
        contentJob?.cancel()
        clearActivity()
        if (!isLive) {
            events.clear(); events.addAll(hostedByCity[city.id].orEmpty() + SampleData.events)
            issues.clear(); issues.addAll(reportsByCity[city.id].orEmpty() + SampleData.issues)
            news = SampleData.news
            webNews = emptyList()
            listedEvents = emptyList()
            spots = SampleData.spots
            alert = SampleData.alert
            poll = samplePoll()
            loading = false
            return
        }
        // Clear the previous city's samples straight away so they never show as live content.
        // Reports and events filed in the last minute may not be in the server's list yet; keep them.
        val pendingIssues = issues.filter { it.isMine && it.reportedAgo == "Just now" }
        val pendingEvents = events.filter { it.hostedByYou }
        events.clear(); issues.clear(); news = emptyList(); alert = null; poll = null; serviceStatus = emptyList()
        spots = emptyList() // nearby places come from the live "trending" lookup instead
        if (webNewsCity != city.id) webNews = emptyList() // never show another city's headlines
        refreshWebNews()
        refreshListedEvents()
        refreshWards()
        val cityId = city.id
        val uid = sb.userId
        contentJob = viewModelScope.launch {
            loading = true
            val here = userPlace
            val iss = async { backend.issues(cityId, uid, here.lat, here.lng) }
            val evs = async { backend.events(cityId, uid, here.lat, here.lng) }
            val nws = async { backend.news(cityId) }
            val alr = async { backend.alert(cityId) }
            val pol = async { backend.poll(cityId, profile.ward) }
            val sts = async { backend.serviceStatus(cityId) }
            val mine = uid?.let { async { backend.myActivity(it) } }
            var failed: String? = null
            when (val r = iss.await()) {
                is Res.Ok -> { val ids = r.value.map { it.id }.toSet(); issues.clear(); issues.addAll(pendingIssues.filter { it.id !in ids } + r.value) }
                is Res.Err -> { issues.addAll(0, pendingIssues); failed = r.message }
            }
            when (val r = evs.await()) {
                is Res.Ok -> { val ids = r.value.map { it.id }.toSet(); events.clear(); events.addAll(pendingEvents.filter { it.id !in ids } + r.value) }
                is Res.Err -> { events.addAll(0, pendingEvents); failed = r.message }
            }
            sts.await().valueOrNull()?.let { serviceStatus = it }
            when (val r = nws.await()) { is Res.Ok -> news = r.value; is Res.Err -> failed = r.message }
            when (val r = alr.await()) { is Res.Ok -> alert = r.value; is Res.Err -> failed = r.message }
            when (val r = pol.await()) { is Res.Ok -> poll = r.value; is Res.Err -> failed = r.message }
            mine?.await()?.let { a ->
                myResponses.putAll(a.responses)
                a.savedEvents.forEach { savedEvents[it] = true }
                bookings.putAll(a.bookings)
                bookingCodes.putAll(a.bookingCodes)
                a.endorsed.forEach { endorsedNews[it] = true }
                // The server's count already includes the user's own endorsement; the screen adds it back.
                if (a.endorsed.isNotEmpty()) news = news.map { if (it.id in a.endorsed) it.copy(endorsements = (it.endorsements - 1).coerceAtLeast(0)) else it }
                a.savedNews.forEach { savedNews[it] = true }
                a.claimed.forEach { claimedOffers[it] = true }
                poll?.let { p -> p.id?.let { a.votes[it] }?.let { opt -> pollChoice = p.options.indexOfFirst { it.id == opt }.takeIf { it >= 0 } } }
                alertAcknowledged = alert?.id?.let { it in a.acked } == true
            }
            loading = false
            failed?.let { toast(it) }
        }
    }

    private fun clearActivity() {
        savedEvents.clear(); bookings.clear(); bookingCodes.clear(); myResponses.clear(); respondingTo.clear(); endorsedNews.clear()
        savedNews.clear(); claimedOffers.clear(); pollChoice = null; alertAcknowledged = false
        helpChat.clear()
    }

    /** The Help & FAQs conversation. Kept only while the app is open; never stored or sent anywhere. */
    val helpChat = mutableStateListOf<HelpMessage>()

    fun addEvent(
        title: String, category: EventCategory, dateLabel: String, timeLabel: String, bucket: DayBucket,
        venue: Place, priceRupees: Int, imageUri: String?, description: String,
        /** When the event starts; needed for live cities. */
        startsAtMillis: Long? = null,
    ): String {
        val live = isLive && sb.signedIn && startsAtMillis != null
        val id = if (live) UUID.randomUUID().toString() else "mine-${System.currentTimeMillis()}"
        val e = CityEvent(
            id = id, title = title.trim(), category = category, dateLabel = dateLabel, timeLabel = timeLabel,
            venue = venue.name, area = venue.area.substringBefore(",").ifBlank { city.name }, priceRupees = priceRupees,
            imageRes = CityBackend.fallbackImage(category), interested = if (live) 0 else 1,
            distanceKm = Transit.distanceKm(userPlace.lat, userPlace.lng, venue.lat, venue.lng),
            description = description.trim().ifBlank { "Hosted by ${profile.name.ifBlank { "a Shehar Saathi neighbour" }}." },
            bucket = bucket, lat = venue.lat, lng = venue.lng, pinLabel = title.trim().take(18),
            imageUri = imageUri, hostedByYou = true,
        )
        events.add(0, e)
        if (!live) {
            hostedByCity.getOrPut(city.id) { mutableListOf() }.add(0, e)
            savedEvents[id] = true
            return id
        }
        val uid = sb.userId ?: return id
        val cityId = city.id
        viewModelScope.launch {
            val photo = photoForUpload(app, imageUri)
            val r = backend.addEvent(
                id, cityId, uid, e.title, category, e.venue, e.area, e.lat, e.lng, startsAtMillis ?: return@launch,
                priceRupees, e.description, photo,
            )
            if (r is Res.Err) {
                events.removeAll { it.id == id }
                toast("Event not published: ${r.message}")
            }
        }
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

    /** Full forecast for the weather screen. */
    var weatherDetail by mutableStateOf<WeatherDetail?>(null)
        private set
    var weatherDetailLoading by mutableStateOf(false)
        private set
    private var weatherDetailJob: Job? = null

    fun loadWeatherDetail() {
        weatherDetailJob?.cancel()
        weatherDetailJob = viewModelScope.launch {
            weatherDetailLoading = true
            val p = userPlace
            val d = WeatherService.detail(p.lat, p.lng)
            if (d != null) { weatherDetail = d; weather = d.now } // keep the header chip in step
            else if (weatherDetail == null) toast("Couldn't load the forecast. Check your connection and try again.")
            weatherDetailLoading = false
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


    // ---- Saved places, listed events and headlines. Kept on this phone, separately for each account,
    // so they survive closing the app and signing out and back in.
    val savedItems = mutableStateListOf<SavedItem>()
    private val savedAccount: String get() = sb.userId ?: "local"

    private fun loadSaved() {
        savedItems.clear()
        savedItems.addAll(SavedItems.decode(prefs.savedJson(savedAccount)))
    }

    private fun toggleSaved(item: SavedItem, what: String) {
        val next = SavedItems.toggle(savedItems, item)
        val added = next.size > savedItems.size
        savedItems.clear(); savedItems.addAll(next)
        prefs.setSavedJson(savedAccount, SavedItems.encode(next))
        Toast.makeText(app, if (added) "$what saved. Find it in your Profile." else "$what removed from saved.", Toast.LENGTH_SHORT).show()
    }

    fun isSaved(kind: SavedKind, key: String) = SavedItems.contains(savedItems, kind, key)
    fun saved(kind: SavedKind): List<SavedItem> = savedItems.filter { it.kind == kind }

    fun isPlaceSaved(p: Place) = isSaved(SavedKind.PLACE, SavedItems.placeKey(p.lat, p.lng))
    fun toggleSavedPlace(p: Place) = toggleSaved(SavedItems.ofPlace(p), "Place")
    fun isListedSaved(e: ListedEvent) = isSaved(SavedKind.EVENT, e.id.toString())
    fun toggleSavedListed(e: ListedEvent) = toggleSaved(SavedItems.ofListed(e), "Event")
    fun isStorySaved(s: WebStory) = isSaved(SavedKind.STORY, s.id.toString())
    fun toggleSavedStory(s: WebStory) = toggleSaved(SavedItems.ofStory(s), "Story")
    fun removeSaved(item: SavedItem) = toggleSaved(item, when (item.kind) { SavedKind.PLACE -> "Place"; SavedKind.EVENT -> "Event"; SavedKind.STORY -> "Story" })

    /** A dropped pin's address arrived after it was saved: keep the bookmark, show the real name. */
    fun renameSavedPlace(p: Place) {
        val key = SavedItems.placeKey(p.lat, p.lng)
        val i = savedItems.indexOfFirst { it.kind == SavedKind.PLACE && it.key == key }
        if (i < 0) return
        savedItems[i] = SavedItems.ofPlace(p, savedItems[i].savedAtMs)
        prefs.setSavedJson(savedAccount, SavedItems.encode(savedItems))
    }

    /** A place the Profile asked Explore to show on the map; Explore clears it once shown. */
    var placeToShow by mutableStateOf<Place?>(null)

    init { loadSaved() }

    val savedEvents = mutableStateMapOf<String, Boolean>()
    val bookings = mutableStateMapOf<String, Int>()
    /** Booking codes from the server, by event id (live cities). */
    val bookingCodes = mutableStateMapOf<String, String>()
    /** The user's own response to each report they have responded to, by report id. */
    val myResponses = mutableStateMapOf<String, IssueResponse>()
    /** Reports whose response is being sent, so a second tap waits for the first. */
    val respondingTo = mutableStateMapOf<String, Boolean>()
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

    init {
        // Searches lean towards wherever the user is (or the spot they picked).
        viewModelScope.launch {
            androidx.compose.runtime.snapshotFlow { userPlace }.collect { Geocoder.biasLat = it.lat; Geocoder.biasLng = it.lng }
        }
    }
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
        if (chosenWardNo == null) saveWardToProfile()
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
        // We know where the user is now; the area name is filled in when the lookup returns.
        locating = false
        viewModelScope.launch {
            val named = Geocoder.reverse(lat, lng)
            if (named != null && locationSource == LocationSource.GPS && userPlace.lat == lat) {
                setUserPlace(named.copy(lat = lat, lng = lng), LocationSource.GPS)
            }
        }
    }

    private var gpsJob: Job? = null

    /** Gets a one-shot GPS fix. Safe to call repeatedly; a request already running is reused. */
    fun locateNow() {
        if (gpsJob?.isActive == true) return
        locating = true
        gpsJob = viewModelScope.launch {
            try {
                val loc = LocationService.currentLocation(app)
                if (loc == null) gpsFailed("Couldn't get a GPS fix. Try again outdoors or pick your area.")
                else applyGpsFix(loc.latitude, loc.longitude)
            } finally {
                if (locating && locationSource != LocationSource.GPS) locating = false
            }
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

    // ---------- Ward poll ----------
    var pollChoice by mutableStateOf<Int?>(null)
        private set

    val pollTotal: Int get() = poll?.options?.sumOf { it.votes } ?: 0

    fun pollPercent(index: Int): Int {
        val opts = poll?.options ?: return 0
        val total = opts.sumOf { it.votes }.coerceAtLeast(1)
        return Math.round((opts.getOrNull(index)?.votes ?: 0) * 100f / total)
    }

    fun vote(index: Int) {
        val p = poll ?: return
        if (pollChoice != null || index !in p.options.indices) return
        poll = p.copy(options = p.options.mapIndexed { i, o -> if (i == index) o.copy(votes = o.votes + 1) else o })
        pollChoice = index
        val pollId = p.id ?: return
        val optionId = p.options[index].id ?: return
        val uid = sb.userId ?: return
        viewModelScope.launch {
            val r = backend.vote(pollId, optionId, uid)
            if (r is Res.Err) {
                poll = p; pollChoice = null
                toast("Vote not counted: ${r.message}")
            }
        }
    }

    private fun samplePoll() = PollUi(
        id = null,
        title = "${myWard?.ward?.label ?: profile.ward.ifBlank { "Your ward" }} Infra Vote",
        detail = "₹1.2 Cr ward fund — help prioritise the next project",
        endsLabel = "Ends in 3 days",
        options = listOf(
            PollOptionUi(null, "Footpath Widening & Tactile Paving", 948),
            PollOptionUi(null, "LED Streetlights & CCTV Grid", 534),
        ),
    )

    val reportsValidated: Int get() = (if (isLive) 0 else 54) + issues.count { it.isMine }
    /** The user's own reports, newest first. */
    val myIssues: List<CivicIssue> get() = issues.filter { it.isMine }.let { if (isLive) it else it.reversed() }

    /** Figures for the Civic Hub, worked out from the city's real reports (live cities). */
    data class CivicStats(val total: Int, val resolved: Int, val open: Int, val potholesFixed: Int, val confirmations: Int) {
        val resolutionRate: Float get() = if (total == 0) 0f else resolved.toFloat() / total
    }

    val civicStats: CivicStats
        get() {
            val resolved = issues.filter { it.status == IssueStatus.RESOLVED }
            return CivicStats(
                total = issues.size, resolved = resolved.size, open = issues.size - resolved.size,
                potholesFixed = resolved.count { it.category == IssueCategory.POTHOLE },
                confirmations = issues.sumOf { it.upvotes },
            )
        }

    /** Title and badge for the citizen card, based on how many reports the user has filed. */
    val citizenTier: Pair<String, String>
        get() {
            if (!isLive) return "Citizen Tier 3" to "Neighborhood Guardian"
            val n = myIssues.size
            return when {
                n >= 20 -> "Citizen Tier 3" to "Neighbourhood Guardian"
                n >= 5 -> "Citizen Tier 2" to "Ward Watcher"
                n >= 1 -> "Citizen Tier 1" to "Active Neighbour"
                else -> "New neighbour" to "File your first report"
            }
        }

    fun event(id: String) = events.firstOrNull { it.id == id }
    fun issue(id: String) = issues.firstOrNull { it.id == id }

    fun toggleSaveEvent(id: String) {
        val on = savedEvents[id] != true
        if (on) savedEvents[id] = true else savedEvents.remove(id)
        syncMine("event_saves", "event_id", id, on) { if (on) savedEvents.remove(id) else savedEvents[id] = true }
    }

    fun book(id: String, qty: Int) {
        if (isLive && (bookings[id] ?: 0) > 0) { toast("You already have a booking for this event."); return }
        bookings[id] = (bookings[id] ?: 0) + qty
        if (!isLive || !sb.signedIn) return
        viewModelScope.launch {
            when (val r = backend.book(id, qty)) {
                is Res.Ok -> {
                    bookings[id] = r.value.quantity
                    if (r.value.bookingCode.isNotBlank()) bookingCodes[id] = r.value.bookingCode
                    toast("Booked! Your booking ID is ${r.value.bookingCode}.")
                }
                is Res.Err -> {
                    val left = (bookings[id] ?: 0) - qty
                    if (left > 0) bookings[id] = left else bookings.remove(id)
                    toast(r.message)
                }
            }
        }
    }

    /**
     * The user taps a public response on a report ("I see this too", "Still not fixed", "It's getting
     * worse", "It's fixed now"). Tapping the response they already gave takes it back.
     */
    fun respondToIssue(id: String, tapped: IssueResponse) {
        val idx = issues.indexOfFirst { it.id == id }
        if (idx < 0 || respondingTo[id] == true) return
        val item = issues[idx]
        if (tapped !in IssueResponses.choices(item)) {
            toast(if (item.isMine) "You reported this one. You can say if it's still there, worse or fixed." else "This report is marked resolved.")
            return
        }
        val old = myResponses[id]
        val new = IssueResponses.next(old, tapped)
        fun show(from: IssueResponse?, to: IssueResponse?) {
            val j = issues.indexOfFirst { it.id == id }
            if (j >= 0) issues[j] = IssueResponses.apply(issues[j], from, to)
            if (to == null) myResponses.remove(id) else myResponses[id] = to
        }
        show(old, new)
        if (!isLive) return
        if (sb.userId == null) { show(new, old); toast("Please sign in to respond to a report."); return }
        respondingTo[id] = true
        viewModelScope.launch {
            when (val r = backend.respond(id, new)) {
                is Res.Ok -> r.value?.let { t ->
                    // The server's totals include everyone else's responses since the list was loaded.
                    val j = issues.indexOfFirst { it.id == id }
                    if (j >= 0) issues[j] = issues[j].copy(
                        upvotes = t.upvoteCount,
                        responses = ResponseCounts(seen = t.seenCount, stillThere = t.stillCount, worse = t.worseCount, fixed = t.fixedCount),
                        lastResponse = IssueResponse.of(t.lastResponseKind),
                        lastResponseAgo = t.lastResponseAt?.let { Times.ago(Times.parse(it)) },
                    )
                }
                is Res.Err -> { show(new, old); toast(r.message) }
            }
            respondingTo.remove(id)
        }
    }

    fun toggleEndorse(id: String) {
        val on = endorsedNews[id] != true
        if (on) endorsedNews[id] = true else endorsedNews.remove(id)
        syncMine("news_endorsements", "news_id", id, on) { if (on) endorsedNews.remove(id) else endorsedNews[id] = true }
    }

    fun toggleSaveNews(id: String) {
        val on = savedNews[id] != true
        if (on) savedNews[id] = true else savedNews.remove(id)
        syncMine("news_saves", "news_id", id, on) { if (on) savedNews.remove(id) else savedNews[id] = true }
    }

    fun claimOffer(id: String) {
        if (claimedOffers[id] == true) return
        claimedOffers[id] = true
        syncMine("offer_claims", "news_id", id, true) { claimedOffers.remove(id) }
    }

    fun acknowledgeAlert() {
        alertAcknowledged = !alertAcknowledged
        val id = alert?.id ?: return
        val on = alertAcknowledged
        syncMine("alert_acknowledgements", "alert_id", id, on) { alertAcknowledged = !on }
    }

    /** Saves a toggle to the server for live cities; [undo] runs if the server refuses it. */
    private fun syncMine(table: String, column: String, key: String, on: Boolean, undo: () -> Unit) {
        if (!isLive) return
        val uid = sb.userId ?: return
        viewModelScope.launch {
            val r = backend.setMine(table, column, key, uid, on)
            if (r is Res.Err) { undo(); toast(r.message) }
        }
    }

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
        val live = isLive && sb.signedIn
        val id = if (live) UUID.randomUUID().toString() else "report-${System.currentTimeMillis()}"
        val title = when (category) {
            IssueCategory.POTHOLE -> "Pothole reported"
            IssueCategory.STREETLIGHT -> "Streetlight outage"
            IssueCategory.GARBAGE -> "Garbage dump reported"
            IssueCategory.WATER -> "Water / drainage issue"
        } + " • " + location.substringBefore(",")
        val jitter = ((issues.size % 5) - 2) * 0.0009
        val issue = CivicIssue(
            id = id,
            title = title,
            category = category,
            location = location,
            description = description.trim(),
            authorities = authorities.ifEmpty { listOf("@SheharSaathi") },
            urgency = urgency,
            status = IssueStatus.REPORTED,
            upvotes = 0,
            imageUri = imageUri,
            reportedAgo = "Just now",
            isMine = true,
            lat = lat ?: (userPlace.lat + jitter),
            lng = lng ?: (userPlace.lng - jitter),
        )
        if (live) issues.add(0, issue) else issues.add(issue)
        if (!live) return id

        val uid = sb.userId ?: return id
        val cityId = city.id
        viewModelScope.launch {
            val photo = photoForUpload(app, imageUri)
            val r = backend.submitIssue(
                id, cityId, uid, category, title, issue.description, urgency, location, issue.lat, issue.lng,
                photo, photoIsLive = false,
            )
            if (r is Res.Err) {
                val i = issues.indexOfFirst { it.id == id }
                if (i >= 0) issues[i] = issues[i].copy(authorityNote = "Not sent: ${r.message}")
                toast("Report not sent: ${r.message}")
            }
        }
        return id
    }

    fun toast(message: String) {
        Toast.makeText(app, message, Toast.LENGTH_LONG).show()
    }

    init {
        if (signedIn) {
            syncAccount()
            loadContent()
        }
        if (hasCity) {
            refreshWeather()
            refreshTrending()
        }
    }
}
