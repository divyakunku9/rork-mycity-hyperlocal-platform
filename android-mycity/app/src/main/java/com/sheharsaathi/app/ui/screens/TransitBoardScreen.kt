package com.sheharsaathi.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sheharsaathi.app.data.BoardKind
import com.sheharsaathi.app.data.CityViewModel
import com.sheharsaathi.app.data.Departure
import com.sheharsaathi.app.data.LineColors
import com.sheharsaathi.app.data.LiveBoard
import com.sheharsaathi.app.ui.components.CityCard
import com.sheharsaathi.app.ui.components.SoftBadge
import com.sheharsaathi.app.ui.components.TagLabel
import com.sheharsaathi.app.ui.theme.CityColors
import kotlinx.coroutines.delay

@Composable
fun TransitBoardScreen(vm: CityViewModel, onBack: () -> Unit, onPlan: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var tick by remember { mutableIntStateOf(0) }
    var lastSync by remember { mutableIntStateOf(0) }
    var spins by remember { mutableIntStateOf(0) }
    val spin by animateFloatAsState(spins * 360f, tween(700), label = "spin")
    var kind by rememberSaveable { mutableStateOf(LiveBoard.kinds.firstOrNull() ?: BoardKind.BUS) }
    var dialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
            if (tick - lastSync >= 30) lastSync = tick
        }
    }

    val list = LiveBoard.departures.filter { it.kind == kind }
        .sortedWith(compareByDescending<Departure> { vm.favoriteLines[it.id] == true }.thenBy { secondsUntil(it, tick) })

    LazyColumn(
        Modifier.fillMaxSize().background(CityColors.Canvas).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Column(Modifier.weight(1f)) {
                    Text(vm.locationLabel, style = MaterialTheme.typography.titleMedium)
                    Text(if (com.sheharsaathi.app.data.CurrentCity.city.isHyderabad) "HITEC City – Ameerpet – MGBS corridor" else com.sheharsaathi.app.data.CurrentCity.city.areas.take(3).joinToString(" – ") { it.first } + " corridor", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                }
                TextButton(onClick = onPlan) {
                    Icon(Icons.Filled.Navigation, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Plan trip")
                }
            }
        }
        item {
            CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            SoftBadge("Estimated from timetables", CityColors.IndigoSoft, CityColors.Indigo)
                            Spacer(Modifier.height(8.dp))
                            Text((com.sheharsaathi.app.data.CurrentCity.city.metroName?.let { "$it & " } ?: "") + "${com.sheharsaathi.app.data.CurrentCity.city.busOperator} " + (if (vm.isLive) "Departures" else "Live Board"), style = MaterialTheme.typography.headlineSmall)
                        }
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            spins++
                            lastSync = tick
                        }) { Icon(Icons.Filled.Sync, "Refresh", tint = CityColors.Indigo, modifier = Modifier.rotate(spin)) }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = CityColors.Outline)
                    if (vm.isLive) {
                        // No live feed yet: only official notices are shown, and times are timetable estimates.
                        val disrupted = vm.serviceStatus.any { it.level > 0 }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (disrupted) SoftBadge("Disruptions reported", CityColors.SaffronSoft, CityColors.SaffronText, icon = Icons.Filled.Warning)
                            else SoftBadge("No disruptions reported", CityColors.GreenSoft, CityColors.Green, icon = Icons.Filled.CheckCircle)
                        }
                        Text("Departure times are estimates from published timetables. Check the operator for live updates.",
                            style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, modifier = Modifier.padding(top = 6.dp))
                    } else Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftBadge("System normal", CityColors.GreenSoft, CityColors.Green, icon = Icons.Filled.CheckCircle)
                        Spacer(Modifier.width(8.dp))
                        Text("98% on time", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.weight(1f))
                        Text("Updated ${tick - lastSync}s ago", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp)) {
                TagLabel(if (vm.isLive) "Official service notices" else "Corridor service status", CityColors.Ink, Modifier.weight(1f))
                if (!vm.isLive) TagLabel("All 42 lines", CityColors.Indigo)
            }
        }
        if (vm.isLive && vm.serviceStatus.isEmpty()) item {
            Text("None right now. Notices from the operators will appear here.",
                style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, modifier = Modifier.padding(horizontal = 20.dp))
        }
        items(if (vm.isLive) vm.serviceStatus else LiveBoard.status) { s ->
            val color = Color(s.color)
            val tagColors = when (s.level) {
                0 -> CityColors.IndigoSoft to CityColors.Indigo
                1 -> CityColors.SaffronSoft to CityColors.SaffronText
                else -> CityColors.RedSoft to CityColors.Red
            }
            CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(Modifier.width(4.dp).fillMaxHeight().background(color))
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(if (s.level == 0) Icons.Filled.CheckCircle else Icons.Filled.Warning, null, tint = color, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(s.title, style = MaterialTheme.typography.titleSmall, color = color, modifier = Modifier.weight(1f))
                                SoftBadge(s.tag, tagColors.first, tagColors.second)
                            }
                            Text(s.body, style = MaterialTheme.typography.bodySmall, color = CityColors.Ink)
                        }
                    }
                }
            }
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                items(LiveBoard.kinds) { k ->
                    val icon: ImageVector = when (k) {
                        BoardKind.METRO -> Icons.Filled.DirectionsSubway
                        BoardKind.BUS -> Icons.Filled.DirectionsBus
                        BoardKind.RAIL -> Icons.Filled.Train
                    }
                    FilterChip(
                        selected = kind == k,
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); kind = k },
                        label = { Text(k.label) },
                        leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) },
                        shape = CircleShape,
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
        items(list, key = { it.id }) { d ->
            DepartureCard(
                d, tick, vm.favoriteLines[d.id] == true,
                onStar = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.toggleFavoriteLine(d.id) },
                modifier = Modifier.animateItem(),
            )
        }
        item {
            TagLabel("Commuter power tools", CityColors.Ink, Modifier.padding(start = 20.dp, top = 10.dp))
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolCard(Icons.Filled.Map, CityColors.Indigo, CityColors.IndigoSoft, "Metro fare matrix", if (com.sheharsaathi.app.data.CurrentCity.city.isHyderabad) "Fares across Red, Blue & Green line stations." else "Metro, bus and auto fares at a glance.", "View fares", Modifier.weight(1f)) { dialog = "fares" }
                ToolCard(Icons.Filled.Alarm, CityColors.SaffronText, CityColors.SaffronSoft, "First & last services", "Terminal departures for every corridor.", "Check hours", Modifier.weight(1f)) { dialog = "hours" }
            }
        }
    }

    dialog?.let { which ->
        AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = { Button(onClick = { dialog = null }) { Text("Got it") } },
            title = { Text(if (which == "fares") "${com.sheharsaathi.app.data.CurrentCity.city.metroName ?: com.sheharsaathi.app.data.CurrentCity.city.busOperator} fares" else "First & last services") },
            text = {
                val c = com.sheharsaathi.app.data.CurrentCity.city
                val rows = if (!c.isHyderabad) {
                    if (which == "fares") buildList {
                        if (c.metroName != null) { add("${c.metroName} • short trip" to "from ₹10"); add("${c.metroName} • longest trip" to "up to ₹60–90") }
                        add("${c.busOperator} ordinary bus" to "from ₹5–10"); add("${c.busOperator} AC bus" to "from ₹15–25")
                        add("Auto rickshaw • first 1.5–2 km" to "₹25–36")
                    } else buildList {
                        c.metroLines.forEach { l -> add("${l.short} • ${l.from} ↔ ${l.to}" to "≈ 5:30–6:00 AM – 11:00 PM") }
                        add("${c.busOperator} city buses" to "≈ 5:00 AM – 11:00 PM")
                        add("Night services" to "Select routes only")
                    }
                } else if (which == "fares") listOf(
                    "Up to 2 km" to "₹12", "2 – 4 km" to "₹18", "Ameerpet → HITEC City" to "₹40",
                    "Raidurg → Ameerpet" to "₹50", "Miyapur → MGBS" to "₹60", "Nagole → Raidurg" to "₹70",
                    "Longest trip (24 km+)" to "₹75",
                ) else listOf(
                    "Red • Miyapur ↔ LB Nagar" to "6:00 AM – 11:00 PM", "Blue • Nagole ↔ Raidurg" to "6:00 AM – 11:00 PM",
                    "Green • JBS ↔ MGBS" to "6:00 AM – 11:00 PM", "MMTS • Lingampally ↔ Falaknuma" to "4:30 AM – 11:30 PM",
                    "Sunday first train" to "7:00 AM",
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { (a, b) ->
                        Row {
                            Text(a, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(b, style = MaterialTheme.typography.labelLarge, color = CityColors.Indigo)
                        }
                    }
                    Text(
                        if (!c.isHyderabad) "Approximate figures. Check the operator's official site for current fares and timings." else if (which == "fares") "Smart card trips get 10% off. QR tickets on WhatsApp & the TSavaari app." else "Trains every 3–4 min at peak, 8–10 min off-peak.",
                        style = MaterialTheme.typography.bodySmall, color = CityColors.Muted,
                    )
                }
            },
        )
    }
}

