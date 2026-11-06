package com.rork.mycity.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.rork.mycity.data.CityViewModel
import com.rork.mycity.data.NewsItem
import com.rork.mycity.data.NewsKind
import com.rork.mycity.ui.components.CityCard
import com.rork.mycity.ui.components.LocationHeader
import com.rork.mycity.ui.components.SoftBadge
import com.rork.mycity.ui.components.TagLabel
import com.rork.mycity.ui.components.shareText
import com.rork.mycity.ui.theme.CityColors

private enum class NewsFilter(val label: String) { TOP("Top Stories"), BULLETINS("Civic Bulletins"), OFFERS("Business Offers"), WINS("Ward Wins") }

@Composable
fun NewsScreen(vm: CityViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var filter by rememberSaveable { mutableStateOf(NewsFilter.TOP) }

    val items = vm.news.filter {
        when (filter) {
            NewsFilter.TOP -> true
            NewsFilter.BULLETINS -> it.kind == NewsKind.BULLETIN
            NewsFilter.OFFERS -> it.kind == NewsKind.OFFER
            NewsFilter.WINS -> it.kind == NewsKind.CIVIC_WIN
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(CityColors.Canvas),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(Modifier.statusBarsPadding()) {
                LocationHeader(title = vm.locationLabel, gps = vm.locationSource == com.rork.mycity.data.LocationSource.GPS, locating = vm.locating, vm = vm, onClick = { vm.showLocationPicker = true })
                TagLabel("Live city dispatch", CityColors.Indigo, Modifier.padding(start = 20.dp, top = 8.dp))
                Text(
                    "${com.rork.mycity.data.CurrentCity.city.name} Civic & Community News",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(NewsFilter.entries) { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(f.label) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CityColors.Surface,
                            selectedContainerColor = CityColors.Indigo,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
        }
        items(items, key = { it.id }) { n ->
            val endorsed = vm.endorsedNews[n.id] == true
            val saved = vm.savedNews[n.id] == true
            val onEndorse = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.toggleEndorse(n.id)
            }
            Box(Modifier.animateItem()) { when (n.kind) {
                NewsKind.BULLETIN -> UrgentCard(n)
                NewsKind.OFFER -> OfferCard(n, claimed = vm.claimedOffers[n.id] == true) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.claimOffer(n.id)
                }
                else -> StoryCard(
                    n, endorsed, saved,
                    onEndorse = onEndorse,
                    onSave = { vm.toggleSaveNews(n.id) },
                    onShare = { shareText(context, "${n.title} — via MyCity") },
                )
            } }
        }
    }
}

@Composable
private fun UrgentCard(n: NewsItem) {
    CityCard(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        container = CityColors.SaffronSoft,
        borderColor = CityColors.Saffron.copy(alpha = 0.55f),
    ) {
        Row(Modifier.padding(16.dp)) {
            Box(Modifier.size(40.dp).background(CityColors.Saffron, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Warning, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SoftBadge(n.tag.uppercase(), CityColors.Saffron, Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(n.ago, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(n.source, style = MaterialTheme.typography.labelLarge, color = CityColors.Ink)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.CheckCircle, "Verified authority", tint = CityColors.Indigo, modifier = Modifier.size(14.dp))
                }
                Text(n.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(n.body, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
            }
        }
    }
}

@Composable
private fun StoryCard(n: NewsItem, endorsed: Boolean, saved: Boolean, onEndorse: () -> Unit, onSave: () -> Unit, onShare: () -> Unit) {
    CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (n.kind == NewsKind.CIVIC_WIN) {
                    SoftBadge(n.tag, CityColors.GreenSoft, CityColors.Green, icon = Icons.Filled.EmojiEvents)
                } else {
                    SoftBadge(n.tag, CityColors.IndigoSoft, CityColors.Indigo)
                }
                Spacer(Modifier.width(8.dp))
                Text(n.ago, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, modifier = Modifier.weight(1f))
                IconButton(onClick = onSave, modifier = Modifier.size(32.dp)) {
                    Icon(if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, if (saved) "Remove bookmark" else "Bookmark", tint = CityColors.Indigo)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(n.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(n.body, style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
            if (n.imageRes != null) {
                Spacer(Modifier.height(12.dp))
                Image(
                    painterResource(n.imageRes), null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp)),
                )
            }
            if (n.kind == NewsKind.CIVIC_WIN) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().background(CityColors.GreenSoft, RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Recycling, null, tint = CityColors.Green)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${com.rork.mycity.data.CurrentCity.city.municipalShort} ${com.rork.mycity.data.CurrentCity.city.areas.first().first} Zone Verified", style = MaterialTheme.typography.titleSmall)
                        Text("1.4 tonnes organic compost diverted monthly", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                    }
                    SoftBadge("Grade A+", CityColors.Green, Color.White)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onEndorse) {
                    AnimatedContent(endorsed, label = "endorse") { e ->
                        Icon(if (e) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, null, Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("${n.endorsements + if (endorsed) 1 else 0} Endorsed")
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, null, tint = CityColors.Muted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${n.comments}", style = MaterialTheme.typography.labelLarge, color = CityColors.Muted)
                }
                IconButton(onClick = onShare) { Icon(Icons.Filled.Share, "Share", tint = CityColors.Muted) }
            }
        }
    }
}

@Composable
private fun OfferCard(n: NewsItem, claimed: Boolean, onClaim: () -> Unit) {
    val context = LocalContext.current
    CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SoftBadge(n.tag, CityColors.SaffronSoft, CityColors.SaffronText, icon = Icons.Filled.LocalOffer)
                Spacer(Modifier.weight(1f))
                Text(n.ago, style = MaterialTheme.typography.labelMedium, color = CityColors.Red)
            }
            Spacer(Modifier.height(10.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    Text(n.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(n.body, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                }
                if (n.imageRes != null) {
                    Spacer(Modifier.width(12.dp))
                    Image(
                        painterResource(n.imageRes), null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(84.dp).clip(RoundedCornerShape(14.dp)),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().background(CityColors.IndigoSoft, RoundedCornerShape(12.dp)).padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Campaign, null, tint = CityColors.Indigo, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (claimed) n.offerCode ?: "" else "••••••••",
                    style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace,
                    color = CityColors.IndigoDeep, modifier = Modifier.weight(1f),
                )
                if (claimed) {
                    TextButton(onClick = { copy(context, n.offerCode ?: "") }) {
                        Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy")
                    }
                } else {
                    Button(onClick = onClaim) { Text("Claim Offer") }
                }
            }
        }
    }
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("MyCity offer", text))
    Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
}
