package com.sheharsaathi.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sheharsaathi.app.data.CityViewModel
import com.sheharsaathi.app.data.IssueCategory
import com.sheharsaathi.app.data.SampleData
import com.sheharsaathi.app.data.Urgency
import com.sheharsaathi.app.ui.theme.CityColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ReportSpot(val label: String, val lat: Double, val lng: Double)

/** The user's own location first, then the four nearest neighbourhoods in the current city. */
private fun reportSpots(vm: CityViewModel): List<ReportSpot> {
    val me = vm.userPlace
    val mine = ReportSpot(if (me.kind == com.sheharsaathi.app.data.PlaceKind.CURRENT) "Near ${me.name}" else me.name, me.lat, me.lng)
    val nearby = com.sheharsaathi.app.data.Transit.areas
        .sortedBy { com.sheharsaathi.app.data.Transit.distanceKm(me.lat, me.lng, it.lat, it.lng) }
        .filter { it.name != me.name }
        .take(4)
        .map { ReportSpot(it.name + ", " + it.area, it.lat, it.lng) }
    return listOf(mine) + nearby
}

fun issueIcon(c: IssueCategory): ImageVector = when (c) {
    IssueCategory.POTHOLE -> Icons.Filled.Warning
    IssueCategory.STREETLIGHT -> Icons.Filled.Lightbulb
    IssueCategory.GARBAGE -> Icons.Filled.Delete
    IssueCategory.WATER -> Icons.Filled.WaterDrop
}

