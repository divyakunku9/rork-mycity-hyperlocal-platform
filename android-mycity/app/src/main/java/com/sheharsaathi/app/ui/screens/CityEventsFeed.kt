package com.sheharsaathi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Festival
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sheharsaathi.app.data.CityViewModel
import com.sheharsaathi.app.data.ListedCategory
import com.sheharsaathi.app.data.ListedEvent
import com.sheharsaathi.app.data.ListedTime
import com.sheharsaathi.app.data.ListedWhen
import com.sheharsaathi.app.ui.components.CityCard
import com.sheharsaathi.app.ui.components.CitySearchField
import com.sheharsaathi.app.ui.components.openDirections
import com.sheharsaathi.app.ui.components.shareText
import com.sheharsaathi.app.ui.theme.CityColors

internal fun ListedCategory.color(): Color = when (this) {
    ListedCategory.MUSIC -> Color(0xFFF58220)
    ListedCategory.COMEDY -> Color(0xFFFACC15)
    ListedCategory.THEATRE -> Color(0xFFF472B6)
    ListedCategory.TECH -> Color(0xFF6C7BFF)
    ListedCategory.WORKSHOP -> Color(0xFF2DD4BF)
    ListedCategory.FESTIVAL -> Color(0xFFA78BFA)
    ListedCategory.FOOD -> Color(0xFFFB923C)
    ListedCategory.NIGHTLIFE -> Color(0xFFC084FC)
    ListedCategory.SPORTS -> Color(0xFF84CC16)
    ListedCategory.ART -> Color(0xFF38BDF8)
    ListedCategory.KIDS -> Color(0xFF4ADE80)
    ListedCategory.SPIRITUAL -> Color(0xFFFDE68A)
    ListedCategory.OTHER -> Color(0xFF94A3B8)
}

internal fun ListedCategory.icon(): ImageVector = when (this) {
    ListedCategory.MUSIC -> Icons.Filled.MusicNote
    ListedCategory.COMEDY -> Icons.Filled.Mic
    ListedCategory.THEATRE -> Icons.Filled.TheaterComedy
    ListedCategory.TECH -> Icons.Filled.Laptop
    ListedCategory.WORKSHOP -> Icons.Filled.Groups
    ListedCategory.FESTIVAL -> Icons.Filled.Festival
    ListedCategory.FOOD -> Icons.Filled.Restaurant
    ListedCategory.NIGHTLIFE -> Icons.Filled.Nightlife
    ListedCategory.SPORTS -> Icons.Filled.SportsSoccer
    ListedCategory.ART -> Icons.Filled.Brush
    ListedCategory.KIDS -> Icons.Filled.ChildCare
    ListedCategory.SPIRITUAL -> Icons.Filled.SelfImprovement
    ListedCategory.OTHER -> Icons.Filled.AutoAwesome
}