private fun secondsUntil(d: Departure, tick: Int): Int {
    val period = d.headwayMin * 60
    var s = d.baseSeconds - tick
    while (s < 0) s += period
    return s
}

@Composable
private fun DepartureCard(d: Departure, tick: Int, fav: Boolean, onStar: () -> Unit, modifier: Modifier = Modifier) {
    val color = Color(d.color)
    val s = secondsUntil(d, tick)
    val min = s / 60
    val bus = d.kind == BoardKind.BUS
    CityCard(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        d.code, color = if (d.color == LineColors.YELLOW) CityColors.Ink else CityColors.Surface,
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(d.name, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = onStar, modifier = Modifier.size(30.dp)) {
                            Icon(
                                if (fav) Icons.Filled.Star else Icons.Filled.StarBorder,
                                if (fav) "Remove favourite" else "Add favourite",
                                tint = if (fav) CityColors.Saffron else CityColors.Muted, modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Text(d.towards, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedContent(if (s < 60) -1 else min, label = "dep") { m ->
                        Text(
                            if (m < 0) "Due" else "$m min",
                            style = MaterialTheme.typography.headlineSmall,
                            color = when {
                                !d.statusGood -> CityColors.Red
                                bus -> CityColors.Saffron
                                else -> CityColors.Indigo
                            },
                        )
                    }
                    SoftBadge(
                        d.status.uppercase(),
                        if (d.statusGood) CityColors.GreenSoft else CityColors.RedSoft,
                        if (d.statusGood) CityColors.Green else CityColors.Red,
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = CityColors.Outline)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (d.meta + d.crowd).filter { it.isNotBlank() }.joinToString("  •  "),
                    style = MaterialTheme.typography.labelMedium, color = CityColors.IndigoDeep, modifier = Modifier.weight(1f), maxLines = 1,
                )
                Text("Then: ", style = MaterialTheme.typography.labelMedium, color = CityColors.Muted)
                listOf(min + d.headwayMin, min + d.headwayMin * 2).forEach { n ->
                    Text(
                        "${n}m",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 4.dp).clip(RoundedCornerShape(6.dp)).background(CityColors.IndigoSoft).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolCard(
    icon: ImageVector, tint: Color, bg: Color, title: String, body: String, action: String,
    modifier: Modifier, onClick: () -> Unit,
) {
    CityCard(modifier.fillMaxHeight(), onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(bg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
            Spacer(Modifier.height(10.dp))
            Text(action.uppercase(), style = MaterialTheme.typography.labelSmall, color = tint)
        }
    }
}
