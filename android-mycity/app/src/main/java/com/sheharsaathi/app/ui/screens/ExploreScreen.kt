package com.sheharsaathi.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.layout.offset
import com.sheharsaathi.app.data.CityViewModel
import com.sheharsaathi.app.data.CurrentCity
import com.sheharsaathi.app.data.ExploreFilter
import com.sheharsaathi.app.data.IssueStatus
import com.sheharsaathi.app.data.Spot
import com.sheharsaathi.app.ui.components.CityCard
import com.sheharsaathi.app.ui.components.CityMap
import com.sheharsaathi.app.data.Place
import com.sheharsaathi.app.data.PlaceKind
import com.sheharsaathi.app.data.Transit
import com.sheharsaathi.app.ui.components.LineBadge
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.sheharsaathi.app.ui.components.LocationHeader
import com.sheharsaathi.app.ui.components.MapPinUi
import com.sheharsaathi.app.ui.components.SectionHeader
import com.sheharsaathi.app.ui.components.SoftBadge
import com.sheharsaathi.app.ui.components.StatusBadge
import com.sheharsaathi.app.ui.components.TagLabel
import com.sheharsaathi.app.ui.components.compactCount
import com.sheharsaathi.app.ui.components.openDirections
import com.sheharsaathi.app.ui.components.shareText
import com.sheharsaathi.app.ui.components.rememberMapController
import com.sheharsaathi.app.ui.components.rememberLocateMe
import com.sheharsaathi.app.ui.components.rememberPlaceSearch
import com.sheharsaathi.app.ui.components.LocationNudge
import com.sheharsaathi.app.ui.components.PlaceResultRow
import com.sheharsaathi.app.ui.components.SearchingRow
import com.sheharsaathi.app.ui.components.placeIcon
import com.sheharsaathi.app.ui.components.CitySearchField
import com.sheharsaathi.app.data.Geocoder
import com.sheharsaathi.app.data.LineColors
import com.sheharsaathi.app.data.LocationService
import com.sheharsaathi.app.data.LocationSource
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.shadow
import kotlinx.coroutines.launch
import com.sheharsaathi.app.ui.components.rupees
import com.sheharsaathi.app.ui.theme.CityColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    vm: CityViewModel,
    onOpenEvent: (String) -> Unit,
    onOpenIssue: (String) -> Unit,
    onSeeAllEvents: () -> Unit,
    onSeeAllIssues: () -> Unit,
    onPlanTrip: () -> Unit,
    onOpenBoard: () -> Unit,
    onSeeNearby: () -> Unit = {},
    onSeeAllNews: () -> Unit = {},
) {
    val context = LocalContext.current
    val startSearch = {
        vm.tripFrom = vm.userPlace
        vm.tripTo = null
        onPlanTrip()
    }
    val planTo = { place: Place ->
        vm.tripFrom = vm.userPlace
        vm.planTo(place)
        onPlanTrip()
    }
    val haptic = LocalHapticFeedback.current
    val mapController = rememberMapController()
    val locateMe = rememberLocateMe(vm)
    val scope = rememberCoroutineScope()
    var selectedSpot by remember { mutableStateOf<Spot?>(null) }
    var mapPlace by remember { mutableStateOf<Place?>(null) }
    var showMapSearch by remember { mutableStateOf(false) }

    // Auto-locate when permission was granted before
    LaunchedEffect(Unit) {
        if (vm.locationSource == LocationSource.DEFAULT && !vm.locating && LocationService.hasPermission(context)) locateMe()
    }
    LaunchedEffect(vm.recenterTick) {
        if (vm.recenterTick > 0) mapController.flyTo(vm.userPlace.lat, vm.userPlace.lng, 15.5)
    }
    val showPlace = { p: Place ->
        mapPlace = p
        mapController.flyTo(p.lat, p.lng, 16.5)
    }
    // A saved place tapped in the Profile opens here on the map.
    LaunchedEffect(vm.placeToShow) {
        vm.placeToShow?.let { showPlace(it); vm.placeToShow = null }
    }
    val dropPin = { lat: Double, lng: Double ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val provisional = Place("pin-${System.currentTimeMillis()}", "Dropped pin", "Finding address…", lat, lng, PlaceKind.LANDMARK)
        mapPlace = provisional
        scope.launch {
            val named = Geocoder.reverse(lat, lng)
            if (mapPlace?.id == provisional.id) {
                mapPlace = named?.copy(id = provisional.id) ?: provisional.copy(area = "%.5f, %.5f".format(lat, lng))
            }
        }
        Unit
    }
    val filter = vm.exploreFilter
    val query = vm.exploreQuery.trim()
    val feed = remember(vm.userPlace, vm.locationLabel, vm.events, vm.listedEvents, vm.issues, vm.webNews) {
        com.sheharsaathi.app.data.NearbyFeed.build(vm.userPlace, CurrentCity.city.name, vm.locationLabel, vm.events, vm.listedEvents, vm.issues, vm.webNews)
    }
    var openListed by remember { mutableStateOf<Long?>(null) }
    val feedActions = FeedActions(onOpenEvent, onOpenIssue, { openListed = it }, { openStory(context, it) })

    fun matches(text: String) = query.isEmpty() || text.contains(query, ignoreCase = true)
    val showEvents = filter == ExploreFilter.ALL || filter == ExploreFilter.EVENTS
    val showCivic = filter == ExploreFilter.ALL || filter == ExploreFilter.CIVIC
    val showFood = filter == ExploreFilter.ALL || filter == ExploreFilter.FOOD

    val pins = buildList {
        vm.issues.forEach { issue ->
            val resolved = issue.status == IssueStatus.RESOLVED
            add(
                MapPinUi(
                    id = issue.id, lat = issue.lat, lng = issue.lng,
                    label = if (resolved) "Fixed" else issue.category.label.substringBefore(" "),
                    sub = if (resolved) issue.location.substringAfterLast(", ").take(18) else "${issue.upvotes} ✓",
                    color = if (resolved) CityColors.Green else CityColors.Red,
                    icon = if (resolved) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    visible = showCivic && matches(issue.title + issue.location),
                    onClick = { onOpenIssue(issue.id) },
                )
            )
        }
        vm.events.forEach { e ->
            add(
                MapPinUi(
                    id = e.id, lat = e.lat, lng = e.lng, label = e.pinLabel.substringBeforeLast(" "),
                    sub = e.pinLabel.substringAfterLast(" "), color = Color(0xFF8B5CF6), icon = Icons.Filled.MusicNote,
                    visible = showEvents && matches(e.title + e.venue), onClick = { onOpenEvent(e.id) },
                )
            )
        }
        vm.trending.forEach { t ->
            add(
                MapPinUi(
                    id = "t-" + t.id, lat = t.lat, lng = t.lng, label = t.name, sub = t.kind.tag.lowercase(),
                    color = CityColors.Saffron, icon = trendIcon(t.kind),
                    visible = showFood && matches(t.name),
                    onClick = { showPlace(Place("trend-${t.id}", t.name, "%.1f km away".format(t.distanceKm), t.lat, t.lng, PlaceKind.LANDMARK)) },
                )
            )
        }
        vm.spots.forEach { s ->
            add(
                MapPinUi(
                    id = s.id, lat = s.lat, lng = s.lng, label = s.name, sub = s.sub.substringBefore(" •"),
                    color = CityColors.Saffron, icon = Icons.Filled.Restaurant,
                    visible = showFood && matches(s.name + s.sub), onClick = { selectedSpot = s },
                )
            )
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val peek = maxHeight * 0.44f
        val sheetMax = maxHeight * 0.86f
        val scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(skipHiddenState = true)
        )
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = peek,
            sheetContainerColor = CityColors.Canvas,
            sheetShadowElevation = 12.dp,
            sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = CityColors.Canvas,
            sheetContent = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(sheetMax),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "commute") {
                        CommuteStrip(
                            onHome = { planTo(Transit.place("home")!!) },
                            onWork = { planTo(Transit.place("work")!!) },
                            onBoard = onOpenBoard,
                            live = vm.isLive,
                        )
                    }
                    // Four sideways rows: what's near the user, civic reports, events, news. Each has See all.
                    fun keep(list: List<com.sheharsaathi.app.data.FeedItem>) = list.filter { matches(it.text) }
                    if (showCivic && vm.alert?.let { matches(it.title + it.body) } == true) {
                        item(key = "alert") {
                            AuthorityAlertCard(vm) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.acknowledgeAlert()
                            }
                        }
                    }
                    if (filter == ExploreFilter.ALL || filter == ExploreFilter.EVENTS) item(key = "near") {
                        FeedSection(
                            "Happening near ${feed.area}", keep(feed.happening).take(12),
                            if (query.isEmpty()) "Nothing listed within ${com.sheharsaathi.app.data.NearbyFeed.RADIUS_KM.toInt()} km of ${feed.area} yet. The rows below cover the whole city."
                            else "Nothing near you matches \"$query\".",
                            feedActions, onSeeNearby,
                        )
                    }
                    if (showCivic) item(key = "civic") {
                        // Open reports, nearest first (each card shows how far it is).
                        FeedSection(
                            "Civic issues near ${feed.area}", keep(feed.issues).take(10),
                            if (query.isEmpty()) "No open civic reports. Spot a pothole or broken streetlight? Use the Report tab." else "No reports match \"$query\".",
                            feedActions, onSeeAllIssues,
                        )
                    }
                    if (showEvents) item(key = "events") {
                        // Events within reach of the area; if there are none, say so in the heading and show the city.
                        val near = keep(feed.eventsNear)
                        FeedSection(
                            if (near.isNotEmpty()) "Events near ${feed.area}" else "Events in ${CurrentCity.city.name}",
                            (near.ifEmpty { keep(feed.events) }).take(12),
                            if (query.isEmpty()) "No upcoming events yet. Host one from the Events tab." else "No events match \"$query\".",
                            feedActions, onSeeAllEvents,
                        )
                    }
                    if (filter == ExploreFilter.ALL) item(key = "news") {
                        // Stories that name the area lead; the rest of the row is the latest city news.
                        val local = feed.news.any { it.local }
                        FeedSection(
                            if (local) "News near ${feed.area}" else "News in ${CurrentCity.city.name}", keep(feed.news).take(10),
                            if (query.isEmpty()) (if (vm.isLive) "City news is loading." else "City news is coming to ${CurrentCity.city.name}.") else "No news matches \"$query\".",
                            feedActions, onSeeAllNews,
                        )
                    }
                    if (showFood) {
                        val spots = vm.spots.filter { matches(it.name + it.sub) }
                        val live = vm.trending.filter { matches(it.name) }
                        val todays = vm.events.filter { it.bucket == com.sheharsaathi.app.data.DayBucket.TODAY && matches(it.title + it.venue) }.take(3)
                        if (spots.isNotEmpty() || live.isNotEmpty() || todays.isNotEmpty() || vm.trendingLoading) {
                            item(key = "spots-h") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.weight(1f)) { SectionHeader("Trending Spots Today") }
                                    if (live.isNotEmpty()) {
                                        SoftBadge("LIVE", CityColors.GreenSoft, CityColors.GreenText, modifier = Modifier.padding(end = 20.dp))
                                    }
                                }
                            }
                            item(key = "spots") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    if (vm.trendingLoading && live.isEmpty()) {
                                        items(3) { TrendingPlaceholder() }
                                    }
                                    items(live, key = { "t-" + it.id }) { t ->
                                        TrendingCard(t) {
                                            showPlace(Place("trend-${t.id}", t.name, "${t.kind.tag.lowercase().replaceFirstChar { it.uppercase() }} • ${"%.1f".format(t.distanceKm)} km away", t.lat, t.lng, PlaceKind.LANDMARK))
                                            scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                                        }
                                    }
                                    items(todays, key = { "e-" + it.id }) { e -> TodayEventCard(e) { onOpenEvent(e.id) } }
                                    items(spots, key = { it.id }) { s -> SpotCard(s) { selectedSpot = s } }
                                }
                            }
                        }
                    }
                }
            },
        ) { _ ->
            Column(Modifier.fillMaxSize().background(CityColors.Canvas)) {
                Column(Modifier.statusBarsPadding().padding(bottom = 10.dp)) {
                    LocationHeader(
                        title = vm.locationLabel,
                        caption = when (vm.locationSource) {
                            LocationSource.GPS -> "You are here"
                            LocationSource.MANUAL -> "Your area"
                            LocationSource.DEFAULT -> "Showing"
                        },
                        gps = vm.locationSource == LocationSource.GPS,
                        locating = vm.locating,
                        vm = vm,
                        onClick = { vm.showLocationPicker = true },
                    )
                    WhereToBar(onClick = startSearch, onGo = { planTo(Transit.quickGo) })
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 10.dp),
                    ) {
                        items(ExploreFilter.entries) { f ->
                            val icon = when (f) {
                                ExploreFilter.ALL -> Icons.Filled.Place
                                ExploreFilter.EVENTS -> Icons.Filled.MusicNote
                                ExploreFilter.CIVIC -> Icons.Filled.Warning
                                ExploreFilter.FOOD -> Icons.Filled.Restaurant
                            }
                            FilterChip(
                                selected = vm.exploreFilter == f,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    vm.exploreFilter = f
                                },
                                label = { Text(f.label) },
                                leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = CityColors.Surface,
                                    selectedContainerColor = CityColors.Indigo,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    iconColor = if (f == ExploreFilter.CIVIC) CityColors.Red else CityColors.Indigo,
                                ),
                            )
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    CityMap(
                        pins = pins, controller = mapController,
                        userLat = vm.userPlace.lat, userLng = vm.userPlace.lng,
                        dropped = mapPlace?.let { it.lat to it.lng },
                        onLongPress = dropPin,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Column(
                        Modifier.align(Alignment.TopStart).padding(start = 16.dp, end = 76.dp, top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MapSearchPill(onClick = { showMapSearch = true })
                        val needsNudge = vm.locationNotice != null ||
                            (vm.locationSource == LocationSource.DEFAULT && !vm.locationBannerDismissed && !vm.locating)
                        if (needsNudge) LocationNudge(vm, onAllow = locateMe)
                    }
                    Column(
                        modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                if (vm.locationSource == LocationSource.DEFAULT) locateMe() else vm.recenter()
                            },
                            containerColor = CityColors.Surface,
                            contentColor = CityColors.Indigo,
                        ) { Icon(Icons.Filled.MyLocation, contentDescription = "Go to my location") }
                        SmallFloatingActionButton(onClick = { mapController.zoomIn() }, containerColor = CityColors.Surface, contentColor = CityColors.Ink) {
                            Icon(Icons.Filled.Add, contentDescription = "Zoom in")
                        }
                        SmallFloatingActionButton(onClick = { mapController.zoomOut() }, containerColor = CityColors.Surface, contentColor = CityColors.Ink) {
                            Icon(Icons.Filled.Remove, contentDescription = "Zoom out")
                        }
                        FloatingActionButton(
                            onClick = startSearch,
                            containerColor = CityColors.Indigo,
                            contentColor = Color.White,
                            shape = CircleShape,
                        ) { Icon(Icons.Filled.Directions, contentDescription = "Open directions") }
                    }
                }
                Spacer(Modifier.height(peek - 24.dp))
            }
        }
    }

    if (showMapSearch) {
        MapSearchSheet(
            onDismiss = { showMapSearch = false },
            onPick = { p -> showMapSearch = false; showPlace(p) },
            onPickOnMap = { showMapSearch = false; vm.toast("Long-press the exact spot on the map to drop a pin") },
        )
    }

    mapPlace?.let { place ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val km = Transit.distanceKm(vm.userPlace.lat, vm.userPlace.lng, place.lat, place.lng)
        ModalBottomSheet(onDismissRequest = { mapPlace = null }, sheetState = sheetState, containerColor = CityColors.Surface, scrimColor = Color.Transparent) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(CityColors.SaffronSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Place, null, tint = CityColors.Saffron)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(place.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(place.area, style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { vm.toggleSavedPlace(place) }) { SaveIcon(vm.isPlaceSaved(place), "place", 26) }
                }
                place.note?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = CityColors.Saffron)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftBadge(
                        if (km < 1) "${(km * 1000).toInt()} m from you" else "%.1f km from you".format(km),
                        CityColors.IndigoSoft, CityColors.Indigo, icon = Icons.Filled.Navigation,
                    )
                    place.precision?.let { com.sheharsaathi.app.ui.components.PrecisionTag(it, Modifier.align(Alignment.CenterVertically)) }
                        ?: SoftBadge(CurrentCity.city.name, CityColors.Canvas, CityColors.Muted)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { val p = place; mapPlace = null; planTo(p) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Filled.Directions, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Get me there")
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { vm.setManualLocation(place); mapPlace = null },
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(Icons.Filled.MyLocation, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Set as my location", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { com.sheharsaathi.app.ui.components.openNavigation(context, place.lat, place.lng, place.name) },
                        modifier = Modifier.height(48.dp),
                    ) {
                        Icon(Icons.Filled.Navigation, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Navigate")
                    }
                }
            }
        }
    }

    vm.listedEvents.firstOrNull { it.id == openListed }?.let { e ->
        ListedEventSheet(
            e, saved = vm.isListedSaved(e), onSave = { vm.toggleSavedListed(e) },
            onDismiss = { openListed = null }, onBook = { openStory(context, e.url) },
            onShare = { shareText(context, "${e.title}\n${com.sheharsaathi.app.data.ListedTime.whenLabel(e)}${e.venue?.let { " · $it" } ?: ""}\n${e.url}\n— via Shehar Saathi") },
            onDirections = if (e.lat != null && e.lng != null) ({ openDirections(context, e.lat, e.lng, e.venue ?: e.title) }) else null,
        )
    }

    selectedSpot?.let { spot ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { selectedSpot = null }, sheetState = sheetState, containerColor = CityColors.Surface) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Image(
                    painterResource(spot.imageRes), contentDescription = spot.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(18.dp)),
                )
                Spacer(Modifier.height(14.dp))
                val spotPlace = Place(spot.id, spot.name, spot.sub.substringBefore(" •"), spot.lat, spot.lng, PlaceKind.VENUE)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TagLabel(spot.tag, CityColors.Saffron, Modifier.weight(1f))
                    IconButton(onClick = { vm.toggleSavedPlace(spotPlace) }) { SaveIcon(vm.isPlaceSaved(spotPlace), "place", 26) }
                }
                Text(spot.name, style = MaterialTheme.typography.headlineSmall)
                Text(spot.sub, style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SoftBadge("${spot.rating}", CityColors.GreenSoft, CityColors.Green, icon = Icons.Filled.Star)
                    Spacer(Modifier.width(8.dp))
                    Text(spot.openNote, style = MaterialTheme.typography.labelLarge, color = CityColors.Green)
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        val p = Place(spot.id, spot.name, spot.sub.substringBefore(" •"), spot.lat, spot.lng, PlaceKind.VENUE)
                        selectedSpot = null
                        planTo(p)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Filled.Directions, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Get me there")
                }
            }
        }
    }
}

