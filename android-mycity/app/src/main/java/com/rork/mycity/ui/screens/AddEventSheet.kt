package com.rork.mycity.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rork.mycity.data.CityViewModel
import com.rork.mycity.data.DayBucket
import com.rork.mycity.data.EventCategory
import com.rork.mycity.data.Place
import com.rork.mycity.ui.components.PlaceResultRow
import com.rork.mycity.ui.components.SearchingRow
import com.rork.mycity.ui.components.copyPickedPhoto
import com.rork.mycity.ui.components.rememberPlaceSearch
import com.rork.mycity.ui.components.saveCameraPhoto
import com.rork.mycity.ui.theme.CityColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Sheet form for hosting a community event. Calls [onCreated] with the new event's id and day bucket. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventSheet(vm: CityViewModel, onDismiss: () -> Unit, onCreated: (String, DayBucket) -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(EventCategory.MUSIC) }
    var dayMillis by remember { mutableStateOf<Long?>(null) }
    var hour by remember { mutableStateOf(18) }
    var minute by remember { mutableStateOf(0) }
    var timeSet by remember { mutableStateOf(false) }
    var venueQuery by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf<Place?>(null) }
    var free by remember { mutableStateOf(true) }
    var price by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<String?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) scope.launch { photo = saveCameraPhoto(context, bmp, "event")?.toString() }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { photo = copyPickedPhoto(context, uri, "event")?.toString() }
    }

    val priceValue = price.toIntOrNull()
    val valid = title.trim().length >= 4 && dayMillis != null && timeSet && venue != null && (free || (priceValue != null && priceValue > 0))

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = CityColors.Surface) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Host an event in ${vm.city.name}", style = MaterialTheme.typography.headlineSmall)

            // Header photo
            Box(
                Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp)).background(CityColors.SurfaceHigh)
                    .border(1.dp, CityColors.Outline, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (photo != null) {
                    AsyncImage(photo, "Event photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Row(
                    Modifier.align(if (photo != null) Alignment.BottomEnd else Alignment.Center).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (photo == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AddAPhoto, null, tint = CityColors.Muted, modifier = Modifier.size(30.dp))
                            Text("Add a header photo", style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                                PhotoChip(Icons.Filled.PhotoCamera, "Camera") { camera.launch(null) }
                                PhotoChip(Icons.Filled.Image, "Gallery") { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                            }
                        }
                    } else {
                        PhotoChip(Icons.Filled.PhotoCamera, "Retake") { camera.launch(null) }
                        PhotoChip(Icons.Filled.Image, "Change") { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    }
                }
            }

            OutlinedTextField(
                value = title, onValueChange = { title = it.take(60) }, label = { Text("Event title") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), colors = formColors(), modifier = Modifier.fillMaxWidth(),
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(0.dp)) {
                items(EventCategory.entries) { c ->
                    FilterChip(
                        selected = category == c, onClick = { category = c },
                        label = { Text(c.label) },
                        leadingIcon = { Icon(categoryIcon(c), null, Modifier.size(18.dp)) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CityColors.SurfaceHigh, selectedContainerColor = CityColors.Indigo,
                            selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White, iconColor = CityColors.Indigo,
                        ),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerField(
                    Icons.Filled.CalendarMonth,
                    dayMillis?.let { SimpleDateFormat("EEE, d MMM", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(it) } ?: "Pick date",
                    set = dayMillis != null, modifier = Modifier.weight(1f),
                ) { showDate = true }
                PickerField(
                    Icons.Filled.AccessTime, if (timeSet) timeLabel(hour, minute) else "Pick time",
                    set = timeSet, modifier = Modifier.weight(1f),
                ) { showTime = true }
            }

            // Venue search
            if (venue == null) {
                OutlinedTextField(
                    value = venueQuery, onValueChange = { venueQuery = it }, label = { Text("Venue") },
                    placeholder = { Text("Search a venue on the map", color = CityColors.Muted) },
                    leadingIcon = { Icon(Icons.Filled.Place, null) }, singleLine = true,
                    shape = RoundedCornerShape(14.dp), colors = formColors(), modifier = Modifier.fillMaxWidth(),
                )
                val (results, loading) = rememberPlaceSearch(venueQuery)
                Column(Modifier.clip(RoundedCornerShape(14.dp)).background(CityColors.SurfaceHigh)) {
                    if (loading) SearchingRow()
                    val list = if (venueQuery.trim().length < 3) listOf(vm.userPlace) + vm.city.landmarks.take(3) else results.take(5)
                    list.forEach { p ->
                        PlaceResultRow(p, Icons.Filled.Place, CityColors.Saffron, sub = p.area.ifBlank { vm.city.name }) { venue = p }
                    }
                }
            } else {
                val v = venue!!
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CityColors.IndigoSoft)
                        .clickable(onClickLabel = "Change venue") { venue = null }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = CityColors.IndigoDeep)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(v.name, style = MaterialTheme.typography.titleSmall, color = CityColors.Ink)
                        Text(v.area.ifBlank { vm.city.name }, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                    }
                    Text("Change", style = MaterialTheme.typography.labelLarge, color = CityColors.Indigo)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Free entry", style = MaterialTheme.typography.titleSmall)
                    Text("Turn off to set a ticket price", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                }
                Switch(free, { free = it }, colors = SwitchDefaults.colors(checkedTrackColor = CityColors.Saffron, checkedThumbColor = Color.White))
            }
            if (!free) {
                OutlinedTextField(
                    value = price, onValueChange = { v -> price = v.filter { it.isDigit() }.take(5) }, label = { Text("Ticket price") },
                    prefix = { Text("₹ ") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp), colors = formColors(), modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = about, onValueChange = { about = it.take(400) }, label = { Text("About (optional)") }, minLines = 3,
                shape = RoundedCornerShape(14.dp), colors = formColors(), modifier = Modifier.fillMaxWidth(),
            )

            PrimaryButton("Publish event", enabled = valid) {
                val ms = dayMillis ?: return@PrimaryButton
                val (label, bucket) = dayInfo(ms)
                val id = vm.addEvent(
                    title = title, category = category, dateLabel = label, timeLabel = timeLabel(hour, minute), bucket = bucket,
                    venue = venue!!, priceRupees = if (free) 0 else priceValue ?: 0, imageUri = photo, description = about,
                )
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                onCreated(id, bucket)
            }
        }
    }

    if (showDate) {
        val today = remember { utcToday() }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dayMillis ?: today,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= today
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = { TextButton(onClick = { dayMillis = state.selectedDateMillis; showDate = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
    if (showTime) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = { TextButton(onClick = { hour = state.hour; minute = state.minute; timeSet = true; showTime = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } },
            text = { TimePicker(state) },
        )
    }
}

@Composable
private fun formColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = CityColors.SurfaceHigh, focusedContainerColor = CityColors.SurfaceHigh,
    unfocusedBorderColor = Color.Transparent, focusedBorderColor = CityColors.Indigo, cursorColor = CityColors.Saffron,
)

@Composable
private fun PhotoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(CityColors.Glass).border(1.dp, CityColors.GlassBorder, CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable
private fun PickerField(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, set: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(CityColors.SurfaceHigh)
            .border(1.dp, if (set) CityColors.Indigo.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (set) CityColors.IndigoDeep else CityColors.Muted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = if (set) CityColors.Ink else CityColors.Muted)
    }
}

private fun timeLabel(h: Int, m: Int): String {
    val hh = if (h % 12 == 0) 12 else h % 12
    return "$hh:${m.toString().padStart(2, '0')} ${if (h < 12) "AM" else "PM"}"
}

private fun utcToday(): Long {
    val local = Calendar.getInstance()
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** Date picker millis (UTC midnight) → display label and list bucket. */
private fun dayInfo(utcMillis: Long): Pair<String, DayBucket> {
    val days = ((utcMillis - utcToday()) / 86_400_000L).toInt()
    val fmt = SimpleDateFormat("EEE, d MMM", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    val label = when (days) {
        0 -> "Today, " + SimpleDateFormat("d MMM", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(utcMillis)
        1 -> "Tomorrow, " + SimpleDateFormat("d MMM", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(utcMillis)
        else -> fmt.format(utcMillis)
    }
    val bucket = when (days) { 0 -> DayBucket.TODAY; 1 -> DayBucket.TOMORROW; else -> DayBucket.WEEKEND }
    return label to bucket
}
