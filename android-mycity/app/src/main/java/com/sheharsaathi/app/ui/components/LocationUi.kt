package com.sheharsaathi.app.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sheharsaathi.app.data.CityViewModel
import com.sheharsaathi.app.data.Geocoder
import com.sheharsaathi.app.data.LocationService
import com.sheharsaathi.app.data.LocationSource
import com.sheharsaathi.app.data.Place
import com.sheharsaathi.app.data.PlaceKind
import com.sheharsaathi.app.data.Transit
import com.sheharsaathi.app.ui.theme.CityColors
import kotlinx.coroutines.delay

/**
 * Returns a callback that asks for location permission when needed and then
 * fetches a one-shot GPS fix into the view model.
 */
@Composable
fun rememberLocateMe(vm: CityViewModel): () -> Unit {
    val context = LocalContext.current

    val fetch: () -> Unit = {
        if (!LocationService.isLocationOn(context)) {
            vm.gpsFailed("Location is turned off on your phone. Turn it on or pick your area.")
        } else {
            // Runs in the view model, so leaving this screen can't cancel it halfway and
            // leave every header stuck on "Finding you…".
            vm.locateNow()
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) fetch()
        else vm.gpsFailed("Location permission denied. You can still pick your area manually.")
    }
    return {
        if (LocationService.hasPermission(context)) fetch()
        else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
}

/** Debounced OpenStreetMap search across the current city. */
@Composable
fun rememberPlaceSearch(query: String): Pair<List<Place>, Boolean> {
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 3) { results = emptyList(); loading = false; return@LaunchedEffect }
        loading = true
        delay(450)
        results = Geocoder.search(q)
        loading = false
    }
    return results to loading
}

fun placeIcon(kind: PlaceKind): ImageVector = when (kind) {
    PlaceKind.METRO -> Icons.Filled.Train
    PlaceKind.VENUE -> Icons.Filled.Place
    PlaceKind.CURRENT -> Icons.Filled.GpsFixed
    else -> Icons.Filled.LocationOn
}

@Composable
fun PlaceResultRow(place: Place, icon: ImageVector, tint: Color, sub: String = place.area, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(place.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SearchingRow() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = CityColors.Indigo)
        Spacer(Modifier.width(12.dp))
        Text("Searching OpenStreetMap…", style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
    }
}

/** Banner nudging the user to share location or pick it manually. */
@Composable
fun LocationNudge(vm: CityViewModel, onAllow: () -> Unit, modifier: Modifier = Modifier) {
    val notice = vm.locationNotice
    CityCard(modifier.fillMaxWidth(), borderColor = if (notice != null) CityColors.Saffron.copy(alpha = 0.5f) else CityColors.Outline) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(if (notice != null) CityColors.SaffronSoft else CityColors.IndigoSoft), contentAlignment = Alignment.Center) {
                    Icon(if (notice != null) Icons.Filled.Info else Icons.Filled.GpsFixed, null,
                        tint = if (notice != null) CityColors.Saffron else CityColors.Indigo, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (notice != null) "Location not set" else "Show what's around you", style = MaterialTheme.typography.titleSmall)
                    Text(
                        notice ?: "Use your phone's GPS or enter your ${vm.city.name} area to see nearby events, reports and routes.",
                        style = MaterialTheme.typography.bodySmall, color = CityColors.Muted,
                    )
                }
                IconButton(onClick = { vm.locationBannerDismissed = true; vm.locationNotice = null }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, "Dismiss", tint = CityColors.Muted, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAllow, contentPadding = PaddingValues(horizontal = 14.dp), modifier = Modifier.height(38.dp)) {
                    Icon(Icons.Filled.GpsFixed, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Use my location", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(onClick = { vm.showLocationPicker = true }, contentPadding = PaddingValues(horizontal = 14.dp), modifier = Modifier.height(38.dp)) {
                    Text("Enter manually", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Sheet to set the user's location: GPS, OpenStreetMap search, or a popular area. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerSheet(vm: CityViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locate = rememberLocateMe(vm)
    var query by remember { mutableStateOf("") }
    val (remote, loading) = rememberPlaceSearch(query)
    val q = query.trim()
    val areas = Transit.areas.filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }

    // Close once a GPS fix lands inside the city
    var waitingForGps by remember { mutableStateOf(false) }
    LaunchedEffect(vm.locating, vm.locationSource, vm.userPlace) {
        if (waitingForGps && !vm.locating && vm.locationSource == LocationSource.GPS) vm.showLocationPicker = false
    }

    ModalBottomSheet(
        onDismissRequest = { vm.showLocationPicker = false },
        sheetState = sheetState,
        containerColor = CityColors.Surface,
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).imePadding()) {
            Text("Set your location", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 20.dp))
            Text(
                "Showing areas in ${vm.city.name}, ${vm.city.state}. Switch city from your profile.",
                style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            vm.locationNotice?.let { notice ->
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)).background(CityColors.SaffronSoft).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Info, null, tint = CityColors.Saffron, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(notice, style = MaterialTheme.typography.bodySmall, color = CityColors.SaffronText)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .border(1.dp, CityColors.Indigo.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .background(CityColors.IndigoSoft.copy(alpha = 0.5f))
                    .clickable(enabled = !vm.locating) { waitingForGps = true; locate() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(CityColors.Indigo), contentAlignment = Alignment.Center) {
                    if (vm.locating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Icon(Icons.Filled.GpsFixed, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (vm.locating) "Finding you…" else "Use my current location", style = MaterialTheme.typography.titleSmall, color = CityColors.Indigo)
                    Text(
                        if (vm.locationSource == LocationSource.GPS) "Currently: ${vm.userPlace.name}" else "Uses your phone's GPS",
                        style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            CitySearchField(query, { query = it }, "Search area, street or landmark in ${vm.city.name}")
            Spacer(Modifier.height(6.dp))
            LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (loading) item { SearchingRow() }
                if (remote.isNotEmpty()) {
                    item { TagLabel("On the map", CityColors.Muted, Modifier.padding(start = 20.dp, top = 10.dp, bottom = 4.dp)) }
                    items(remote, key = { "r-" + it.id }) { p ->
                        PlaceResultRow(p, placeIcon(p.kind), CityColors.Indigo) { vm.setManualLocation(p) }
                    }
                }
                if (!loading && q.length >= 3 && remote.isEmpty() && areas.isEmpty()) {
                    item {
                        Text("No places found in ${vm.city.name} for \u201c$q\u201d.", style = MaterialTheme.typography.bodyMedium,
                            color = CityColors.Muted, modifier = Modifier.padding(20.dp))
                    }
                }
                if (areas.isNotEmpty()) {
                    item { TagLabel("Popular areas", CityColors.Muted, Modifier.padding(start = 20.dp, top = 10.dp, bottom = 4.dp)) }
                    items(areas, key = { it.id }) { p ->
                        PlaceResultRow(p, Icons.Filled.Apartment, CityColors.Saffron, sub = "${vm.city.name}, ${vm.city.state}") { vm.setManualLocation(p) }
                    }
                }
            }
        }
    }
}