/**
 * "Around the city": events listed on District, Eventbrite, Meetup and AllEvents.
 * Booking happens on those sites; this list only shows them and links out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityEventsFeed(vm: CityViewModel, header: LazyListScope.() -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var whenName by rememberSaveable { mutableStateOf(ListedWhen.ANY.name) }
    var catKey by rememberSaveable { mutableStateOf<String?>(null) }
    var freeOnly by rememberSaveable { mutableStateOf(false) }
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    val w = ListedWhen.valueOf(whenName)
    val all = vm.listedEvents

    val inWhen = remember(all, w) { val now = System.currentTimeMillis(); all.filter { ListedTime.matches(w, it, now) } }
    val cats = remember(inWhen) {
        inWhen.groupingBy { it.category }.eachCount().entries.sortedByDescending { it.value }
            .map { it.key }.let { l -> l.filter { it != ListedCategory.OTHER } + l.filter { it == ListedCategory.OTHER } }
    }
    val selected = catKey?.let { ListedCategory.of(it) }
    val shown = inWhen.filter { e ->
        (selected == null || e.category == selected) && (!freeOnly || e.isFree == true) &&
            (query.isBlank() || "${e.title} ${e.venue.orEmpty()} ${e.address.orEmpty()} ${e.source}".contains(query.trim(), ignoreCase = true))
    }
    val byDay = remember(shown) {
        val now = System.currentTimeMillis()
        // A show that started earlier and is still running sits under today.
        shown.groupBy { ListedTime.dayKey(maxOf(it.startMs, now)) }.toSortedMap()
    }

    PullToRefreshBox(
        isRefreshing = vm.listedEventsLoading && all.isNotEmpty(),
        onRefresh = { vm.refreshListedEvents() },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            Modifier.fillMaxSize().background(CityColors.Canvas),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            header()
            if (all.isNotEmpty()) {
                item(key = "search") { CitySearchField(query, { query = it }, "Search shows, venues, artists") }
                item(key = "when") {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ListedWhen.entries) { x ->
                            Chip(x.label, null, whenName == x.name) { whenName = x.name }
                        }
                        item { Chip("Free", null, freeOnly) { freeOnly = !freeOnly } }
                    }
                }
                item(key = "cats") {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { Chip("All", Icons.Filled.Event, catKey == null) { catKey = null } }
                        items(cats, key = { it.key }) { c -> Chip(c.label, c.icon(), catKey == c.key, c.color()) { catKey = if (catKey == c.key) null else c.key } }
                    }
                }
            }
            when {
                all.isEmpty() && vm.listedEventsLoading -> items(3) { EventSkeleton() }
                all.isEmpty() -> item(key = "empty") {
                    Empty(
                        if (vm.isLive) "Couldn't load city events" else "City events are coming to ${vm.city.name}",
                        if (vm.isLive) "Check your connection, then pull down to try again."
                        else "Listings from District, Eventbrite, Meetup and AllEvents are live in Hyderabad first. Community events are in the Community tab.",
                    )
                }
                shown.isEmpty() -> item(key = "none") { Empty("Nothing matches", "Try another day, topic or search.") }
                else -> {
                    item(key = "meta") {
                        val sources = shown.map { it.source }.distinct().sorted()
                        Text(
                            "${shown.size} events · from ${sources.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall, color = CityColors.Muted,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                    byDay.forEach { (day, list) ->
                        item(key = "day-$day") {
                            Text(
                                ListedTime.dayHeading(maxOf(list.first().startMs, System.currentTimeMillis())),
                                style = MaterialTheme.typography.titleSmall, color = CityColors.Ink, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp),
                            )
                        }
                        items(list, key = { it.id }) { e ->
                            Box(Modifier.animateItem()) {
                                ListedEventCard(
                                    e,
                                    onOpen = { openId = e.id },
                                    onBook = { openStory(context, e.url) },
                                )
                            }
                        }
                    }
                    item(key = "credit") {
                        Text(
                            "Listings come from District, Eventbrite, Meetup and AllEvents and refresh every few hours. " +
                                "Tickets are sold by those sites, not by Shehar Saathi; check details there before you go.",
                            style = MaterialTheme.typography.bodySmall, color = CityColors.Muted,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }

    all.firstOrNull { it.id == openId }?.let { e ->
        ListedEventSheet(
            e,
            onDismiss = { openId = null },
            onBook = { openStory(context, e.url) },
            onShare = { shareText(context, "${e.title}\n${ListedTime.whenLabel(e)}${e.venue?.let { " · $it" } ?: ""}\n${e.url}\n— via Shehar Saathi") },
            onDirections = if (e.lat != null && e.lng != null) ({ openDirections(context, e.lat, e.lng, e.venue ?: e.title) }) else null,
        )
    }
}

@Composable
private fun Chip(label: String, icon: ImageVector?, selected: Boolean, tint: Color = CityColors.Indigo, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = icon?.let { { Icon(it, null, Modifier.size(16.dp), tint = if (selected) Color.White else tint) } },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = CityColors.Surface,
            selectedContainerColor = CityColors.Indigo,
            selectedLabelColor = Color.White,
        ),
    )
}

@Composable
private fun EventPicture(e: ListedEvent, modifier: Modifier, iconSize: Int) {
    val c = e.category.color()
    Box(modifier.background(c.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        Icon(e.category.icon(), null, tint = c.copy(alpha = 0.8f), modifier = Modifier.size(iconSize.dp))
        if (e.imageUrl != null) AsyncImage(e.imageUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ListedEventCard(e: ListedEvent, onOpen: () -> Unit, onBook: () -> Unit) {
    CityCard(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).semantics { contentDescription = "Listed event: ${e.title}" },
        onClick = onOpen,
    ) {
        Row(Modifier.padding(10.dp)) {
            EventPicture(e, Modifier.width(104.dp).height(132.dp).clip(RoundedCornerShape(14.dp)), 32)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(e.category.color(), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(e.category.label, style = MaterialTheme.typography.labelSmall, color = e.category.color(), maxLines = 1)
                }
                Spacer(Modifier.height(4.dp))
                Text(e.title, style = MaterialTheme.typography.titleSmall, color = CityColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Line(Icons.Filled.Schedule, ListedTime.whenLabel(e))
                e.venue?.let { Line(Icons.Filled.Place, it) }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.priceLabel, style = MaterialTheme.typography.labelLarge, color = if (e.isFree == true) CityColors.GreenText else CityColors.Ink, maxLines = 1)
                        Text("on ${e.source}", style = MaterialTheme.typography.labelSmall, color = CityColors.Muted, maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = onBook,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp),
                    ) {
                        Text(e.bookLabel, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Line(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        Icon(icon, null, tint = CityColors.Muted, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListedEventSheet(e: ListedEvent, onDismiss: () -> Unit, onBook: () -> Unit, onShare: () -> Unit, onDirections: (() -> Unit)?) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = CityColors.Surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 16.dp)) {
            EventPicture(e, Modifier.fillMaxWidth().aspectRatio(16f / 9f), 56)
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(e.category.icon(), null, tint = e.category.color(), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(e.category.label, style = MaterialTheme.typography.labelMedium, color = e.category.color())
                }
                Spacer(Modifier.height(6.dp))
                Text(e.title, style = MaterialTheme.typography.headlineSmall, color = CityColors.Ink)
                Spacer(Modifier.height(12.dp))
                Detail(Icons.Filled.Schedule, ListedTime.whenLabel(e), if (!e.hasTime) "Check the time on ${e.source}" else null)
                if (e.venue != null || e.address != null) Detail(Icons.Filled.Place, e.venue ?: e.address!!, e.address?.takeIf { it != e.venue })
                Detail(Icons.Filled.ConfirmationNumber, e.priceLabel, if (e.priceMin != null && e.priceMax != null && e.priceMax > e.priceMin) "Up to ₹${e.priceMax.toInt()}" else null)
                e.summary?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onBook,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CityColors.Saffron, contentColor = Color.White),
                ) {
                    Text(e.actionLabel, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onDirections != null) OutlinedButton(onClick = onDirections, modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Directions")
                    }
                    OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Share")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Listed on ${e.source}. Booking, payment and refunds are handled by ${e.source}, not Shehar Saathi.",
                    style = MaterialTheme.typography.bodySmall, color = CityColors.Muted,
                )
            }
        }
    }
}

@Composable
private fun Detail(icon: ImageVector, text: String, sub: String?) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(32.dp).background(CityColors.SurfaceHigh, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = CityColors.Indigo, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).padding(top = 2.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = CityColors.Ink)
            sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted) }
        }
    }
}

@Composable
private fun EventSkeleton() {
    CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(10.dp)) {
            Box(Modifier.width(104.dp).height(132.dp).clip(RoundedCornerShape(14.dp)).background(CityColors.SurfaceHigh))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).padding(top = 6.dp)) {
                Box(Modifier.width(80.dp).height(10.dp).background(CityColors.SurfaceHigh, RoundedCornerShape(5.dp)))
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(14.dp).background(CityColors.SurfaceHigh, RoundedCornerShape(7.dp)))
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(0.6f).height(12.dp).background(CityColors.SurfaceHigh, RoundedCornerShape(6.dp)))
            }
        }
    }
}

@Composable
private fun Empty(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.EventBusy, null, tint = CityColors.Muted, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
    }
}