private fun authorityIcon(handle: String): ImageVector = com.sheharsaathi.app.data.CurrentCity.city.let { c ->
    when (handle) {
        c.traffic.handle -> Icons.Filled.LocalPolice
        c.power.handle -> Icons.Filled.ElectricBolt
        c.water.handle -> Icons.Filled.WaterDrop
        else -> Icons.Filled.AccountBalance
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun ReportScreen(vm: CityViewModel, onSubmitted: (String) -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryName by rememberSaveable { mutableStateOf(IssueCategory.POTHOLE.name) }
    val category = IssueCategory.valueOf(categoryName)
    val extraTags = remember { mutableStateListOf<String>() }
    var description by rememberSaveable { mutableStateOf("") }
    var urgencyName by rememberSaveable { mutableStateOf(Urgency.MEDIUM.name) }
    val urgency = Urgency.valueOf(urgencyName)
    val locations = remember(vm.userPlace) { reportSpots(vm) }
    var locationIndex by rememberSaveable(vm.userPlace.id) { mutableStateOf(0) }
    val spot = locations.getOrElse(locationIndex) { locations.first() }
    val location = spot.label
    var locationMenu by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    val primaryAuthority = SampleData.defaultAuthorityFor(category)

    var photoStamp by rememberSaveable { mutableStateOf<String?>(null) }
    var photoSheet by remember { mutableStateOf(false) }
    val locateMe = com.sheharsaathi.app.ui.components.rememberLocateMe(vm)
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val copied = com.sheharsaathi.app.ui.components.copyPickedPhoto(context, uri, "report")
            photoUri = (copied ?: uri).toString()
            photoStamp = "From gallery • pinned at ${spot.label.substringBefore(",")}"
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) scope.launch {
            val saved = com.sheharsaathi.app.ui.components.saveCameraPhoto(context, bmp, "report", spot.lat, spot.lng)
            if (saved != null) {
                photoUri = saved.toString()
                val time = java.text.SimpleDateFormat("h:mm a", java.util.Locale.ENGLISH).format(java.util.Date())
                photoStamp = "Live photo • GPS %.4f, %.4f • %s".format(spot.lat, spot.lng, time)
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            }
        }
    }
    val locationForCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) locateMe()
        else Toast.makeText(context, "Photo will be tagged with your chosen spot instead of GPS", Toast.LENGTH_SHORT).show()
        runCatching { camera.launch(null) }
    }
    val openCamera = {
        photoSheet = false
        if (com.sheharsaathi.app.data.LocationService.hasPermission(context)) {
            if (vm.locationSource == com.sheharsaathi.app.data.LocationSource.DEFAULT) locateMe()
            runCatching { camera.launch(null) }.onFailure {
                Toast.makeText(context, "No camera app available", Toast.LENGTH_SHORT).show()
            }
        } else {
            locationForCamera.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        Unit
    }
    val openGallery = {
        photoSheet = false
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!text.isNullOrBlank()) description = (if (description.isBlank()) text else "$description $text").take(500)
    }

    Column(
        Modifier.fillMaxSize().background(CityColors.Canvas).verticalScroll(rememberScrollState())
            .statusBarsPadding().padding(bottom = 24.dp)
    ) {
        Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Report Civic Issue", style = MaterialTheme.typography.headlineSmall, color = CityColors.IndigoDeep)
                Text("Help make ${vm.city.name} better", style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
            }
            IconButton(onClick = { showInfo = true }) { Icon(Icons.Filled.Info, contentDescription = "How reporting works", tint = CityColors.IndigoDeep) }
        }

        // Photo
        Box(
            Modifier.padding(16.dp).fillMaxWidth().height(230.dp).clip(RoundedCornerShape(20.dp))
                .background(CityColors.IndigoSoft)
        ) {
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri, contentDescription = "Issue photo", contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Column(
                    Modifier.padding(12.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4ADE80), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Photo attached", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                    photoStamp?.let {
                        Text(it, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Surface(
                    onClick = { photoSheet = true },
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    shape = CircleShape, color = Color.Black.copy(alpha = 0.6f),
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Edit, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Change", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                Surface(
                    onClick = { photoSheet = true },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxSize()
                        .border(BorderStroke(1.5.dp, CityColors.Indigo.copy(alpha = 0.35f)), RoundedCornerShape(20.dp)),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box(Modifier.size(60.dp).background(CityColors.SurfaceHigh, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.AddAPhoto, null, tint = CityColors.Indigo, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Snap a live photo of the issue", style = MaterialTheme.typography.titleMedium, color = CityColors.IndigoDeep)
                        Text("A GPS-stamped photo helps crews find the exact spot", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                    }
                }
            }
            // Location chip
            Box(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Surface(onClick = { locationMenu = true }, shape = RoundedCornerShape(12.dp), color = Color.Black.copy(alpha = 0.62f)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(location, color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
                DropdownMenu(expanded = locationMenu, onDismissRequest = { locationMenu = false }) {
                    locations.forEachIndexed { i, loc ->
                        DropdownMenuItem(
                            text = { Text(loc.label) },
                            leadingIcon = { Icon(if (i == 0) Icons.Filled.MyLocation else Icons.Filled.LocationOn, null) },
                            trailingIcon = if (i == locationIndex) { { Icon(Icons.Filled.Check, null) } } else null,
                            onClick = { locationIndex = i; locationMenu = false },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Change my location…") },
                        leadingIcon = { Icon(Icons.Filled.EditLocationAlt, null) },
                        onClick = { locationMenu = false; vm.showLocationPicker = true },
                    )
                }
            }
        }

        // Tagged-spot preview
        val previewMap = com.sheharsaathi.app.ui.components.rememberMapController()
        androidx.compose.runtime.LaunchedEffect(spot.lat, spot.lng) { previewMap.flyTo(spot.lat, spot.lng, 16.5) }
        Box(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).fillMaxWidth().height(120.dp)
                .clip(RoundedCornerShape(18.dp)).border(1.dp, CityColors.GlassBorder, RoundedCornerShape(18.dp))
        ) {
            com.sheharsaathi.app.ui.components.CityMap(
                pins = emptyList(), controller = previewMap, userLat = vm.userPlace.lat, userLng = vm.userPlace.lng,
                modifier = Modifier.fillMaxSize(), dropped = spot.lat to spot.lng,
            )
            Row(
                Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(10.dp))
                    .background(CityColors.Glass).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.MyLocation, null, tint = CityColors.GreenText, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tagged spot • %.4f, %.4f".format(spot.lat, spot.lng), color = CityColors.Ink, style = MaterialTheme.typography.labelMedium)
            }
        }

        if (photoSheet) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { photoSheet = false }, containerColor = CityColors.Surface,
            ) {
                Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Add a photo", style = MaterialTheme.typography.titleLarge, color = CityColors.Ink)
                    Text("Live photos are stamped with your GPS position so the authority can find the spot.", style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted)
                    com.sheharsaathi.app.ui.screens.PrimaryButton("Take live photo", enabled = true, onClick = openCamera)
                    androidx.compose.material3.OutlinedButton(
                        onClick = openGallery, modifier = Modifier.fillMaxWidth().height(52.dp),
                        border = BorderStroke(1.dp, CityColors.Outline),
                    ) { Text("Choose from gallery", color = CityColors.Ink) }
                }
            }
        }

        // Category
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(IssueCategory.entries) { c ->
                FilterChip(
                    selected = category == c,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        categoryName = c.name
                        extraTags.remove(SampleData.defaultAuthorityFor(c))
                    },
                    label = { Text(c.label, modifier = Modifier.padding(vertical = 8.dp)) },
                    leadingIcon = { Icon(issueIcon(c), null, Modifier.size(18.dp)) },
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

        // Authorities
        SectionLabel("Auto-routed to")
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val ordered = SampleData.authorities.sortedBy { if (it.handle == primaryAuthority) 0 else 1 }
            items(ordered, key = { it.handle }) { a ->
                val isPrimary = a.handle == primaryAuthority
                val tagged = isPrimary || a.handle in extraTags
                val border by animateColorAsState(if (tagged) CityColors.Indigo else CityColors.Outline, label = "authBorder")
                Surface(
                    onClick = {
                        if (!isPrimary) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (a.handle in extraTags) extraTags.remove(a.handle) else extraTags.add(a.handle)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (tagged) CityColors.IndigoSoft.copy(alpha = 0.5f) else CityColors.Surface,
                    border = BorderStroke(1.dp, border),
                    modifier = Modifier.width(250.dp),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).background(CityColors.SurfaceHigh, CircleShape).border(1.dp, CityColors.Outline, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(authorityIcon(a.handle), null, tint = CityColors.IndigoDeep)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.handle, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                            when {
                                isPrimary -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = CityColors.Green, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Verified • Auto-routed", style = MaterialTheme.typography.labelMedium, color = CityColors.Green)
                                }
                                tagged -> Text("Tagged", style = MaterialTheme.typography.labelMedium, color = CityColors.Indigo)
                                else -> Text("+ Tag", style = MaterialTheme.typography.labelMedium, color = CityColors.Indigo)
                            }
                            Text(a.name, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 2)
                        }
                    }
                }
            }
        }

        // Description
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Describe the issue", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${description.length}/500", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
        }
        OutlinedTextField(
            value = description,
            onValueChange = { description = it.take(500); if (showError) showError = it.trim().length < 10 },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("e.g. Large crater near Cyber Towers junction causing bike skids.") },
            minLines = 2,
            maxLines = 5,
            isError = showError,
            supportingText = if (showError) { { Text("Add at least a short sentence so crews know what to fix") } } else null,
            trailingIcon = {
                IconButton(onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe the issue")
                    }
                    try { speech.launch(intent) } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, "Voice input isn't available on this device", Toast.LENGTH_SHORT).show()
                    }
                }) { Icon(Icons.Filled.Mic, contentDescription = "Dictate description") }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = CityColors.Surface, focusedContainerColor = CityColors.SurfaceHigh,
                unfocusedBorderColor = CityColors.Outline,
            ),
        )

        // Urgency
        SectionLabel("Select urgency")
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Urgency.entries.forEach { u ->
                val selected = u == urgency
                val accent = when (u) {
                    Urgency.LOW -> CityColors.Green
                    Urgency.MEDIUM -> CityColors.Saffron
                    Urgency.CRITICAL -> CityColors.Red
                }
                val bg by animateColorAsState(if (selected) accent.copy(alpha = 0.16f) else CityColors.Surface, label = "urgBg")
                val bd by animateColorAsState(if (selected) accent else CityColors.Outline, label = "urgBd")
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        urgencyName = u.name
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = bg,
                    border = BorderStroke(if (selected) 1.5.dp else 1.dp, bd),
                    modifier = Modifier.weight(1f).height(118.dp),
                ) {
                    Column(
                        Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            when (u) { Urgency.LOW -> Icons.Filled.BarChart; Urgency.MEDIUM -> Icons.Filled.PriorityHigh; Urgency.CRITICAL -> Icons.Filled.Report },
                            null, tint = accent, modifier = Modifier.size(30.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(u.label, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 2)
                        Text(u.sub, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Text(
            "Expected response: ${urgency.sla}",
            style = MaterialTheme.typography.bodySmall, color = CityColors.Muted,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        // Dispatch note
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().background(CityColors.IndigoSoft, RoundedCornerShape(16.dp)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.ErrorOutline, null, tint = CityColors.Indigo)
            Spacer(Modifier.width(10.dp))
            Text(
                "Your report will be visible to nearby residents and routed to ${primaryAuthority.removePrefix("@")}.",
                style = MaterialTheme.typography.bodySmall, color = CityColors.IndigoDeep,
            )
        }

        Button(
            onClick = {
                if (description.trim().length < 10) {
                    showError = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    return@Button
                }
                submitting = true
                scope.launch {
                    delay(700)
                    val id = vm.submitReport(
                        category = category,
                        description = description,
                        authorities = listOf(primaryAuthority) + extraTags,
                        urgency = urgency,
                        imageUri = photoUri,
                        location = location,
                        lat = if (locationIndex == 0) null else spot.lat,
                        lng = if (locationIndex == 0) null else spot.lng,
                    )
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    photoUri = null; photoStamp = null; description = ""; extraTags.clear(); urgencyName = Urgency.MEDIUM.name
                    submitting = false
                    onSubmitted(id)
                }
            },
            enabled = !submitting,
            modifier = Modifier.padding(16.dp).fillMaxWidth().height(56.dp),
            shape = CircleShape,
        ) {
            if (submitting) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            else Text("Submit Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("How reporting works") },
            text = {
                Text("1. Snap a photo and pin the spot.\n2. We route it to the right authority automatically.\n3. Neighbours confirm it to raise priority.\n4. Track its status any time under My filed issues in the Civic Hub.")
            },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Got it") } },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
    )
}
