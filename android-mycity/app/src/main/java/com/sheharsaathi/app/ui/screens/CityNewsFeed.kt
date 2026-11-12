package com.sheharsaathi.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.sheharsaathi.app.data.NewsTopic
import com.sheharsaathi.app.data.WebStory
import com.sheharsaathi.app.data.backend.Times
import com.sheharsaathi.app.ui.components.CityCard
import com.sheharsaathi.app.ui.components.openUrl
import com.sheharsaathi.app.ui.components.shareText
import com.sheharsaathi.app.ui.theme.CityColors

/** Colour and icon for each topic; also used when a story has no picture. */
internal fun NewsTopic.color(): Color = when (this) {
    NewsTopic.CIVIC -> Color(0xFF38BDF8)
    NewsTopic.TRAFFIC -> Color(0xFFF59E0B)
    NewsTopic.WEATHER -> Color(0xFF60A5FA)
    NewsTopic.CRIME -> Color(0xFFEF4444)
    NewsTopic.POLITICS -> Color(0xFFA78BFA)
    NewsTopic.BUSINESS -> Color(0xFF22C55E)
    NewsTopic.EDUCATION -> Color(0xFF2DD4BF)
    NewsTopic.HEALTH -> Color(0xFFF472B6)
    NewsTopic.ENTERTAINMENT -> Color(0xFFF58220)
    NewsTopic.SPORTS -> Color(0xFF84CC16)
    NewsTopic.CITY -> Color(0xFF94A3B8)
}

internal fun NewsTopic.icon(): ImageVector = when (this) {
    NewsTopic.CIVIC -> Icons.Filled.LocationCity
    NewsTopic.TRAFFIC -> Icons.Filled.DirectionsBus
    NewsTopic.WEATHER -> Icons.Filled.Cloud
    NewsTopic.CRIME -> Icons.Filled.Shield
    NewsTopic.POLITICS -> Icons.Filled.Gavel
    NewsTopic.BUSINESS -> Icons.Filled.BusinessCenter
    NewsTopic.EDUCATION -> Icons.Filled.School
    NewsTopic.HEALTH -> Icons.Filled.LocalHospital
    NewsTopic.ENTERTAINMENT -> Icons.Filled.TheaterComedy
    NewsTopic.SPORTS -> Icons.Filled.SportsCricket
    NewsTopic.CITY -> Icons.Filled.Newspaper
}

/** Opens the story in an in-app browser tab (like Chrome's feed); falls back to any browser. */
fun openStory(context: Context, url: String) {
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(0xFF0F172A.toInt()).build())
            .build()
            .launchUrl(context, Uri.parse(url))
    } catch (_: Exception) {
        openUrl(context, url)
    }
}