@Composable
private fun LiveEventCard(
    title: String, category: String, date: String, venue: String, interested: Int, priceRupees: Int,
    imageRes: Int, saved: Boolean, booked: Boolean, onSave: () -> Unit, onOpen: () -> Unit, imageUri: String? = null,
) {
    CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), onClick = onOpen) {
        Row(Modifier.padding(10.dp)) {
            Box {
                if (imageUri != null) coil3.compose.AsyncImage(
                    imageUri, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 112.dp, height = 132.dp).clip(RoundedCornerShape(14.dp)),
                ) else Image(
                    painterResource(imageRes), contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 112.dp, height = 132.dp).clip(RoundedCornerShape(14.dp)),
                )
                Text(
                    if (priceRupees == 0) "Free" else rupees(priceRupees),
                    style = MaterialTheme.typography.labelMedium, color = Color.White,
                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    TagLabel(category, CityColors.Saffron, Modifier.weight(1f).padding(top = 4.dp))
                    IconButton(onClick = onSave, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (saved) "Remove bookmark" else "Bookmark",
                            tint = CityColors.Indigo,
                        )
                    }
                }
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(date, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                Text(venue, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${compactCount(interested)} interested", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, modifier = Modifier.weight(1f))
                    if (booked) {
                        SoftBadge("Booked", CityColors.GreenSoft, CityColors.Green, icon = Icons.Filled.CheckCircle)
                    } else {
                        Button(onClick = onOpen, contentPadding = PaddingValues(horizontal = 14.dp), modifier = Modifier.height(36.dp)) {
                            Text("Book Tickets", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorityAlertCard(vm: CityViewModel, onAck: () -> Unit) {
    val a = vm.alert ?: return
    val acked = vm.alertAcknowledged
    CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), borderColor = CityColors.Saffron.copy(alpha = 0.5f)) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(CityColors.Saffron))
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SoftBadge("${a.authority} Verified", CityColors.SaffronSoft, CityColors.SaffronText, icon = Icons.Filled.CheckCircle)
                    Spacer(Modifier.width(8.dp))
                    Text(a.window, style = MaterialTheme.typography.labelMedium, color = CityColors.Saffron, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WaterDrop, null, tint = CityColors.Saffron, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(a.title, style = MaterialTheme.typography.titleMedium)
                }
                Text(a.body, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a.impact, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = onAck, contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.height(34.dp)) {
                        Icon(if (acked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Acknowledge (${a.acknowledgements + if (acked) 1 else 0})", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

private fun trendIcon(k: com.sheharsaathi.app.data.TrendKind): ImageVector = when (k) {
    com.sheharsaathi.app.data.TrendKind.LANDMARK -> Icons.Filled.AccountBalance
    com.sheharsaathi.app.data.TrendKind.PARK -> Icons.Filled.Park
    com.sheharsaathi.app.data.TrendKind.MARKET -> Icons.Filled.Storefront
    com.sheharsaathi.app.data.TrendKind.CAFE -> Icons.Filled.LocalCafe
    com.sheharsaathi.app.data.TrendKind.FOOD -> Icons.Filled.Restaurant
}

private fun trendColors(k: com.sheharsaathi.app.data.TrendKind): List<Color> = when (k) {
    com.sheharsaathi.app.data.TrendKind.LANDMARK -> listOf(Color(0xFF3B4C8C), Color(0xFF1E293B))
    com.sheharsaathi.app.data.TrendKind.PARK -> listOf(Color(0xFF166249), Color(0xFF1E293B))
    com.sheharsaathi.app.data.TrendKind.MARKET -> listOf(Color(0xFF8A4A1C), Color(0xFF1E293B))
    com.sheharsaathi.app.data.TrendKind.CAFE -> listOf(Color(0xFF6B4632), Color(0xFF1E293B))
    com.sheharsaathi.app.data.TrendKind.FOOD -> listOf(Color(0xFF8C2F3F), Color(0xFF1E293B))
}

@Composable
private fun TrendingCard(t: com.sheharsaathi.app.data.TrendingPlace, onClick: () -> Unit) {
    CityCard(Modifier.width(188.dp), onClick = onClick) {
        Column {
            Box(Modifier.fillMaxWidth().height(110.dp).background(Brush.linearGradient(trendColors(t.kind)))) {
                Icon(
                    trendIcon(t.kind), null, tint = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier.size(96.dp).align(Alignment.BottomEnd).offset(x = 14.dp, y = 14.dp),
                )
                Icon(trendIcon(t.kind), null, tint = Color.White, modifier = Modifier.padding(12.dp).size(28.dp).align(Alignment.BottomStart))
                SoftBadge(t.kind.tag, CityColors.Glass, CityColors.SaffronText, modifier = Modifier.padding(8.dp))
                SoftBadge(
                    "%.1f km".format(t.distanceKm), CityColors.Glass, Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                )
            }
            Column(Modifier.padding(10.dp)) {
                Text(t.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Popular nearby • from map data", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 1)
                Text("Open on map", style = MaterialTheme.typography.labelMedium, color = CityColors.Indigo)
            }
        }
    }
}

@Composable
private fun TodayEventCard(e: com.sheharsaathi.app.data.CityEvent, onClick: () -> Unit) {
    CityCard(Modifier.width(188.dp), onClick = onClick) {
        Column {
            Box {
                com.sheharsaathi.app.ui.components.EventImage(e, Modifier.fillMaxWidth().height(110.dp))
                SoftBadge("TODAY", CityColors.Saffron, Color.White, modifier = Modifier.padding(8.dp))
            }
            Column(Modifier.padding(10.dp)) {
                Text(e.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${e.venue} • ${"%.1f".format(e.distanceKm)} km", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 1)
                Text(e.timeLabel, style = MaterialTheme.typography.labelMedium, color = CityColors.SaffronText)
            }
        }
    }
}

@Composable
private fun TrendingPlaceholder() {
    val t = rememberInfiniteTransition(label = "shimmer")
    val a by t.animateFloat(0.35f, 0.7f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Box(Modifier.width(188.dp).height(190.dp).clip(RoundedCornerShape(20.dp)).background(CityColors.SurfaceHigh.copy(alpha = a)))
}

@Composable
private fun SpotCard(spot: Spot, onClick: () -> Unit) {
    CityCard(Modifier.width(188.dp), onClick = onClick) {
        Column {
            Box {
                Image(
                    painterResource(spot.imageRes), contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                )
                SoftBadge(
                    "${spot.rating}", CityColors.Green, Color.White, icon = Icons.Filled.Star,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                )
                SoftBadge(spot.tag, CityColors.Glass, CityColors.SaffronText, modifier = Modifier.padding(8.dp))
            }
            Column(Modifier.padding(10.dp)) {
                Text(spot.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(spot.sub, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 1)
                Text(spot.openNote, style = MaterialTheme.typography.labelMedium, color = CityColors.Green)
            }
        }
    }
}

@Composable
private fun WhereToBar(onClick: () -> Unit, onGo: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(54.dp)
            .clip(CircleShape).background(CityColors.SurfaceHigh).border(1.dp, CityColors.Outline, CircleShape)
            .clickable(onClick = onClick).padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = CityColors.Ink)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Where to?", style = MaterialTheme.typography.titleMedium, color = CityColors.Ink)
            Text(CurrentCity.city.let { c -> listOfNotNull(if (c.metroName != null) "Metro" else null, c.busOperator, "autos, bikes").joinToString(", ") + " — compared live" }, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 1)
        }
        Row(
            Modifier.clip(CircleShape).background(CityColors.Indigo).clickable(onClick = onGo).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Navigation, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("GO", style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}

@Composable
private fun CommuteStrip(onHome: () -> Unit, onWork: () -> Unit, onBoard: () -> Unit, live: Boolean = false) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Live cities don't know the user's home or work yet, so no invented trip times.
        if (!live) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val c = CurrentCity.city
            CommuteTile(Icons.Filled.Home, "Home", "18 min • Auto", Modifier.weight(1f), onHome)
            CommuteTile(Icons.Filled.Work, "Work", if (c.metroName != null) "24 min • Metro" else "26 min • Bus", Modifier.weight(1f), onWork)
        }
        CityCard(Modifier.fillMaxWidth(), onClick = onBoard) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Radar, null, tint = CityColors.Indigo, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (live) "Transit nearby" else "Live transit nearby", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        com.sheharsaathi.app.data.LiveBoard.departures.take(3).forEachIndexed { i, d ->
                            LineBadge(d.code, Color(d.color))
                            Text(if (live) "every ${d.headwayMin}m" else "${2 + i * 3} min", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CityColors.Muted)
            }
        }
    }
}

@Composable
private fun CommuteTile(icon: ImageVector, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    CityCard(modifier, onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(CityColors.IndigoSoft), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = CityColors.Indigo, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
            }
        }
    }
}

@Composable
private fun MapSearchPill(onClick: () -> Unit) {
    Row(
        Modifier.shadow(6.dp, CircleShape).clip(CircleShape).background(CityColors.SurfaceHigh)
            .clickable(onClickLabel = "Search the map", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.TravelExplore, null, tint = CityColors.Indigo, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Find any place in ${CurrentCity.city.name}", style = MaterialTheme.typography.labelLarge, color = CityColors.Ink, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapSearchSheet(onDismiss: () -> Unit, onPick: (Place) -> Unit, onPickOnMap: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val (remote, loading) = rememberPlaceSearch(query)
    val q = query.trim()
    val presets = (Transit.places.filter { it.kind != PlaceKind.HOME && it.kind != PlaceKind.WORK } + Transit.areas)
        .filter { q.isEmpty() || it.name.contains(q, true) || it.area.contains(q, true) }
        .take(if (q.isEmpty()) 10 else 6)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CityColors.Surface) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).imePadding()) {
            Text("Find on map", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 20.dp))
            Text("Any street, landmark, shop, colony, or a full address with door number", style = MaterialTheme.typography.bodyMedium,
                color = CityColors.Muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            Spacer(Modifier.height(8.dp))
            CitySearchField(query, { query = it }, "e.g. 8-2-293/82/A, Road No 36, Jubilee Hills")
            LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp)) {
                if (loading) item { SearchingRow() }
                if (remote.isNotEmpty()) {
                    item { TagLabel("On the map", CityColors.Muted, Modifier.padding(start = 20.dp, top = 10.dp, bottom = 4.dp)) }
                    items(remote, key = { "r-" + it.id }) { p -> PlaceResultRow(p, placeIcon(p.kind), CityColors.Indigo) { onPick(p) } }
                }
                if (presets.isNotEmpty()) {
                    item { TagLabel(if (q.isEmpty()) "Popular in ${CurrentCity.city.name}" else "Suggestions", CityColors.Muted, Modifier.padding(start = 20.dp, top = 10.dp, bottom = 4.dp)) }
                    items(presets, key = { "p-" + it.id }) { p -> PlaceResultRow(p, placeIcon(p.kind), CityColors.Saffron) { onPick(p) } }
                }
                if (!loading && q.length >= 3 && remote.isEmpty() && presets.isEmpty()) {
                    item {
                        Text("No places found in ${CurrentCity.city.name} for \u201c$q\u201d. Try the street or area name.", style = MaterialTheme.typography.bodyMedium,
                            color = CityColors.Muted, modifier = Modifier.padding(20.dp))
                    }
                }
                if (q.length >= 3 && !loading) item {
                    // Not every door is on any map; the user can always mark the exact spot themselves.
                    Row(
                        Modifier.fillMaxWidth().clickable(onClick = onPickOnMap).padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(38.dp).clip(CircleShape).background(CityColors.SaffronSoft), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Place, null, tint = CityColors.Saffron, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Can't find the exact door? Mark it on the map", style = MaterialTheme.typography.titleSmall)
                            Text("Close this, then long-press the exact spot to drop a pin", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                        }
                    }
                }
            }
        }
    }
}
