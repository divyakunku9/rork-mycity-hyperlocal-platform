package com.sheharsaathi.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sheharsaathi.app.data.CityViewModel
import com.sheharsaathi.app.data.Leg
import com.sheharsaathi.app.data.LegMode
import com.sheharsaathi.app.data.Transit
import com.sheharsaathi.app.ui.components.CityCard
import com.sheharsaathi.app.ui.components.LegTile
import com.sheharsaathi.app.ui.components.LineBadge
import com.sheharsaathi.app.ui.components.RouteMap
import com.sheharsaathi.app.ui.components.TagLabel
import com.sheharsaathi.app.ui.components.dial
import com.sheharsaathi.app.ui.components.legColor
import com.sheharsaathi.app.ui.components.shareText
import com.sheharsaathi.app.ui.theme.CityColors
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.max

/** Simulated seconds that pass per real second while in GO mode. */
private const val SIM_SPEED = 15

@Composable
fun GoModeScreen(
    vm: CityViewModel,
    onEnd: () -> Unit,
    onReroute: () -> Unit,
    onReport: () -> Unit,
    onOpenBoard: () -> Unit,
) {
    val route = vm.activeRoute
    if (route == null) {
        LaunchedEffect(Unit) { onEnd() }
        return
    }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val legs = route.legs
    val durations = legs.map { (it.minutes + it.waitMin) * 60 }
    val starts = durations.runningFold(0) { acc, d -> acc + d }
    val total = starts.last()
    var elapsed by rememberSaveable(route.id) { mutableIntStateOf(0) }
    var wakeAlarm by rememberSaveable { mutableStateOf(true) }
    val destination = vm.tripTo?.name ?: legs.last().to

    LaunchedEffect(route.id) {
        while (elapsed < total) {
            delay(1000)
            val before = legs.indices.firstOrNull { starts[it + 1] > elapsed }
            elapsed = (elapsed + SIM_SPEED).coerceAtMost(total)
            val after = legs.indices.firstOrNull { starts[it + 1] > elapsed }
            if (before != after) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val arrived = elapsed >= total
    val current = legs.indices.firstOrNull { starts[it + 1] > elapsed } ?: legs.lastIndex
    val remainingMin = ceil((total - elapsed) / 60.0).toInt()
    val progress by animateFloatAsState(elapsed / total.toFloat().coerceAtLeast(1f), tween(900), label = "progress")
    val boardingIdx = legs.indices.firstOrNull { i -> i >= current && (legs[i].mode == LegMode.METRO || legs[i].mode == LegMode.BUS) }

    Column(Modifier.fillMaxSize().background(CityColors.Canvas)) {
        // Header
        Column(Modifier.background(CityColors.SurfaceHigh).statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LivePill(arrived)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (arrived) "Trip complete" else "Step ${current + 1} of ${legs.size}",
                    style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onEnd,
                    colors = ButtonDefaults.textButtonColors(containerColor = CityColors.RedSoft, contentColor = CityColors.Red),
                ) { Text(if (arrived) "Close" else "End trip") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                AnimatedContent(remainingMin, label = "eta") { m ->
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = CityColors.Indigo)) {
                                append(if (arrived) "Arrived" else "$m min")
                            }
                            if (!arrived) withStyle(SpanStyle(fontSize = 15.sp, color = CityColors.Muted)) { append(" left") }
                        },
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Arrive ${Transit.clockIn(remainingMin)}",
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (!arrived) {
                    Row(
                        Modifier.clip(CircleShape).background(CityColors.GreenSoft).padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Bolt, null, tint = CityColors.Green, modifier = Modifier.size(14.dp))
                        Text("2 min ahead", style = MaterialTheme.typography.labelMedium, color = CityColors.Green)
                    }
                }
            }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(170.dp)) {
                RouteMap(legs, Modifier.fillMaxSize(), progress = progress)
                Row(
                    Modifier.padding(10.dp).clip(CircleShape).background(CityColors.SurfaceHigh).padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(legColor(legs[current])))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (arrived) "At $destination" else "${legs[current].title} • ${if (legs[current].mode == LegMode.WALK) "Walking" else "In transit"}",
                        style = MaterialTheme.typography.labelMedium, maxLines = 1,
                    )
                }
            }

            if (arrived) {
                ArrivedCard(destination, route.priceLabel, onEnd)
            } else if (boardingIdx != null) {
                val leg = legs[boardingIdx]
                val untilBoard = max(0, starts[boardingIdx] + leg.waitMin * 60 - elapsed)
                BoardingCard(leg, onBoard = elapsed >= starts[boardingIdx] + leg.waitMin * 60, untilBoardMin = ceil(untilBoard / 60.0).toInt())
            } else {
                CurrentLegCard(legs[current], ceil((starts[current + 1] - elapsed) / 60.0).toInt())
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CityCard(Modifier.weight(1f).fillMaxHeight()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(CityColors.IndigoSoft), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.NotificationsActive, null, tint = CityColors.Indigo, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Wake alarm", style = MaterialTheme.typography.titleSmall)
                            Text("1 stop prior", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                        }
                        Switch(
                            checked = wakeAlarm,
                            onCheckedChange = {
                                wakeAlarm = it
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(context, if (it) "We'll buzz you one stop before" else "Wake alarm off", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = CityColors.Indigo),
                        )
                    }
                }
                CityCard(Modifier.weight(1f).fillMaxHeight(), onClick = {
                    shareText(context, "I'm on my way to $destination via Shehar Saathi — arriving around ${Transit.clockIn(remainingMin)} ($remainingMin min).")
                }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(CityColors.GreenSoft), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Share, null, tint = CityColors.Green, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Share ETA", style = MaterialTheme.typography.titleSmall)
                            Text("WhatsApp & SMS", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                        }
                    }
                }
            }

            StepsCard(legs, starts, elapsed, current, arrived, destination, route.distanceKm)

            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp), color = CityColors.IndigoSoft,
                border = BorderStroke(1.dp, CityColors.IndigoMist),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Radar, null, tint = CityColors.Indigo)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Disruption radar", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.size(7.dp).clip(CircleShape).background(CityColors.Green))
                        }
                        Text("All systems normal on your route", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                    }
                    OutlinedButton(onClick = onOpenBoard, colors = ButtonDefaults.outlinedButtonColors(containerColor = CityColors.Surface)) {
                        Text("View radar")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Bottom actions
        Row(
            Modifier.fillMaxWidth().background(CityColors.SurfaceHigh).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(onClick = onReport, modifier = Modifier.weight(1f).height(50.dp)) {
                Icon(Icons.Filled.Warning, null, tint = CityColors.Saffron, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Report", maxLines = 1)
            }
            Button(onClick = onReroute, modifier = Modifier.weight(1f).height(50.dp)) {
                Icon(Icons.AutoMirrored.Filled.AltRoute, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Re-route", maxLines = 1)
            }
            Button(
                onClick = { dial(context, "112") },
                modifier = Modifier.height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CityColors.RedSoft, contentColor = CityColors.Red),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) { Text("SOS", fontWeight = FontWeight.ExtraBold) }
        }
    }
}

