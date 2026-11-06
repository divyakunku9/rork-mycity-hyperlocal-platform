package com.rork.mycity.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rork.mycity.data.CityEvent
import com.rork.mycity.data.CityViewModel
import com.rork.mycity.data.DayBucket
import com.rork.mycity.data.EventCategory
import com.rork.mycity.ui.components.CityCard
import com.rork.mycity.ui.components.EventImage
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.rork.mycity.ui.components.CitySearchField
import com.rork.mycity.ui.components.LocationHeader
import com.rork.mycity.ui.components.SoftBadge
import com.rork.mycity.ui.components.TagLabel
import com.rork.mycity.ui.components.rupees
import com.rork.mycity.ui.theme.CityColors

fun categoryIcon(c: EventCategory): ImageVector = when (c) {
    EventCategory.MUSIC -> Icons.Filled.MusicNote
    EventCategory.WORKSHOP -> Icons.Filled.Groups
    EventCategory.FLEA -> Icons.Filled.Storefront
    EventCategory.TECH -> Icons.Filled.Laptop
    EventCategory.CULTURE -> Icons.AutoMirrored.Filled.MenuBook
    EventCategory.ART -> Icons.Filled.Brush
}

fun categoryColors(c: EventCategory): Pair<Color, Color> = when (c) {
    EventCategory.MUSIC -> CityColors.SaffronSoft to CityColors.SaffronText
    EventCategory.TECH -> CityColors.IndigoSoft to CityColors.Indigo
    EventCategory.CULTURE -> CityColors.GreenSoft to CityColors.Green
    EventCategory.ART -> CityColors.SaffronSoft to CityColors.SaffronText
    EventCategory.FLEA -> CityColors.GreenSoft to CityColors.GreenText
    EventCategory.WORKSHOP -> CityColors.IndigoSoft to CityColors.IndigoDeep
}

private val chipCategories = listOf(null, EventCategory.MUSIC, EventCategory.WORKSHOP, EventCategory.FLEA, EventCategory.TECH)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(vm: CityViewModel, onOpenEvent: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    var query by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var bucket by rememberSaveable { mutableStateOf(DayBucket.TODAY) }
    val category = categoryName?.let { EventCategory.valueOf(it) }

    val featured = vm.events.firstOrNull { it.featured }
    val filtered = vm.events.filter { e ->
        e.id != featured?.id &&
            (category == null || e.category == category || (category == EventCategory.WORKSHOP && e.category == EventCategory.ART)) &&
            (query.isBlank() || (e.title + e.venue + e.area).contains(query.trim(), ignoreCase = true))
    }
    val listForBucket = filtered.filter { it.bucket == bucket }
    var adding by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(CityColors.Canvas),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.statusBarsPadding()) {
                LocationHeader(title = vm.locationLabel, gps = vm.locationSource == com.rork.mycity.data.LocationSource.GPS, locating = vm.locating, vm = vm, onClick = { vm.showLocationPicker = true })
                CitySearchField(query, { query = it }, "Search gigs, flea markets, tech meetups")
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chipCategories) { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            categoryName = c?.name
                        },
                        label = { Text(c?.label ?: "All Events") },
                        leadingIcon = if (c != null) { { Icon(categoryIcon(c), null, Modifier.size(18.dp)) } } else null,
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
        if (featured != null && category == null && query.isBlank()) {
            item {
                FeaturedEventCard(featured, booked = (vm.bookings[featured.id] ?: 0) > 0) { onOpenEvent(featured.id) }
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                DayBucket.entries.forEachIndexed { i, b ->
                    SegmentedButton(
                        selected = bucket == b,
                        onClick = { bucket = b },
                        shape = SegmentedButtonDefaults.itemShape(i, DayBucket.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = CityColors.IndigoSoft,
                            activeContentColor = CityColors.IndigoDeep,
                            inactiveContainerColor = CityColors.Surface,
                        ),
                        label = { Text(b.label) },
                    )
                }
            }
        }
        item {
            AnimatedContent(
                targetState = listForBucket,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "bucket",
            ) { list ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (list.isEmpty()) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Outlined.EventBusy, null, tint = CityColors.Muted, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Nothing matching ${bucket.label.lowercase()}", style = MaterialTheme.typography.titleMedium)
                            Text("Try another day or category", style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
                        }
                    }
                    list.forEach { e ->
                        EventRow(
                            e, saved = vm.savedEvents[e.id] == true, booked = (vm.bookings[e.id] ?: 0) > 0,
                            onSave = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.toggleSaveEvent(e.id)
                            },
                            onOpen = { onOpenEvent(e.id) },
                        )
                    }
                }
            }
        }
    }

    ExtendedFloatingActionButton(
        onClick = { adding = true },
        expanded = !listState.canScrollBackward || listState.firstVisibleItemIndex < 2,
        icon = { Icon(Icons.Filled.Add, null) },
        text = { Text("Add Event") },
        containerColor = CityColors.Saffron,
        contentColor = Color.White,
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
    )
    }

    if (adding) {
        AddEventSheet(
            vm = vm,
            onDismiss = { adding = false },
            onCreated = { _, b ->
                adding = false
                bucket = b
                query = ""
                categoryName = null
                scope.launch { listState.animateScrollToItem(3) }
            },
        )
    }
}

