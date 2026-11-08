package com.sheharsaathi.app.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sheharsaathi.app.data.CityViewModel
import com.sheharsaathi.app.data.CurrentCity
import com.sheharsaathi.app.data.FeedItem
import com.sheharsaathi.app.data.IssueCategory
import com.sheharsaathi.app.data.IssueStatus
import com.sheharsaathi.app.data.ListedTime
import com.sheharsaathi.app.data.NearbyFeed
import com.sheharsaathi.app.data.backend.Times
import com.sheharsaathi.app.ui.components.CityCard
import com.sheharsaathi.app.ui.components.SectionHeader
import com.sheharsaathi.app.ui.components.openDirections
import com.sheharsaathi.app.ui.components.shareText
import com.sheharsaathi.app.ui.theme.CityColors

private fun kmLabel(km: Double?): String? = km?.let { if (it < 1) "${(it * 1000).toInt()} m" else "%.1f km".format(it) }

private fun IssueCategory.icon(): ImageVector = when (this) {
    IssueCategory.POTHOLE -> Icons.Filled.Construction
    IssueCategory.STREETLIGHT -> Icons.Filled.Lightbulb
    IssueCategory.GARBAGE -> Icons.Filled.Delete
    IssueCategory.WATER -> Icons.Filled.WaterDrop
}

private fun statusColor(s: IssueStatus): Color = when (s) {
    IssueStatus.RESOLVED -> CityColors.GreenText
    IssueStatus.IN_PROGRESS -> CityColors.Saffron
    IssueStatus.UNDER_REVIEW -> CityColors.Indigo
    IssueStatus.REPORTED -> CityColors.Red
}

/** What opening a card does; Explore and the "near you" page share these. */
class FeedActions(
    val openEvent: (String) -> Unit,
    val openIssue: (String) -> Unit,
    val openListed: (Long) -> Unit,
    val openStory: (String) -> Unit,
)

/**
 * One card: picture (or a coloured icon), a type tag, title and two short lines.
 * The same card is used for events, listed events, civic reports and news.
 */