@Composable
private fun LivePill(arrived: Boolean) {
    val pulse = rememberInfiniteTransition(label = "live")
    val a by pulse.animateFloat(1f, 0.25f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Row(
        Modifier.clip(CircleShape).background(if (arrived) CityColors.GreenSoft else CityColors.SaffronSoft).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).alpha(if (arrived) 1f else a).clip(CircleShape).background(if (arrived) CityColors.Green else CityColors.Saffron))
        Spacer(Modifier.width(6.dp))
        Text(
            if (arrived) "ARRIVED" else "LIVE GO MODE",
            style = MaterialTheme.typography.labelSmall,
            color = if (arrived) CityColors.Green else CityColors.SaffronText,
        )
    }
}

@Composable
private fun BoardingCard(leg: Leg, onBoard: Boolean, untilBoardMin: Int) {
    val metro = leg.mode == LegMode.METRO
    val line = if (metro) "${leg.lineLabel} Line" else "${com.sheharsaathi.app.data.CurrentCity.city.busOperator} ${leg.lineLabel}"
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp), color = CityColors.Indigo, shadowElevation = 6.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(CityColors.SurfaceHigh), contentAlignment = Alignment.Center) {
                    LegTile(leg, size = 40)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LineBadge(line.uppercase(), Color(leg.lineColor))
                        Spacer(Modifier.width(6.dp))
                        Text(if (onBoard) "On board" else "Next boarding", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (onBoard) "Ride to ${leg.to}" else "Board $line to ${leg.towards ?: leg.to}",
                        style = MaterialTheme.typography.titleLarge, color = Color.White,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (metro) "NEXT TRAIN" else "BUS IN", style = MaterialTheme.typography.labelSmall, color = Color(0xFF7CF0C2))
                    AnimatedContent(if (onBoard) -1 else untilBoardMin, label = "next") { m ->
                        Text(
                            when { m < 0 -> "Aboard"; m == 0 -> "Now"; else -> "$m min" },
                            style = MaterialTheme.typography.headlineSmall, color = Color(0xFF7CF0C2),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (metro) "${leg.platform} • Every 4 min • ${leg.from} Stn" else "${leg.from} • Timetable estimate • ${leg.detail}",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.1f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)).padding(12.dp),
            ) {
                Text(if (metro) "BEST CARRIAGE STRATEGY" else "RIDE TIP", style = MaterialTheme.typography.labelSmall, color = Color(0xFF7CF0C2))
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (metro) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            (1..6).forEach { c ->
                                Box(
                                    Modifier.size(width = 14.dp, height = 18.dp).clip(RoundedCornerShape(3.dp))
                                        .background(if (c <= 2) Color(0xFF7CF0C2) else Color.White.copy(alpha = 0.25f)),
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        if (metro) "Board in the front car (carriage 1–2) for instant stairs at ${leg.to} Gate B."
                        else "Board from the front door. Ladies' seats on the left. Alight at ${leg.to}.",
                        style = MaterialTheme.typography.bodySmall, color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentLegCard(leg: Leg, minutesLeft: Int) {
    CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), borderColor = CityColors.Indigo.copy(alpha = 0.4f)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            LegTile(leg, size = 48)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                TagLabel("Now", CityColors.Saffron)
                Text(leg.title, style = MaterialTheme.typography.titleLarge)
                Text(leg.detail, style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$minutesLeft min", style = MaterialTheme.typography.headlineSmall, color = CityColors.Indigo)
                Text("remaining", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
            }
        }
    }
}

@Composable
private fun ArrivedCard(destination: String, price: String, onDone: () -> Unit) {
    CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), borderColor = CityColors.Green.copy(alpha = 0.4f), container = CityColors.GreenSoft) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, tint = CityColors.Green, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("You've arrived", style = MaterialTheme.typography.titleLarge)
                    Text(destination, style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
                }
                Text(price, style = MaterialTheme.typography.titleLarge, color = CityColors.Green)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = CityColors.Green)) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun StepsCard(
    legs: List<Leg>, starts: List<Int>, elapsed: Int, current: Int, arrived: Boolean, destination: String, km: Double,
) {
    CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Journey steps", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text("Total ${"%.1f".format(km)} km", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
            }
            Spacer(Modifier.height(12.dp))
            legs.forEachIndexed { i, leg ->
                val done = arrived || i < current
                val isCurrent = !arrived && i == current
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Column(Modifier.width(28.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(22.dp).clip(CircleShape).background(
                                when { done -> CityColors.IndigoSoft; isCurrent -> CityColors.Saffron; else -> CityColors.SurfaceHigh }
                            ).border(2.dp, if (done) CityColors.IndigoMist else if (isCurrent) CityColors.SaffronSoft else legColor(leg), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (done) Icon(Icons.Filled.Check, null, tint = CityColors.Indigo, modifier = Modifier.size(14.dp))
                            else if (isCurrent) Box(Modifier.size(8.dp).clip(CircleShape).background(CityColors.SurfaceHigh))
                        }
                        Box(Modifier.width(2.dp).weight(1f).background(CityColors.Outline))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f).padding(bottom = 14.dp)) {
                        val content: @Composable () -> Unit = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    leg.title + if (leg.mode == LegMode.METRO) " • ${leg.lineLabel} Line" else "",
                                    style = MaterialTheme.typography.titleSmall,
                                    textDecoration = if (done) TextDecoration.LineThrough else null,
                                    color = if (done) CityColors.Muted else CityColors.Ink,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    when {
                                        isCurrent -> "${ceil((starts[i + 1] - elapsed) / 60.0).toInt()} min left"
                                        leg.mode == LegMode.METRO -> "${leg.stops.size - 1} stops • ${leg.minutes} min"
                                        else -> Transit.clockIn((starts[i] - elapsed) / 60)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isCurrent) CityColors.SaffronText else CityColors.Indigo,
                                )
                            }
                            Text(
                                if (done) "Completed in ${leg.minutes + leg.waitMin} min" else "${leg.from} → ${leg.to}",
                                style = MaterialTheme.typography.bodySmall, color = CityColors.Muted,
                            )
                            if (leg.mode == LegMode.METRO && !done && leg.stops.size >= 2) {
                                StationTicker(leg, if (isCurrent) ((elapsed - starts[i] - leg.waitMin * 60).coerceAtLeast(0) / (leg.minutes * 60f)) else 0f)
                            }
                        }
                        if (isCurrent) {
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CityColors.SaffronSoft)
                                    .border(1.dp, CityColors.Saffron.copy(alpha = 0.35f), RoundedCornerShape(12.dp)).padding(10.dp),
                            ) {
                                TagLabel("Current step", CityColors.SaffronText)
                                content()
                            }
                        } else content()
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Flag, null, tint = if (arrived) CityColors.Green else CityColors.Saffron, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Arrive • $destination", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Transit.clockIn((starts.last() - elapsed) / 60), style = MaterialTheme.typography.labelMedium, color = CityColors.Indigo)
            }
        }
    }
}

@Composable
private fun StationTicker(leg: Leg, progress: Float) {
    val first = leg.stops.first()
    val mid = leg.stops[leg.stops.size / 2]
    val last = leg.stops.last()
    val color = Color(leg.lineColor)
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(10.dp)).background(CityColors.IndigoSoft).padding(10.dp),
    ) {
        TagLabel("Upcoming stations", CityColors.Muted)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(12.dp), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(CityColors.IndigoMist))
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp).background(color))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(0f, 0.5f, 1f).forEach { p ->
                    Box(Modifier.size(12.dp).clip(CircleShape).background(if (progress >= p) color else CityColors.SurfaceHigh).border(2.dp, color, CircleShape))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(first, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
            if (leg.stops.size > 2) Text(mid, style = MaterialTheme.typography.labelMedium, color = CityColors.Muted, maxLines = 1)
            Text(last, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
        }
    }
}