@Composable
private fun FeaturedEventCard(e: CityEvent, booked: Boolean, onOpen: () -> Unit) {
    CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), onClick = onOpen) {
        Column {
            Box {
                EventImage(e, Modifier.fillMaxWidth().height(190.dp), description = e.title)
                if (e.fastFilling) {
                    SoftBadge(
                        "Fast filling", CityColors.Saffron, Color.White, icon = Icons.Filled.LocalFireDepartment,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            Column(Modifier.padding(16.dp)) {
                TagLabel("Featured event", CityColors.Saffron)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Text(e.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text("From ${rupees(e.priceRupees)}", style = MaterialTheme.typography.titleSmall, color = CityColors.Ink)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("${e.dateLabel} • ${e.timeLabel}", style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Place, null, tint = CityColors.Muted, modifier = Modifier.size(16.dp))
                            Text("${e.venue}, ${e.area}", style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (booked) {
                        SoftBadge("Booked", CityColors.GreenSoft, CityColors.Green, icon = Icons.Filled.CheckCircle)
                    } else {
                        Button(onClick = onOpen) { Text("Book Now") }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(e: CityEvent, saved: Boolean, booked: Boolean, onSave: () -> Unit, onOpen: () -> Unit) {
    CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), onClick = onOpen) {
        Row(Modifier.padding(10.dp)) {
            Box {
                EventImage(e, Modifier.size(width = 104.dp, height = 112.dp).clip(RoundedCornerShape(14.dp)))
                if (e.hostedByYou) {
                    Text(
                        "YOURS", style = MaterialTheme.typography.labelSmall, color = Color.White,
                        modifier = Modifier.padding(6.dp).clip(CircleShape).background(CityColors.Saffron).padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text(e.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = onSave, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (saved) "Remove bookmark" else "Bookmark", tint = CityColors.Indigo,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = CityColors.Muted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${e.dateLabel} • ${e.timeLabel}", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Place, null, tint = CityColors.Muted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(e.venue, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val (bg, fg) = categoryColors(e.category)
                    if (e.hostedByYou) SoftBadge("Hosted by you", CityColors.SaffronSoft, CityColors.SaffronText, icon = Icons.Filled.Star)
                    else SoftBadge(e.category.label, bg, fg, icon = categoryIcon(e.category))
                    if (booked) SoftBadge("Booked", CityColors.GreenSoft, CityColors.Green)
                    else SoftBadge(if (e.priceRupees == 0) "Free" else rupees(e.priceRupees), CityColors.IndigoSoft.copy(alpha = 0.6f), CityColors.Ink)
                }
            }
        }
    }
}
