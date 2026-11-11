package com.sheharsaathi.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sheharsaathi.app.data.CityViewModel
import com.sheharsaathi.app.data.CivicAction
import com.sheharsaathi.app.data.CivicService
import com.sheharsaathi.app.data.Geocoder
import com.sheharsaathi.app.data.LocationSource
import com.sheharsaathi.app.data.Ward
import com.sheharsaathi.app.data.WardFinder
import com.sheharsaathi.app.data.WardSource
import com.sheharsaathi.app.ui.components.CityCard
import com.sheharsaathi.app.ui.components.CitySearchField
import com.sheharsaathi.app.ui.components.IconTile
import com.sheharsaathi.app.ui.components.SoftBadge
import com.sheharsaathi.app.ui.components.TagLabel
import com.sheharsaathi.app.ui.components.dial
import com.sheharsaathi.app.ui.components.openUrl
import com.sheharsaathi.app.ui.theme.CityColors

/** "Your ward" card at the top of the Civic Hub (Hyderabad). */
@Composable
fun WardCard(vm: CityViewModel, onLocate: () -> Unit) {
    val match = vm.myWard
    CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp).semantics { contentDescription = "Your ward" }) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.HowToVote, null, tint = CityColors.Indigo, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                TagLabel("Your ward", CityColors.Indigo, Modifier.weight(1f))
                if (match != null) TextButton(onClick = { vm.showWardPicker = true }) { Text("Change ward", color = CityColors.Indigo) }
            }
            when {
                match != null -> WardDetails(vm, match.ward, match.source, match.byName, onLocate)
                vm.wardsLoading || (vm.locating && vm.wards.isEmpty()) -> Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = CityColors.Indigo)
                    Spacer(Modifier.width(10.dp))
                    Text("Finding your ward…", style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
                }
                else -> WardUnknown(vm, onLocate)
            }
        }
    }
}

@Composable
private fun WardUnknown(vm: CityViewModel, onLocate: () -> Unit) {
    val outside = vm.locationSource != LocationSource.DEFAULT && vm.wards.isNotEmpty()
    Text(
        if (outside) "${vm.userPlace.name} isn't inside the GHMC, Cyberabad or Malkajgiri wards. Choose your ward from the list."
        else "Share your location to see your ward number and who represents it, or choose your ward from the list.",
        style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onLocate, enabled = !vm.locating, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.GpsFixed, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
            Text(if (vm.locating) "Locating…" else "Use location", maxLines = 1)
        }
        OutlinedButton(onClick = { vm.showWardPicker = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.EditLocationAlt, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
            Text("Choose ward", maxLines = 1)
        }
    }
}

@Composable
private fun WardDetails(vm: CityViewModel, w: Ward, source: WardSource, byName: Boolean, onLocate: () -> Unit) {
    val body = vm.civicBodyFor(w)
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
        Text("Ward ${w.no}", style = MaterialTheme.typography.headlineMedium, color = CityColors.Ink)
        Spacer(Modifier.width(10.dp))
        Text(w.name, style = MaterialTheme.typography.titleLarge, color = CityColors.Ink, modifier = Modifier.padding(bottom = 3.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Text(
        "${body?.name ?: w.corporation} · ${w.circle} circle, ${w.zone} zone",
        style = MaterialTheme.typography.bodySmall, color = CityColors.Muted,
    )
    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = CityColors.Outline)

    // Corporator: shown when one is elected; otherwise say so plainly and who runs the ward now.
    Row(verticalAlignment = Alignment.Top) {
        IconTile(Icons.Filled.Person, CityColors.Saffron, CityColors.SaffronSoft, size = 36)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Ward corporator", style = MaterialTheme.typography.labelMedium, color = CityColors.Muted)
            if (w.corporator != null) {
                Text(w.corporator + (w.corporatorParty?.let { " ($it)" } ?: ""), style = MaterialTheme.typography.titleMedium)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("None elected right now", style = MaterialTheme.typography.titleMedium)
                }
                body?.councilNote?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, modifier = Modifier.padding(top = 2.dp)) }
            }
        }
    }
    if (w.corporator == null && body != null) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Top) {
            IconTile(Icons.Filled.AccountBalance, CityColors.Indigo, CityColors.IndigoSoft, size = 36)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Running your ward now", style = MaterialTheme.typography.labelMedium, color = CityColors.Muted)
                body.headName?.let { Text("${body.headTitle}: $it", style = MaterialTheme.typography.bodyMedium) }
                body.specialOfficer?.let { Text("Special Officer: $it", style = MaterialTheme.typography.bodyMedium) }
                body.asOf?.let { Text("As reported in $it", style = MaterialTheme.typography.labelSmall, color = CityColors.Muted) }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CityColors.Canvas).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (source == WardSource.CHOSEN) Icons.Filled.TouchApp else Icons.Filled.GpsFixed, null, tint = CityColors.Muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            when {
                source == WardSource.CHOSEN -> "You chose this ward"
                byName -> "From your location (${Geocoder.headerLabel(vm.userPlace).substringBefore(",")})"
                else -> "From your location: nearest ward. Near a ward edge? Check with Change ward."
            },
            style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, modifier = Modifier.weight(1f),
        )
        if (source == WardSource.CHOSEN) {
            Text(
                "Use location", style = MaterialTheme.typography.labelLarge, color = CityColors.Indigo,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                    vm.followLocationForWard()
                    if (vm.locationSource == LocationSource.DEFAULT) onLocate()
                }.padding(6.dp),
            )
        }
    }
}

