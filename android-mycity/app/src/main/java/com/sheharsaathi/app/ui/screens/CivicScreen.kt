package com.sheharsaathi.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sheharsaathi.app.data.CityViewModel
import com.sheharsaathi.app.data.CivicService
import com.sheharsaathi.app.data.HyderabadServices
import com.sheharsaathi.app.data.HelplineKind
import com.sheharsaathi.app.data.LocationService
import com.sheharsaathi.app.data.LocationSource
import com.sheharsaathi.app.ui.components.LocalOpenProfile
import com.sheharsaathi.app.ui.components.ProfileAvatar
import com.sheharsaathi.app.ui.components.rememberLocateMe
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.sheharsaathi.app.data.IssueStatus
import com.sheharsaathi.app.ui.components.CityCard
import com.sheharsaathi.app.ui.components.IconTile
import com.sheharsaathi.app.ui.components.SoftBadge
import com.sheharsaathi.app.ui.components.dial
import com.sheharsaathi.app.ui.components.openUrl
import com.sheharsaathi.app.ui.theme.CityColors

@Composable
fun CivicScreen(vm: CityViewModel, onOpenReports: (mine: Boolean) -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var bulkWasteDialog by remember { mutableStateOf(false) }
    var bulkRequested by remember { mutableStateOf(false) }

    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    // Live cities show figures from real reports; sample cities keep the demo numbers.
    val stats = vm.civicStats
    val target = if (vm.isLive) stats.resolutionRate else 0.84f
    val health by animateFloatAsState(if (started) target else 0f, tween(1100), label = "health")

    val city = vm.city
    val mine = vm.myIssues
    // A ward picked by hand counts as knowing where the user is.
    val gated = !LocationService.hasPermission(context) && vm.locationSource == LocationSource.DEFAULT && vm.myWard == null
    val locate = rememberLocateMe(vm)
    var service by remember { mutableStateOf<CivicService?>(null) }
    LaunchedEffect(vm.hasWards) { vm.refreshWards() }
    if (gated) {
        CivicLocationGate(
            cityName = city.name, onAllow = locate, onPick = { vm.showLocationPicker = true },
            onChooseWard = if (vm.hasWards) ({ vm.showWardPicker = true }) else null,
        )
        WardPickerSheet(vm)
        return
    }
    val ward = vm.myWard?.ward
    val body = vm.civicBodyFor(ward)
    val corpShort = body?.shortName?.let { if (it == "GHMC") it else "$it MC" } ?: city.municipalShort
    val inReview = mine.count { it.status != IssueStatus.RESOLVED }
    val resolved = mine.count { it.status == IssueStatus.RESOLVED }

    Column(
        Modifier.fillMaxSize().background(CityColors.Canvas).verticalScroll(rememberScrollState())
            .statusBarsPadding().padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AccountBalance, null, tint = CityColors.IndigoDeep, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Civic Hub", style = MaterialTheme.typography.headlineSmall, color = CityColors.IndigoDeep)
                Text(if (ward != null) "$corpShort • ${ward.label}" else "${city.municipalShort} • ${vm.userPlace.area}", style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
            }
            IconButton(onClick = { onOpenReports(true) }) { Icon(Icons.Filled.Notifications, "My report updates") }
            val openProfile = LocalOpenProfile.current
            ProfileAvatar(vm.profile, 44, Modifier.clip(CircleShape).clickable(onClickLabel = "Open profile", onClick = openProfile))
        }

        if (vm.hasWards) WardCard(vm, onLocate = locate)

        // Health index
        CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (vm.isLive) "Report resolution rate" else "Neighbourhood Health Index", style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (vm.isLive) "From ${stats.total} report${if (stats.total == 1) "" else "s"} on Shehar Saathi" else "+4.2% vs last month",
                            style = MaterialTheme.typography.bodySmall, color = if (vm.isLive) CityColors.Muted else CityColors.Green,
                        )
                    }
                    Text(
                        buildAnnotatedString {
                            if (vm.isLive && stats.total == 0) {
                                // Nothing to score yet: a 0/100 would wrongly suggest the ward is doing badly.
                                withStyle(SpanStyle(fontSize = 40.sp, color = CityColors.Muted)) { append("–") }
                            } else {
                                withStyle(SpanStyle(fontSize = 40.sp, color = CityColors.Green)) { append("${(health * 100).toInt()}") }
                                withStyle(SpanStyle(fontSize = 18.sp, color = CityColors.Muted)) { append(if (vm.isLive) "%" else "/100") }
                            }
                        },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { health },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = CityColors.Green,
                    trackColor = CityColors.Outline,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(Icons.Filled.Warning, "Potholes Fixed", if (vm.isLive) "${stats.potholesFixed}" else "142", CityColors.Red, CityColors.RedSoft, Modifier.weight(1f))
                    StatTile(Icons.Filled.Lightbulb, if (vm.isLive) "Resolved" else "Streetlights", if (vm.isLive) "${stats.resolved}" else "98%", CityColors.Saffron, CityColors.SaffronSoft, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(Icons.Filled.Delete, if (vm.isLive) "Confirmations" else "Waste Compliance", if (vm.isLive) "${stats.confirmations}" else "92%", CityColors.Green, CityColors.GreenSoft, Modifier.weight(1f))
                    StatTile(Icons.Filled.Description, "Grievances Open", if (vm.isLive) "${stats.open}" else "18", CityColors.Indigo, CityColors.IndigoSoft, Modifier.weight(1f))
                }
            }
        }

        // Citizen tier
        CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), onClick = { onOpenReports(true) }) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(vm.citizenTier.first, style = MaterialTheme.typography.titleLarge)
                        Text(vm.locationLabel, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                    }
                    SoftBadge(vm.citizenTier.second, CityColors.IndigoSoft, CityColors.IndigoDeep, icon = Icons.Filled.WorkspacePremium)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MilitaryTech, null, tint = CityColors.Saffron, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${vm.reportsValidated} Report${if (vm.reportsValidated == 1) "" else "s"} ${if (vm.isLive) "Filed" else "Validated"}", style = MaterialTheme.typography.titleMedium)
                        Text("Making ${city.name} better, one report at a time", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().background(CityColors.Canvas, RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("My filed issues", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    SoftBadge("$inReview in review", CityColors.SaffronSoft, CityColors.SaffronText)
                    Spacer(Modifier.width(6.dp))
                    SoftBadge("$resolved resolved", CityColors.GreenSoft, CityColors.Green)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CityColors.Muted)
                }
            }
        }

        // Ward poll (from the server for live cities; hidden when none is open)
        vm.poll?.let { poll ->
        CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(poll.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Text(poll.endsLabel, style = MaterialTheme.typography.labelMedium, color = CityColors.Muted)
                }
                if (poll.detail.isNotBlank()) {
                    Text(poll.detail, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                }
                Spacer(Modifier.height(8.dp))
                poll.options.forEachIndexed { i, option ->
                    PollOption(
                        label = option.label,
                        percent = vm.pollPercent(i),
                        selected = vm.pollChoice == i,
                        revealed = vm.pollChoice != null,
                        onClick = {
                            if (vm.pollChoice == null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.vote(i)
                        },
                    )
                }
                Text(
                    if (vm.pollChoice != null) "Vote recorded • ${"%,d".format(vm.pollTotal)} verified residents" else "Tap an option to cast your vote • ${"%,d".format(vm.pollTotal)} votes so far",
                    style = MaterialTheme.typography.bodySmall, color = CityColors.Muted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        }

        // Helplines
        CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Ward helplines", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    city.helplines.take(3).forEach { h ->
                        val icon = when (h.kind) {
                            HelplineKind.CIVIC -> Icons.Filled.AccountBalance
                            HelplineKind.POWER -> Icons.Filled.ElectricBolt
                            HelplineKind.WATER -> Icons.Filled.WaterDrop
                            HelplineKind.POLICE -> Icons.Filled.LocalPolice
                        }
                        Helpline(h.display, h.label, icon, Modifier.weight(1f)) { dial(context, h.dial) }
                    }
                }
            }
        }

        // Services
        CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ServiceTile(Icons.Filled.ReceiptLong, "Pay Utility Bills", "Tax, power & water", CityColors.Indigo, CityColors.IndigoSoft, Modifier.weight(1f)) {
                        if (vm.hasWards) service = HyderabadServices.bills(ward?.corporation) else openUrl(context, city.portals.utilityBills)
                    }
                    ServiceTile(Icons.Filled.Storefront, "Trade License", if (vm.hasWards) "Apply / Renew · $corpShort" else "Apply / Renew", CityColors.Saffron, CityColors.SaffronSoft, Modifier.weight(1f)) {
                        if (vm.hasWards) service = HyderabadServices.trade(ward?.corporation) else openUrl(context, city.portals.tradeLicense)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ServiceTile(
                        Icons.Filled.LocalShipping, "Bulk Waste",
                        if (vm.hasWards) "Debris pickup lines" else if (vm.isLive) "Apply on the ${city.municipalShort} portal" else if (bulkRequested) "Pickup requested ✓" else "Request a pickup",
                        CityColors.Green, CityColors.GreenSoft, Modifier.weight(1f),
                    ) {
                        // Pickups can't be booked through the app yet, so live cities go straight to the official service.
                        if (vm.hasWards) service = HyderabadServices.bulkWaste
                        else if (vm.isLive) openUrl(context, city.portals.bulkWaste) else bulkWasteDialog = true
                    }
                    ServiceTile(Icons.Filled.Forest, "Tree Fall / Hazard", if (vm.hasWards) "Report to HYDRAA" else "Report to ${city.municipalShort}", CityColors.Red, CityColors.RedSoft, Modifier.weight(1f)) {
                        if (vm.hasWards) service = HyderabadServices.treeFall else openUrl(context, city.portals.treeHazard)
                    }
                }
            }
        }
    }

    WardPickerSheet(vm)
    service?.let { CivicServiceSheet(it) { service = null } }

    if (bulkWasteDialog) {
        AlertDialog(
            onDismissRequest = { bulkWasteDialog = false },
            icon = { Icon(Icons.Filled.LocalShipping, null, tint = CityColors.Green) },
            title = { Text(if (bulkRequested) "Pickup already scheduled" else "Request bulk waste pickup") },
            text = {
                Text(
                    if (bulkRequested) "A ${city.municipalShort} e-waste & debris van is scheduled for Saturday, 9 AM – 1 PM in ${vm.userPlace.area}."
                    else "Book the ${city.municipalShort} e-waste & debris van for ${vm.userPlace.area}. Next available slot: Saturday, 9 AM – 1 PM. You can also apply on the official portal."
                )
            },
            confirmButton = {
                if (bulkRequested) TextButton(onClick = { bulkWasteDialog = false }) { Text("OK") }
                else Button(onClick = {
                    bulkRequested = true
                    bulkWasteDialog = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) { Text("Book slot") }
            },
            dismissButton = {
                TextButton(onClick = { bulkWasteDialog = false; openUrl(context, city.portals.bulkWaste) }) { Text("Official portal") }
            },
        )
    }
}

