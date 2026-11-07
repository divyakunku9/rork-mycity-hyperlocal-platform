package com.sheharsaathi.app.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sheharsaathi.app.data.CityViewModel
import com.sheharsaathi.app.data.LegMode
import com.sheharsaathi.app.data.Place
import com.sheharsaathi.app.data.PlaceKind
import com.sheharsaathi.app.data.RouteOption
import com.sheharsaathi.app.data.RouteTab
import com.sheharsaathi.app.data.Transit
import com.sheharsaathi.app.ui.components.CityCard
import com.sheharsaathi.app.ui.components.LegChain
import com.sheharsaathi.app.ui.components.LegTile
import com.sheharsaathi.app.ui.components.LineBadge
import com.sheharsaathi.app.ui.components.RouteMap
import com.sheharsaathi.app.ui.components.SoftBadge
import com.sheharsaathi.app.ui.components.TagLabel
import com.sheharsaathi.app.ui.components.openUrl
import com.sheharsaathi.app.ui.theme.CityColors

private enum class EditField { FROM, TO }

private val departOptions = listOf(0 to "Depart now", 15 to "In 15 min", 30 to "In 30 min", 60 to "In 1 hour")

@Composable
fun PlannerScreen(
    vm: CityViewModel,
    onBack: () -> Unit,
    onStartGo: () -> Unit,
    onOpenBoard: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var editing by remember { mutableStateOf(if (vm.tripTo == null) EditField.TO else null) }
    var query by rememberSaveable { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(CityColors.Canvas).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (editing != null && vm.tripTo != null) { editing = null; query = "" } else onBack()
            }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Get me somewhere", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenBoard) {
                Icon(Icons.Filled.Radar, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Live board")
            }
        }

        TripCard(
            vm = vm,
            editing = editing,
            onEdit = { editing = it; query = "" },
            onSwap = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.swapTrip()
            },
        )

        if (editing != null) {
            PlaceSearch(
                field = editing!!,
                query = query,
                onQuery = { query = it },
                vm = vm,
                onPick = { p ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (editing == EditField.FROM) vm.tripFrom = p else vm.planTo(p)
                    editing = if (vm.tripTo == null) EditField.TO else null
                    query = ""
                    expandedId = null
                },
            )
        } else {
            val all = vm.routes
            val shown = if (vm.routeTab == RouteTab.BEST) all else all.filter { it.tab == vm.routeTab }
            val firstId = shown.firstOrNull()?.id
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "tabs") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        items(RouteTab.entries) { t ->
                            val icon = when (t) {
                                RouteTab.BEST -> Icons.Filled.Bolt
                                RouteTab.METRO -> Icons.Filled.DirectionsSubway
                                RouteTab.BUS -> Icons.Filled.DirectionsBus
                                RouteTab.RIDE -> Icons.Filled.ElectricRickshaw
                                RouteTab.CYCLE -> Icons.Filled.PedalBike
                            }
                            val count = if (t == RouteTab.BEST) all.size else all.count { it.tab == t }
                            FilterChip(
                                selected = vm.routeTab == t,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    vm.routeTab = t
                                    expandedId = null
                                },
                                label = { Text(if (t == RouteTab.BEST) t.label else "${t.label} · $count") },
                                leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) },
                                shape = CircleShape,
                                enabled = count > 0,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = CityColors.Surface,
                                    selectedContainerColor = CityColors.Indigo,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    iconColor = CityColors.Indigo,
                                ),
                            )
                        }
                    }
                }
                if (shown.isEmpty()) {
                    item(key = "empty") {
                        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Place, null, tint = CityColors.Muted, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (all.isEmpty()) "You're already there" else "No ${vm.routeTab.label.lowercase()} options for this trip",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                if (vm.rainSafe || vm.stepFree) "Try turning off Rain-safe or Step-free." else "Try another mode.",
                                style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted,
                            )
                        }
                    }
                }
                items(shown, key = { it.id }) { r ->
                    RouteCard(
                        route = r,
                        departOffset = vm.departOffsetMin,
                        destination = vm.tripTo,
                        expanded = (expandedId ?: firstId) == r.id,
                        highlight = r.id == firstId,
                        onToggle = { expandedId = if ((expandedId ?: firstId) == r.id) "" else r.id },
                        onGo = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.activeRoute = r
                            onStartGo()
                        },
                    )
                }
                if (shown.isNotEmpty()) {
                    item(key = "radar") { NearbyRadarCard(onOpenBoard) }
                }
            }
        }
    }
}

