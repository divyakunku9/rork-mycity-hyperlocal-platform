package com.rork.mycity.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rork.mycity.data.CityViewModel
import com.rork.mycity.ui.components.CityCard
import com.rork.mycity.ui.components.SoftBadge
import com.rork.mycity.ui.components.TagLabel
import com.rork.mycity.ui.components.compactCount
import com.rork.mycity.ui.components.openDirections
import com.rork.mycity.ui.components.rupees
import com.rork.mycity.ui.components.shareText
import com.rork.mycity.ui.theme.CityColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun EventDetailScreen(vm: CityViewModel, eventId: String, onBack: () -> Unit, onPlanTrip: () -> Unit) {
    val event = vm.event(eventId) ?: return
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var qty by rememberSaveable { mutableIntStateOf(1) }
    var confirm by remember { mutableStateOf(false) }
    val booked = vm.bookings[event.id] ?: 0
    val saved = vm.savedEvents[event.id] == true
    val isFree = event.priceRupees == 0

    val base = event.priceRupees * qty
    val cess = (base * 0.01).roundToInt()
    val total = base + cess

    Scaffold(
        containerColor = CityColors.Canvas,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(color = CityColors.Surface, shadowElevation = 12.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedIconButton(onClick = { if (qty > 1) qty-- }, enabled = qty > 1) {
                        Icon(Icons.Filled.Remove, contentDescription = "Fewer tickets")
                    }
                    Text("$qty", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 10.dp))
                    OutlinedIconButton(onClick = { if (qty < 8) qty++ }, enabled = qty < 8) {
                        Icon(Icons.Filled.Add, contentDescription = "More tickets")
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = { confirm = true }, modifier = Modifier.weight(1f).height(52.dp)) {
                        Icon(Icons.Filled.ConfirmationNumber, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isFree) "RSVP Free" else "Book • ${rupees(total)}")
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = padding.calculateBottomPadding())
        ) {
            Box {
                com.rork.mycity.ui.components.EventImage(
                    event, description = event.title,
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                )
                Box(
                    Modifier.fillMaxWidth().height(110.dp)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)))
                )
                Row(Modifier.statusBarsPadding().padding(8.dp).fillMaxWidth()) {
                    FilledIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = CityColors.Surface, contentColor = CityColors.Ink),
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    Spacer(Modifier.weight(1f))
                    FilledIconButton(
                        onClick = { shareText(context, "${event.title} — ${event.dateLabel}, ${event.timeLabel} at ${event.venue}. Found on MyCity.") },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = CityColors.Surface, contentColor = CityColors.Ink),
                    ) { Icon(Icons.Filled.Share, contentDescription = "Share") }
                    FilledIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.toggleSaveEvent(event.id)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = CityColors.Surface, contentColor = CityColors.Indigo),
                    ) {
                        Icon(if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = if (saved) "Remove bookmark" else "Bookmark")
                    }
                }
            }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    val (bg, fg) = categoryColors(event.category)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SoftBadge(event.category.label, bg, fg, icon = categoryIcon(event.category))
                        if (event.fastFilling) SoftBadge("Fast filling", CityColors.SaffronSoft, CityColors.Saffron)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(event.title, style = MaterialTheme.typography.headlineMedium)
                }
                AnimatedVisibility(booked > 0, enter = fadeIn() + expandVertically()) {
                    CityCard(Modifier.fillMaxWidth(), container = CityColors.GreenSoft, borderColor = CityColors.Green.copy(alpha = 0.4f)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ConfirmationNumber, null, tint = CityColors.Green)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("You're going! $booked ${if (booked == 1) "pass" else "passes"} confirmed", style = MaterialTheme.typography.titleSmall, color = CityColors.Green)
                                Text("Booking ID MC-${event.id.take(4).uppercase()}-${1000 + booked * 37}", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                            }
                        }
                    }
                }
                CityCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoRow(Icons.Filled.CalendarMonth, event.dateLabel, event.timeLabel)
                        HorizontalDivider(color = CityColors.Outline)
                        InfoRow(Icons.Filled.Place, event.venue, "${event.area} • ${"%.1f".format(com.rork.mycity.data.Transit.distanceKm(vm.userPlace.lat, vm.userPlace.lng, event.lat, event.lng))} km away")
                        HorizontalDivider(color = CityColors.Outline)
                        InfoRow(Icons.Filled.Groups, "${compactCount(event.interested)} interested", "Neighbours from " + com.rork.mycity.data.CurrentCity.city.areas.take(3).joinToString(", ") { it.first })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {
                        vm.tripFrom = vm.userPlace
                        vm.planTo(com.rork.mycity.data.Place("ev-" + event.id, event.venue, event.area, event.lat, event.lng, com.rork.mycity.data.PlaceKind.VENUE))
                        onPlanTrip()
                    }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Directions, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Get me there")
                    }
                    OutlinedButton(
                        onClick = {
                            com.rork.mycity.ui.components.openUrl(
                                context, "https://m.uber.com/ul/?action=setPickup&dropoff[latitude]=${event.lat}&dropoff[longitude]=${event.lng}"
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Book a ride") }
                }
                Column {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(event.description, style = MaterialTheme.typography.bodyLarge, color = CityColors.Muted)
                }
                if (!isFree) {
                    CityCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TagLabel("Transparent pricing", CityColors.Indigo)
                            PriceLine("Entry ticket × $qty", rupees(base))
                            PriceLine("Entertainment cess (1%)", rupees(cess))
                            PriceLine("Platform fee", "Waived", valueColor = CityColors.Green)
                            HorizontalDivider(color = CityColors.Outline)
                            Row {
                                Text("Total", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Text(rupees(total), style = MaterialTheme.typography.titleLarge, color = CityColors.Indigo)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.VerifiedUser, null, tint = CityColors.Green, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Verified venue & noise permits", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            icon = { Icon(Icons.Filled.ConfirmationNumber, null, tint = CityColors.Indigo) },
            title = { Text(if (isFree) "Confirm RSVP" else "Confirm booking") },
            text = {
                Text(
                    if (isFree) "Reserve $qty free ${if (qty == 1) "spot" else "spots"} for ${event.title}?"
                    else "$qty ${if (qty == 1) "ticket" else "tickets"} for ${event.title}. You'll pay ${rupees(total)} at the venue gate with your MyCity pass."
                )
            },
            confirmButton = {
                Button(onClick = {
                    confirm = false
                    vm.book(event.id, qty)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch { snackbar.showSnackbar("Booked! Your pass is saved on this event.") }
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, sub: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).background(CityColors.IndigoSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = CityColors.Indigo, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
        }
    }
}

@Composable
private fun PriceLine(label: String, value: String, valueColor: Color = CityColors.Ink) {
    Row {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