/** Searchable list of all 300 Hyderabad wards. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardPickerSheet(vm: CityViewModel) {
    if (!vm.showWardPicker) return
    var query by remember { mutableStateOf("") }
    val results = remember(query, vm.wards) { WardFinder.search(query, vm.wards) }
    // A new search starts at the top; otherwise the list keeps its old place and the best match sits off screen.
    val listState = rememberLazyListState()
    LaunchedEffect(query) { listState.scrollToItem(0) }
    ModalBottomSheet(
        onDismissRequest = { vm.showWardPicker = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CityColors.Surface,
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).imePadding()) {
            Text("Choose your ward", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 20.dp))
            Text(
                "All ${vm.wards.size} wards of GHMC, Cyberabad and Malkajgiri corporations. Search by ward number, area or circle.",
                style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            CitySearchField(query, { query = it }, "e.g. 231, Kondapur, Malakpet")
            if (vm.wardsLoading) Row(Modifier.padding(20.dp)) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) }
            LazyColumn(Modifier.fillMaxWidth().weight(1f), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
                if (results.isEmpty() && !vm.wardsLoading) item {
                    Text("No ward matches “${query.trim()}”.", style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted, modifier = Modifier.padding(20.dp))
                }
                items(results, key = { it.no }) { w ->
                    val selected = vm.myWard?.ward?.no == w.no
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.chooseWard(w.no) }
                            .background(if (selected) CityColors.IndigoSoft.copy(alpha = 0.5f) else Color.Transparent)
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(CityColors.IndigoSoft), contentAlignment = Alignment.Center) {
                            Text("${w.no}", style = MaterialTheme.typography.titleSmall, color = CityColors.IndigoDeep)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(w.name, style = MaterialTheme.typography.titleSmall)
                            Text("${w.circle} circle · ${vm.civicBodies[w.corporation]?.shortName ?: w.corporation}",
                                style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                        }
                        if (selected) SoftBadge("Yours", CityColors.IndigoSoft, CityColors.IndigoDeep)
                    }
                }
            }
        }
    }
}

/** The options behind a Civic Hub tile: sites to open, numbers to call or WhatsApp. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CivicServiceSheet(service: CivicService, onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CityColors.Surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(service.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 20.dp))
            Text(service.intro, style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            service.actions.forEach { a ->
                val (icon, tint) = when (a.kind) {
                    CivicAction.Kind.WEB -> Icons.AutoMirrored.Filled.OpenInNew to CityColors.Indigo
                    CivicAction.Kind.CALL -> Icons.Filled.Call to CityColors.Green
                    CivicAction.Kind.WHATSAPP -> Icons.AutoMirrored.Filled.Chat to CityColors.Green
                }
                ActionRow(icon, tint, a.title, a.sub) {
                    when (a.kind) {
                        CivicAction.Kind.WEB -> openUrl(context, a.target)
                        CivicAction.Kind.CALL -> dial(context, a.target)
                        CivicAction.Kind.WHATSAPP -> openUrl(context, "https://wa.me/${a.target}")
                    }
                }
            }
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, null, tint = CityColors.Muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Official sites can be slow or busy at times. If one doesn't load, try again later.",
                    style = MaterialTheme.typography.labelSmall, color = CityColors.Muted)
            }
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, tint: Color, title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClickLabel = title, onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, tint, CityColors.SurfaceHigh, size = 40)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CityColors.Muted)
    }
}