@Composable
private fun TripCard(vm: CityViewModel, editing: EditField?, onEdit: (EditField) -> Unit, onSwap: () -> Unit) {
    var departMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    var swapTurns by remember { mutableStateOf(0) }
    val swapRot by animateFloatAsState(swapTurns * 180f, label = "swap")
    CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.padding(end = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(14.dp).border(3.dp, CityColors.Indigo, CircleShape))
                    Box(Modifier.width(2.dp).height(42.dp).background(CityColors.IndigoMist))
                    Box(Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(CityColors.Saffron))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TripField(
                        label = "Start",
                        value = vm.tripFrom.name,
                        placeholder = "Choose start",
                        active = editing == EditField.FROM,
                        trailing = if (vm.tripFrom.kind == PlaceKind.CURRENT) "GPS" else null,
                        trailingIcon = Icons.Filled.GpsFixed,
                        onClick = { onEdit(EditField.FROM) },
                    )
                    TripField(
                        label = "Destination",
                        value = vm.tripTo?.name,
                        placeholder = "Where to?",
                        active = editing == EditField.TO,
                        trailing = null,
                        trailingIcon = Icons.Filled.Flag,
                        onClick = { onEdit(EditField.TO) },
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = { swapTurns++; onSwap() },
                    enabled = vm.tripTo != null,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = CityColors.IndigoSoft, contentColor = CityColors.Indigo,
                    ),
                ) { Icon(Icons.Filled.SwapVert, "Swap start and destination", Modifier.rotate(swapRot)) }
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = CityColors.Outline)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Box {
                        val label = departOptions.first { it.first == vm.departOffsetMin }.second
                        FilterChip(
                            selected = vm.departOffsetMin != 0,
                            onClick = { departMenu = true },
                            label = { Text("$label (${Transit.clockIn(vm.departOffsetMin)})") },
                            leadingIcon = { Icon(Icons.Filled.Schedule, null, Modifier.size(18.dp)) },
                            trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(18.dp)) },
                            shape = CircleShape,
                            colors = chipColors(),
                        )
                        DropdownMenu(expanded = departMenu, onDismissRequest = { departMenu = false }) {
                            departOptions.forEach { (min, text) ->
                                DropdownMenuItem(
                                    text = { Text("$text  ·  ${Transit.clockIn(min)}") },
                                    onClick = { vm.departOffsetMin = min; departMenu = false },
                                    leadingIcon = {
                                        if (vm.departOffsetMin == min) Icon(Icons.Filled.CheckCircle, null, tint = CityColors.Indigo)
                                        else Icon(Icons.Filled.Schedule, null, tint = CityColors.Muted)
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    FilterChip(
                        selected = vm.rainSafe,
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); vm.rainSafe = !vm.rainSafe },
                        label = { Text("Rain-safe") },
                        leadingIcon = { Icon(Icons.Filled.Umbrella, null, Modifier.size(18.dp)) },
                        shape = CircleShape, colors = chipColors(),
                    )
                }
                item {
                    FilterChip(
                        selected = vm.stepFree,
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); vm.stepFree = !vm.stepFree },
                        label = { Text("Step-free") },
                        leadingIcon = { Icon(Icons.Filled.Accessible, null, Modifier.size(18.dp)) },
                        shape = CircleShape, colors = chipColors(),
                    )
                }
            }
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = CityColors.Canvas,
    selectedContainerColor = CityColors.IndigoSoft,
    selectedLabelColor = CityColors.IndigoDeep,
    selectedLeadingIconColor = CityColors.Indigo,
    iconColor = CityColors.Indigo,
)