@Composable
private fun CivicLocationGate(cityName: String, onAllow: () -> Unit, onPick: () -> Unit, onChooseWard: (() -> Unit)?) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF1A2547), CityColors.Canvas)),
        ).statusBarsPadding().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(96.dp).clip(CircleShape).background(CityColors.IndigoSoft)
                    .border(1.dp, CityColors.Indigo.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.ShareLocation, null, tint = CityColors.IndigoDeep, modifier = Modifier.size(46.dp)) }
            Spacer(Modifier.height(22.dp))
            Text("Find your ward", style = MaterialTheme.typography.headlineSmall, color = CityColors.Ink)
            Text(
                "Civic Hub shows your ward number, who represents it, ward votes and the corporation that handles your complaints in $cityName. Share your location, pick a spot on the map, or choose your ward.",
                style = MaterialTheme.typography.bodyLarge, color = CityColors.Muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 26.dp),
            )
            PrimaryButton("Allow location", enabled = true, onClick = onAllow)
            TextButton(onClick = onPick, modifier = Modifier.padding(top = 6.dp)) {
                Text("Pick on map instead", color = CityColors.Indigo, style = MaterialTheme.typography.titleSmall)
            }
            if (onChooseWard != null) {
                TextButton(onClick = onChooseWard) {
                    Text("Choose my ward from the list", color = CityColors.Indigo, style = MaterialTheme.typography.titleSmall)
                }
            }
            Text(
                "Your location stays on this phone.", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, label: String, value: String, tint: Color, bg: Color, modifier: Modifier) {
    Row(
        modifier.background(bg, RoundedCornerShape(14.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, tint, CityColors.SurfaceHigh, size = 40)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = CityColors.Ink, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun PollOption(label: String, percent: Int, selected: Boolean, revealed: Boolean, onClick: () -> Unit) {
    val fill by animateFloatAsState(if (revealed) percent / 100f else 0f, tween(700), label = "poll")
    Box(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .background(CityColors.SurfaceHigh, RoundedCornerShape(12.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton, enabled = !revealed || selected)
    ) {
        Box(
            Modifier.fillMaxWidth(fill.coerceAtLeast(0.001f)).height(48.dp)
                .background(if (selected) CityColors.IndigoSoft else CityColors.Canvas, RoundedCornerShape(12.dp))
        )
        Row(Modifier.height(48.dp).padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 8.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text("$percent%", style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun Helpline(number: String, label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(14.dp), color = CityColors.Canvas) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = CityColors.Indigo, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Call, null, tint = CityColors.Ink, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(number, style = MaterialTheme.typography.titleSmall)
            }
            Text(label, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
        }
    }
}

@Composable
private fun ServiceTile(icon: ImageVector, title: String, sub: String, tint: Color, bg: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(14.dp), color = bg) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, tint, CityColors.SurfaceHigh, size = 40)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 1)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CityColors.Muted, modifier = Modifier.size(18.dp))
        }
    }
}