/** The "City news" feed: latest headlines from local news sites, sorted by topic. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityNewsFeed(vm: CityViewModel, header: LazyListScope.() -> Unit) {
    val context = LocalContext.current
    var topic by rememberSaveable { mutableStateOf<String?>(null) } // null = "For you"
    val all = vm.webNews
    val topics = remember(all) {
        all.groupingBy { it.category }.eachCount().entries
            .sortedByDescending { it.value }.map { it.key }
            .let { list -> list.filter { it != NewsTopic.CITY } + list.filter { it == NewsTopic.CITY } }
    }
    val selected = topic?.let { NewsTopic.of(it) }
    val stories = if (selected == null) all else all.filter { it.category == selected }
    val listState = rememberLazyListState()
    LaunchedEffect(topic) { if (listState.firstVisibleItemIndex > 2) listState.scrollToItem(2) }

    PullToRefreshBox(
        isRefreshing = vm.webNewsLoading && all.isNotEmpty(),
        onRefresh = { vm.refreshWebNews() },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            Modifier.fillMaxSize().background(CityColors.Canvas),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            header()
            if (all.isNotEmpty()) item(key = "topics") {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { TopicChip("For you", null, topic == null) { topic = null } }
                    items(topics, key = { it.key }) { t ->
                        TopicChip(t.label, t, topic == t.key) { topic = t.key }
                    }
                }
            }
            when {
                all.isEmpty() && vm.webNewsLoading -> items(4) { SkeletonCard(big = it == 0) }
                all.isEmpty() -> item(key = "empty") {
                    EmptyFeed(
                        if (vm.isLive) "Couldn't load city news" else "City news is coming to ${vm.city.name}",
                        if (vm.isLive) "Check your connection, then pull down to try again."
                        else "Headlines from local news sites are live in Hyderabad first. Community posts are in the Community tab.",
                    )
                }
                else -> {
                    item(key = "meta-$topic") {
                        val sources = stories.map { it.source }.distinct().size
                        Text(
                            buildString {
                                append("${stories.size} stories from $sources ${if (sources == 1) "source" else "sources"}")
                                if (vm.webNewsUpdatedMs > 0) append(" · updated ${Times.ago(vm.webNewsUpdatedMs).lowercase()}")
                            },
                            style = MaterialTheme.typography.bodySmall, color = CityColors.Muted,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                    // Chrome-style rhythm: a large picture card leads, then compact cards with a
                    // thumbnail, with another large card every so often.
                    val hero = stories.indexOfFirst { it.imageUrl != null }.takeIf { it in 0..2 } ?: 0
                    val ordered = listOf(stories[hero]) + stories.filterIndexed { i, _ -> i != hero }
                    ordered.forEachIndexed { i, s ->
                        item(key = s.id) {
                            val large = i == 0 || (i % 7 == 0 && s.imageUrl != null)
                            val open = { openStory(context, s.url) }
                            val share = { shareText(context, "${s.title}\n${s.url}\n— via Shehar Saathi") }
                            val saved = vm.isStorySaved(s)
                            val save = { vm.toggleSavedStory(s) }
                            Box(Modifier.animateItem()) {
                                if (large) LargeStoryCard(s, saved, open, share, save) else CompactStoryCard(s, saved, open, share, save)
                            }
                        }
                    }
                    item(key = "credit") {
                        Text(
                            "Headlines and pictures belong to their publishers. Tap a story to read it on their site.",
                            style = MaterialTheme.typography.bodySmall, color = CityColors.Muted,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicChip(label: String, t: NewsTopic?, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(t?.icon() ?: Icons.AutoMirrored.Filled.TrendingUp, null, Modifier.size(16.dp), tint = if (selected) Color.White else (t?.color() ?: CityColors.Indigo))
        },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = CityColors.Surface,
            selectedContainerColor = CityColors.Indigo,
            selectedLabelColor = Color.White,
        ),
    )
}

/** Story picture; shows the topic's colour and icon while loading or when there's no picture. */
@Composable
private fun StoryImage(s: WebStory, modifier: Modifier, iconSize: Int) {
    val c = s.category.color()
    Box(modifier.background(c.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        Icon(s.category.icon(), null, tint = c.copy(alpha = 0.8f), modifier = Modifier.size(iconSize.dp))
        if (s.imageUrl != null) AsyncImage(s.imageUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun SourceLine(s: WebStory) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(18.dp).background(sourceColor(s.source), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(s.source.take(1).uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(6.dp))
        Text(s.source, style = MaterialTheme.typography.labelMedium, color = CityColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Text(" · ${Times.ago(s.publishedMs)}", style = MaterialTheme.typography.labelMedium, color = CityColors.Muted, maxLines = 1)
    }
}

private fun sourceColor(name: String): Color {
    val palette = listOf(0xFF2563EB, 0xFFDC2626, 0xFF7C3AED, 0xFF059669, 0xFFD97706, 0xFF0891B2, 0xFFDB2777)
    return Color(palette[(name.hashCode() and 0x7fffffff) % palette.size])
}

@Composable
private fun TopicTag(t: NewsTopic) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(t.color(), CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(t.label, style = MaterialTheme.typography.labelSmall, color = t.color())
    }
}

@Composable
private fun CardActions(s: WebStory, saved: Boolean, onShare: () -> Unit, onSave: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { TopicTag(s.category) }
        IconButton(onClick = onSave, modifier = Modifier.size(36.dp)) {
            Icon(
                if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                if (saved) "Remove saved story" else "Save story",
                tint = if (saved) CityColors.Saffron else CityColors.Muted, modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Share, "Share story", tint = CityColors.Muted, modifier = Modifier.size(18.dp))
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = CityColors.Muted, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun LargeStoryCard(s: WebStory, saved: Boolean, onOpen: () -> Unit, onShare: () -> Unit, onSave: () -> Unit) {
    CityCard(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).semantics { contentDescription = "Story: ${s.title}" },
        onClick = onOpen,
    ) {
        Column {
            StoryImage(s, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)), 48)
            Column(Modifier.padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 6.dp)) {
                SourceLine(s)
                Spacer(Modifier.height(8.dp))
                Text(s.title, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(end = 6.dp))
                s.summary?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(end = 6.dp))
                }
                Spacer(Modifier.height(4.dp))
                CardActions(s, saved, onShare, onSave)
            }
        }
    }
}

@Composable
private fun CompactStoryCard(s: WebStory, saved: Boolean, onOpen: () -> Unit, onShare: () -> Unit, onSave: () -> Unit) {
    CityCard(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).semantics { contentDescription = "Story: ${s.title}" },
        onClick = onOpen,
    ) {
        Column(Modifier.padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 4.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    SourceLine(s)
                    Spacer(Modifier.height(6.dp))
                    Text(s.title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    s.summary?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.width(12.dp))
                StoryImage(s, Modifier.padding(top = 2.dp).size(92.dp).clip(RoundedCornerShape(12.dp)), 32)
            }
            CardActions(s, saved, onShare, onSave)
        }
    }
}

@Composable
private fun SkeletonCard(big: Boolean) {
    CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(if (big) 0.dp else 14.dp)) {
            if (big) Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(CityColors.SurfaceHigh))
            Column(Modifier.padding(if (big) 16.dp else 0.dp)) {
                Box(Modifier.width(120.dp).height(10.dp).background(CityColors.SurfaceHigh, RoundedCornerShape(5.dp)))
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(14.dp).background(CityColors.SurfaceHigh, RoundedCornerShape(7.dp)))
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth(0.7f).height(14.dp).background(CityColors.SurfaceHigh, RoundedCornerShape(7.dp)))
            }
        }
    }
}

@Composable
private fun EmptyFeed(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Newspaper, null, tint = CityColors.Muted, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
    }
}