@Composable
private fun TripField(
    label: String, value: String?, placeholder: String, active: Boolean,
    trailing: String?, trailingIcon: ImageVector, onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) CityColors.IndigoSoft else CityColors.Canvas)
            .border(1.dp, if (active) CityColors.Indigo else CityColors.Outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            TagLabel(label, CityColors.Muted)
            Text(
                value ?: placeholder,
                style = MaterialTheme.typography.titleSmall,
                color = if (value == null) CityColors.Muted else CityColors.Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = CityColors.Indigo)
            Spacer(Modifier.width(4.dp))
        }
        Icon(trailingIcon, null, tint = CityColors.Indigo, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PlaceSearch(field: EditField, query: String, onQuery: (String) -> Unit, vm: CityViewModel, onPick: (Place) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(field) { runCatching { focus.requestFocus() } }
    val q = query.trim()
    val matches: (Place) -> Boolean = { q.isEmpty() || (it.name + " " + it.area).contains(q, ignoreCase = true) }
    val home = Transit.place("home")!!
    val work = Transit.place("work")!!
    val (remote, searching) = com.sheharsaathi.app.ui.components.rememberPlaceSearch(query)

    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).focusRequester(focus),
                placeholder = { Text(if (field == EditField.TO) "Search places, stations, venues" else "Search a start point", color = CityColors.Muted) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Filled.Close, "Clear") }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = CityColors.Surface, focusedContainerColor = CityColors.SurfaceHigh,
                    unfocusedBorderColor = CityColors.Outline, focusedBorderColor = CityColors.Indigo,
                ),
            )
        }
        if (q.isEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickPlace(Icons.Filled.Home, "Home", etaLabel(vm.tripFrom, home), Modifier.weight(1f)) { onPick(home) }
                    QuickPlace(Icons.Filled.Work, "Work", etaLabel(vm.tripFrom, work), Modifier.weight(1f)) { onPick(work) }
                }
            }
            if (field == EditField.FROM) {
                item {
                    PlaceRow(vm.userPlace, Icons.Filled.GpsFixed, CityColors.Indigo, "Your location • ${vm.userPlace.area}") { onPick(vm.userPlace) }
                }
            }
            item { TagLabel("Recent", CityColors.Muted, Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp)) }
            items(vm.recentPlaces, key = { "r-" + it.id }) { p ->
                PlaceRow(p, Icons.Filled.History, CityColors.Muted, p.area) { onPick(p) }
            }
        }
        if (searching) item { com.sheharsaathi.app.ui.components.SearchingRow() }
        if (remote.isNotEmpty()) {
            item { TagLabel("On the map", CityColors.Muted, Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp)) }
            items(remote, key = { "osm-" + it.id }) { p ->
                PlaceRow(p, com.sheharsaathi.app.ui.components.placeIcon(p.kind), CityColors.Indigo,
                    "${p.area} • ${"%.1f".format(Transit.roadKm(vm.tripFrom, p))} km") { onPick(p) }
            }
        }
        val list = (Transit.places.filter { it.kind != PlaceKind.HOME && it.kind != PlaceKind.WORK } + if (q.isEmpty()) emptyList() else Transit.areas)
            .filter(matches)
        if (list.isNotEmpty()) item { TagLabel(if (q.isEmpty()) "Popular in ${com.sheharsaathi.app.data.CurrentCity.city.name}" else "Suggestions", CityColors.Muted, Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp)) }
        items(list, key = { "p-" + it.id }) { p ->
            val (icon, tint) = when (p.kind) {
                PlaceKind.METRO -> Icons.Filled.DirectionsSubway to Color(com.sheharsaathi.app.data.LineColors.BLUE)
                PlaceKind.VENUE -> Icons.Filled.MusicNote to CityColors.Saffron
                else -> Icons.Filled.Place to CityColors.Indigo
            }
            PlaceRow(p, icon, tint, "${p.area} • ${"%.1f".format(Transit.roadKm(vm.tripFrom, p))} km") { onPick(p) }
        }
        if (list.isEmpty() && remote.isEmpty() && !searching) {
            item {
                Text(
                    "No places in ${com.sheharsaathi.app.data.CurrentCity.city.name} match \"$q\". Try a station, area or venue.",
                    style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
    }
}

private fun etaLabel(from: Place, to: Place): String {
    val best = Transit.plan(from, to, rainSafe = false, stepFree = false).minByOrNull { it.totalMinutes }
    return best?.let { "${it.totalMinutes} min" } ?: "Here"
}

@Composable
private fun QuickPlace(icon: ImageVector, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    CityCard(modifier, onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(CityColors.IndigoSoft), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = CityColors.Indigo, modifier = Modifier.size(20.dp))
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
private fun PlaceRow(place: Place, icon: ImageVector, tint: Color, sub: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(CityColors.SurfaceHigh).border(1.dp, CityColors.Outline, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(place.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 1)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CityColors.Muted)
    }
}

@Composable
private fun RouteCard(
    route: RouteOption,
    departOffset: Int,
    destination: Place?,
    expanded: Boolean,
    highlight: Boolean,
    onToggle: () -> Unit,
    onGo: () -> Unit,
) {
    val context = LocalContext.current
    val arrive = Transit.clockIn(departOffset + route.totalMinutes)
    val accent = when (route.id) {
        "metro" -> CityColors.Indigo
        "auto", "cab" -> CityColors.SaffronText
        "bus" -> CityColors.Saffron
        else -> CityColors.Green
    }
    CityCard(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).animateContentSize(),
        onClick = onToggle,
        borderColor = if (highlight) CityColors.Indigo.copy(alpha = 0.5f) else CityColors.Outline,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    route.badge?.let {
                        SoftBadge(
                            it, if (highlight) CityColors.Indigo else accent.copy(alpha = 0.1f),
                            if (highlight) Color.White else accent,
                            icon = if (highlight) Icons.Filled.Star else Icons.Filled.Bolt,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = if (highlight) CityColors.Indigo else CityColors.Ink)) {
                                    append("${route.totalMinutes}")
                                }
                                withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (highlight) CityColors.Indigo else CityColors.Ink)) {
                                    append(" min")
                                }
                            },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Arrive $arrive", style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted, modifier = Modifier.padding(bottom = 5.dp))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(route.priceLabel, style = MaterialTheme.typography.titleLarge, color = if (route.id == "bus" || route.id == "cycle") CityColors.Green else CityColors.Ink)
                    route.priceSub?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = accent) }
                }
            }
            Spacer(Modifier.height(8.dp))
            LegChain(route.legs)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (route.departsInMin > 0) {
                    SoftBadge("Leaves in ${route.departsInMin} min", CityColors.SaffronSoft, CityColors.SaffronText, icon = Icons.Filled.Schedule)
                }
                route.tags.take(2).forEach { SoftBadge(it, CityColors.Canvas, CityColors.IndigoDeep) }
            }

            AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CityColors.Canvas)
                            .border(1.dp, CityColors.Outline, RoundedCornerShape(14.dp)).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        route.legs.forEach { leg ->
                            Row(verticalAlignment = Alignment.Top) {
                                LegTile(leg)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            when (leg.mode) {
                                                LegMode.WALK -> "${leg.minutes} min walk"
                                                LegMode.METRO -> leg.title
                                                else -> "${leg.minutes} min ${leg.title}"
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (leg.mode == LegMode.METRO) CityColors.Indigo else CityColors.Ink,
                                        )
                                        if (leg.lineLabel != null && leg.mode == LegMode.METRO) {
                                            Spacer(Modifier.width(6.dp))
                                            LineBadge("${leg.lineLabel} Line", Color(leg.lineColor))
                                        }
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            if (leg.priceRupees > 0) "₹${leg.priceRupees}" else if (leg.distanceM > 0) "${leg.distanceM} m" else "",
                                            style = MaterialTheme.typography.labelLarge, color = CityColors.Muted,
                                        )
                                    }
                                    val detail = when (leg.mode) {
                                        LegMode.METRO -> "${leg.platform} • Next train in ${leg.waitMin} min (${leg.minutes} min ride, ${leg.stops.size - 1} stops)"
                                        LegMode.BUS -> "Leaves in ${leg.waitMin} min from ${leg.from} • ${leg.detail}"
                                        else -> leg.detail
                                    }
                                    Text(detail, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    RouteMap(route.legs, Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(14.dp)))
                }
            }

            route.note?.let {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(if (route.noteGood) CityColors.GreenSoft else CityColors.SaffronSoft)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (route.noteGood) Icons.Filled.CheckCircle else Icons.Filled.Traffic, null,
                        tint = if (route.noteGood) CityColors.Green else CityColors.SaffronText, modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = CityColors.Ink)
                }
            }

            Spacer(Modifier.height(12.dp))
            if (route.rideApps.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    route.rideApps.take(2).forEach { app ->
                        OutlinedButton(
                            onClick = { openRideApp(context, app, destination) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            border = BorderStroke(1.dp, CityColors.IndigoMist),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = CityColors.IndigoSoft.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) { Text(app, maxLines = 1) }
                    }
                    GoButton(onGo, compact = true)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (expanded) "Tap card to collapse" else "Tap for step-by-step",
                        style = MaterialTheme.typography.labelMedium, color = CityColors.Muted, modifier = Modifier.weight(1f),
                    )
                    GoButton(onGo)
                }
            }
        }
    }
}

