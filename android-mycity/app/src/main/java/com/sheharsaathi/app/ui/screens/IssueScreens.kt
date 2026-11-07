package com.sheharsaathi.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sheharsaathi.app.data.CityViewModel
import com.sheharsaathi.app.data.CivicIssue
import com.sheharsaathi.app.data.IssueStatus
import com.sheharsaathi.app.data.Urgency
import com.sheharsaathi.app.ui.components.CityCard
import com.sheharsaathi.app.ui.components.SoftBadge
import com.sheharsaathi.app.ui.components.StatusBadge
import com.sheharsaathi.app.ui.components.TagLabel
import com.sheharsaathi.app.ui.components.shareText
import com.sheharsaathi.app.ui.theme.CityColors

@Composable
private fun IssueThumb(issue: CivicIssue, modifier: Modifier) {
    when {
        issue.imageUri != null -> AsyncImage(issue.imageUri, null, contentScale = ContentScale.Crop, modifier = modifier)
        issue.imageRes != null -> Image(painterResource(issue.imageRes), null, contentScale = ContentScale.Crop, modifier = modifier)
        else -> Box(modifier.background(CityColors.IndigoSoft), contentAlignment = Alignment.Center) {
            Icon(issueIcon(issue.category), null, tint = CityColors.Indigo, modifier = Modifier.size(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(vm: CityViewModel, issueId: String, onBack: () -> Unit) {
    val issue = vm.issue(issueId) ?: return MissingItem("This report isn't available", "It may have been removed, or the list is still loading.", onBack)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val upvoted = vm.upvotedIssues[issue.id] == true
    val bump by animateFloatAsState(if (upvoted) 1f else 0.9f, label = "bump")

    Scaffold(
        containerColor = CityColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Civic report") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { shareText(context, "${issue.title} at ${issue.location} — ${issue.upvotes} residents confirmed on Shehar Saathi.") }) {
                        Icon(Icons.Filled.Share, "Share report")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CityColors.Canvas),
            )
        },
        bottomBar = {
            if (issue.status != IssueStatus.RESOLVED && !(vm.isLive && issue.isMine)) {
                Surface(color = CityColors.Surface, shadowElevation = 12.dp) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.toggleUpvote(issue.id)
                        },
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(54.dp).scale(bump.coerceAtLeast(0.96f)),
                        shape = CircleShape,
                        colors = if (upvoted) ButtonDefaults.buttonColors(containerColor = CityColors.GreenSoft, contentColor = CityColors.Green)
                        else ButtonDefaults.buttonColors(),
                    ) {
                        Icon(if (upvoted) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (upvoted) "You confirmed this • ${issue.upvotes}" else "I see this too • ${issue.upvotes}")
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IssueThumb(issue, Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)))
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(issue.status)
                    val (bg, fg) = when (issue.urgency) {
                        Urgency.CRITICAL -> CityColors.RedSoft to CityColors.Red
                        Urgency.MEDIUM -> CityColors.SaffronSoft to CityColors.SaffronText
                        Urgency.LOW -> CityColors.GreenSoft to CityColors.Green
                    }
                    SoftBadge(issue.urgency.label, bg, fg)
                    if (issue.isMine) SoftBadge("Your report", CityColors.IndigoSoft, CityColors.Indigo)
                }
                Spacer(Modifier.height(10.dp))
                Text(issue.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = CityColors.Muted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${issue.location} • ${issue.reportedAgo}", style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
                }
                Spacer(Modifier.height(10.dp))
                Text(issue.description, style = MaterialTheme.typography.bodyLarge)
            }

            CityCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    TagLabel("Status timeline", CityColors.Indigo)
                    Spacer(Modifier.height(12.dp))
                    val current = issue.status.ordinal
                    IssueStatus.entries.forEachIndexed { i, s ->
                        val done = i <= current
                        Row(verticalAlignment = Alignment.Top) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier.size(24.dp).background(if (done) CityColors.Indigo else CityColors.Outline, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (done) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                                if (i < IssueStatus.entries.lastIndex) {
                                    Box(Modifier.width(2.dp).height(26.dp).background(if (i < current) CityColors.Indigo else CityColors.Outline))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(s.label, style = MaterialTheme.typography.titleSmall, color = if (done) CityColors.Ink else CityColors.Muted)
                                if (i == current) {
                                    Text(
                                        if (s == IssueStatus.REPORTED) "Waiting for the authority to pick this up" else "Current stage",
                                        style = MaterialTheme.typography.bodySmall, color = CityColors.Muted,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            CityCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TagLabel("Routed to", CityColors.Indigo)
                    issue.authorities.forEach { handle ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.VerifiedUser, null, tint = CityColors.Green, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(handle, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, null, tint = CityColors.Muted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Target response: ${issue.urgency.sla}", style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
                    }
                    if (issue.authorityNote != null) {
                        Column(Modifier.fillMaxWidth().background(CityColors.IndigoSoft, RoundedCornerShape(12.dp)).padding(12.dp)) {
                            Text("Official response", style = MaterialTheme.typography.labelMedium, color = CityColors.Indigo)
                            Text(issue.authorityNote, style = MaterialTheme.typography.bodyMedium, color = CityColors.IndigoDeep)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CivicReportsScreen(vm: CityViewModel, startOnMine: Boolean, onBack: () -> Unit, onOpenIssue: (String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(if (startOnMine) 1 else 0) }
    val list = if (tab == 0) vm.issues.sortedByDescending { it.upvotes } else vm.myIssues

    Scaffold(
        containerColor = CityColors.Canvas,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Civic reports") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CityColors.Canvas),
                )
                PrimaryTabRow(selectedTabIndex = tab, containerColor = CityColors.Canvas) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("All in ${vm.city.name}") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("My reports (${vm.myIssues.size})") })
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (list.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No reports yet", style = MaterialTheme.typography.titleMedium)
                        Text("Use the Report tab to flag a pothole, streetlight or garbage issue.", style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
                    }
                }
            }
            items(list, key = { it.id }) { issue ->
                CityCard(Modifier.fillMaxWidth().animateItem(), onClick = { onOpenIssue(issue.id) }) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        IssueThumb(issue, Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(issue.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(issue.reportedAgo + " • " + issue.authorities.first(), style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(issue.status)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Filled.ThumbUp, null, tint = CityColors.Muted, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${issue.upvotes}", style = MaterialTheme.typography.labelMedium, color = CityColors.Muted)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CityColors.Muted)
                    }
                }
            }
        }
    }
}

/** Shown instead of a blank screen when a report or event can't be found. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingItem(title: String, body: String, onBack: () -> Unit) {
    Scaffold(
        containerColor = CityColors.Canvas,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CityColors.Canvas),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
        }
    }
}