@Composable
private fun FeedCard(
    width: Dp?,
    picture: Any?,            // image URL / Uri string, a drawable id, or null for the icon
    icon: ImageVector,
    tint: Color,
    tag: String,
    corner: String?,          // small badge on the picture (distance, "Today"…)
    title: String,
    line1: String,
    line2: String,
    line2Color: Color,
    describe: String,
    onClick: () -> Unit,
) {
    val m = if (width != null) Modifier.width(width) else Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    CityCard(m.semantics { contentDescription = describe }, onClick = onClick) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(if (width != null) 128.dp else 160.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)).background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = tint.copy(alpha = 0.85f), modifier = Modifier.size(40.dp))
                when (picture) {
                    is Int -> Image(painterResource(picture), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    is String -> AsyncImage(picture, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Row(
                    Modifier.align(Alignment.TopStart).padding(8.dp).clip(CircleShape).background(Color(0xCC0F172A)).padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tag, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
                }
                corner?.let {
                    Text(
                        it, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(CircleShape).background(Color(0xCC0F172A)).padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
                // Cards in a row keep two title lines so the row lines up.
                Text(title, style = MaterialTheme.typography.titleSmall, color = CityColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    minLines = if (width != null) 2 else 1)
                Spacer(Modifier.height(4.dp))
                Text(line1, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(line2, style = MaterialTheme.typography.labelMedium, color = line2Color, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Draws any feed item as a card. */
@Composable
fun FeedItemCard(item: FeedItem, actions: FeedActions, width: Dp? = 252.dp) {
    when (item) {
        is FeedItem.Event -> {
            val e = item.e
            FeedCard(
                width, e.imageUri ?: e.imageRes, Icons.Filled.Event, CityColors.Indigo,
                tag = "Event · ${e.category.label}", corner = kmLabel(item.km),
                title = e.title, line1 = "${e.dateLabel} · ${e.timeLabel} · ${e.venue}",
                line2 = if (e.priceRupees == 0) "Free · on Shehar Saathi" else "₹${e.priceRupees} · on Shehar Saathi",
                line2Color = CityColors.Saffron, describe = "Feed event: ${e.title}",
            ) { actions.openEvent(e.id) }
        }
        is FeedItem.Listed -> {
            val e = item.e
            FeedCard(
                width, e.imageUrl, e.category.icon(), e.category.color(),
                tag = e.category.label, corner = kmLabel(item.km),
                title = e.title, line1 = ListedTime.whenLabel(e) + (e.venue?.let { " · $it" } ?: ""),
                line2 = "${e.priceLabel} · ${e.bookLabel} on ${e.source}",
                line2Color = if (e.isFree == true) CityColors.GreenText else CityColors.Saffron, describe = "Feed listed event: ${e.title}",
            ) { actions.openListed(e.id) }
        }
        is FeedItem.Issue -> {
            val i = item.i
            FeedCard(
                width, i.imageUri ?: i.imageRes, i.category.icon(), CityColors.Red,
                tag = "Civic · ${i.category.label}", corner = kmLabel(item.km),
                title = i.title, line1 = "${i.location} · ${i.reportedAgo}",
                line2 = "${i.status.label} · ${i.upvotes} confirmations",
                line2Color = statusColor(i.status), describe = "Feed civic report: ${i.title}",
            ) { actions.openIssue(i.id) }
        }
        is FeedItem.News -> {
            val s = item.s
            FeedCard(
                width, s.imageUrl, s.category.icon(), s.category.color(),
                tag = if (item.local) "Local news" else s.category.label, corner = Times.ago(s.publishedMs),
                title = s.title, line1 = s.summary ?: s.category.label,
                line2 = s.source, line2Color = CityColors.Muted, describe = "Feed news: ${s.title}",
            ) { actions.openStory(s.url) }
        }
    }
}

/** A titled, sideways-scrolling row of cards with "See all". */
@Composable
fun FeedSection(title: String, items: List<FeedItem>, empty: String, actions: FeedActions, onSeeAll: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title, action = if (onSeeAll != null) "See all" else null, onAction = onSeeAll)
        if (items.isEmpty()) {
            Text(empty, style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted, modifier = Modifier.padding(horizontal = 20.dp))
        } else {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items, key = { it.key }) { FeedItemCard(it, actions) }
            }
        }
    }
}

/** "See all" for Happening near: everything within reach of the user's spot, as full lists. */
@Composable
fun NearbyScreen(vm: CityViewModel, onBack: () -> Unit, onOpenEvent: (String) -> Unit, onOpenIssue: (String) -> Unit) {
    val context = LocalContext.current
    var openListed by rememberSaveable { mutableStateOf<Long?>(null) }
    val feed = remember(vm.userPlace, vm.events, vm.listedEvents, vm.issues, vm.webNews) {
        NearbyFeed.build(vm.userPlace, CurrentCity.city.name, vm.events, vm.listedEvents, vm.issues, vm.webNews)
    }
    val actions = FeedActions(onOpenEvent, onOpenIssue, { openListed = it }, { openStory(context, it) })
    var tab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Events (${feed.eventsNear.size})", "Civic (${feed.issuesNear.size})", "News (${feed.newsNear.size})")
    val list: List<FeedItem> = when (tab) { 0 -> feed.eventsNear; 1 -> feed.issuesNear; else -> feed.newsNear }

    Column(Modifier.fillMaxSize().background(CityColors.Canvas)) {
        Row(Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = CityColors.Ink) }
            Column(Modifier.weight(1f)) {
                Text("Near ${feed.area}", style = MaterialTheme.typography.titleLarge, color = CityColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Within ${NearbyFeed.RADIUS_KM.toInt()} km of you, plus news that mentions ${feed.area}", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
            }
        }
        androidx.compose.material3.PrimaryTabRow(selectedTabIndex = tab, containerColor = CityColors.Canvas, contentColor = CityColors.Ink) {
            tabs.forEachIndexed { i, t ->
                androidx.compose.material3.Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t, maxLines = 1) },
                    selectedContentColor = CityColors.Ink, unselectedContentColor = CityColors.Muted)
            }
        }
        LazyColumn(
            Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (list.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(when (tab) { 0 -> Icons.Filled.Event; 1 -> Icons.Filled.Warning; else -> Icons.Filled.Newspaper }, null, tint = CityColors.Muted, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when (tab) {
                            0 -> "No events listed within ${NearbyFeed.RADIUS_KM.toInt()} km right now. The Events tab has the whole city."
                            1 -> "No open civic reports within ${NearbyFeed.RADIUS_KM.toInt()} km. Spot a problem? Use the Report tab."
                            else -> "No recent news mentions ${feed.area}. The News Hub has the whole city."
                        },
                        style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted,
                    )
                }
            }
            items(list, key = { it.key }) { FeedItemCard(it, actions, width = null) }
        }
    }

    vm.listedEvents.firstOrNull { it.id == openListed }?.let { e ->
        ListedEventSheet(
            e, onDismiss = { openListed = null }, onBook = { openStory(context, e.url) },
            onShare = { shareText(context, "${e.title}\n${ListedTime.whenLabel(e)}${e.venue?.let { " · $it" } ?: ""}\n${e.url}\n— via Shehar Saathi") },
            onDirections = if (e.lat != null && e.lng != null) ({ openDirections(context, e.lat, e.lng, e.venue ?: e.title) }) else null,
        )
    }
}