@Composable
private fun GoButton(onClick: () -> Unit, compact: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CityColors.IndigoDeep),
        contentPadding = PaddingValues(horizontal = if (compact) 14.dp else 20.dp),
    ) {
        Text(if (compact) "GO" else "Start GO", fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Filled.Navigation, null, Modifier.size(16.dp))
    }
}

private fun openRideApp(context: Context, app: String, to: Place?) {
    val lat = to?.lat ?: 17.4483
    val lng = to?.lng ?: 78.3915
    val url = when (app) {
        "Uber", "Uber Auto", "Uber Moto" -> "https://m.uber.com/ul/?action=setPickup&pickup=my_location&dropoff[latitude]=$lat&dropoff[longitude]=$lng"
        "Ola" -> "https://book.olacabs.com/?drop_lat=$lat&drop_lng=$lng"
        else -> "https://www.rapido.bike/"
    }
    openUrl(context, url)
}

@Composable
private fun NearbyRadarCard(onOpen: () -> Unit) {
    CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), onClick = onOpen) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Radar, null, tint = CityColors.Indigo, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Nearby transit radar", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("Live board", style = MaterialTheme.typography.labelLarge, color = CityColors.Indigo)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CityColors.Indigo)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                com.sheharsaathi.app.data.LiveBoard.departures.take(3).forEachIndexed { i, d ->
                    LineBadge(d.code, Color(d.color)); Text("${2 + i * 4} min", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
