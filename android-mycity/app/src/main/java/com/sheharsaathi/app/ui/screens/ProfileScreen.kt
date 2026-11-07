package com.sheharsaathi.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sheharsaathi.app.data.CityViewModel
import com.sheharsaathi.app.ui.components.CityCard
import com.sheharsaathi.app.ui.components.ProfileAvatar
import com.sheharsaathi.app.ui.components.copyPickedPhoto
import com.sheharsaathi.app.ui.components.saveCameraPhoto
import com.sheharsaathi.app.ui.theme.CityColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: CityViewModel, onBack: () -> Unit, onOpenEvent: (String) -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val p = vm.profile
    val city = vm.city

    var name by rememberSaveable { mutableStateOf(p.name) }
    var phone by rememberSaveable { mutableStateOf(p.phone) }
    var email by rememberSaveable { mutableStateOf(p.email) }
    var ward by rememberSaveable { mutableStateOf(p.ward) }
    var photoSheet by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    var savedFlash by remember { mutableStateOf(false) }
    val dirty = name.trim() != p.name || phone != p.phone || email.trim() != p.email || ward.trim() != p.ward
    val phoneOk = phone.isEmpty() || (phone.length == 10 && phone.first() in '6'..'9')
    val emailOk = email.isBlank() || Regex("^[^@\\s]+@[^@\\s]+\\.[a-zA-Z]{2,}$").matches(email.trim())

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) scope.launch {
            saveCameraPhoto(context, bmp, "avatar")?.let { vm.updateProfile(vm.profile.copy(photoUri = it.toString())) }
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            copyPickedPhoto(context, uri, "avatar")?.let { vm.updateProfile(vm.profile.copy(photoUri = it.toString())) }
        }
    }

    Column(
        Modifier.fillMaxSize().background(CityColors.Canvas).verticalScroll(rememberScrollState())
            .statusBarsPadding().imePadding().navigationBarsPadding().padding(bottom = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Profile", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        }

        // Hero
        Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                ProfileAvatar(p, 104, Modifier.clip(CircleShape).clickable(onClickLabel = "Change photo") { photoSheet = true })
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp).size(34.dp).clip(CircleShape)
                        .background(CityColors.Indigo).border(3.dp, CityColors.Canvas, CircleShape)
                        .clickable(onClickLabel = "Change photo") { photoSheet = true },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
            }
            Spacer(Modifier.height(12.dp))
            Text(p.name.ifBlank { "Shehar Saathi resident" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                listOf(p.phone.takeIf { it.isNotBlank() }?.let { "+91 $it" }, p.email.takeIf { it.isNotBlank() }).filterNotNull().joinToString("  •  "),
                style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted,
            )
        }

        // City card
        Box(
            Modifier.padding(16.dp).fillMaxWidth().height(112.dp).clip(RoundedCornerShape(22.dp))
                .border(1.dp, CityColors.GlassBorder, RoundedCornerShape(22.dp))
                .clickable(onClickLabel = "Change city") { vm.pickingCity = true },
        ) {
            Image(painterResource(city.imageRes), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF00F172A), Color(0x660F172A)))))
            Row(Modifier.fillMaxSize().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("YOUR CITY", style = MaterialTheme.typography.labelSmall, color = CityColors.SaffronText)
                    Text(city.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${city.state} • ${city.municipalShort}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                }
                Row(
                    Modifier.clip(CircleShape).background(CityColors.Glass).border(1.dp, CityColors.GlassBorder, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.SwapHoriz, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Change", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }

        SectionTitle("Your details")
        CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileField("Full name", name) { name = it.take(40) }
                val loginPhone = vm.signInChannel == com.sheharsaathi.app.data.OtpChannel.PHONE
                val loginEmail = vm.signInChannel == com.sheharsaathi.app.data.OtpChannel.EMAIL
                ProfileField("Mobile", phone, prefix = "+91  ", keyboard = KeyboardType.Phone, readOnly = loginPhone, error = if (!phoneOk) "Enter a 10-digit Indian mobile" else null) { v ->
                    phone = v.filter { it.isDigit() }.take(10)
                }
                ProfileField("Email", email, keyboard = KeyboardType.Email, readOnly = loginEmail, error = if (!emailOk) "Enter a valid email" else null) { email = it.trim().take(80) }
                ProfileField("Ward", ward, placeholder = "e.g. Ward 104, ${city.areas.first().first}") { ward = it.take(40) }
                AnimatedVisibility(dirty || savedFlash) {
                    PrimaryButton(
                        text = if (savedFlash && !dirty) "Saved" else "Save changes",
                        enabled = dirty && phoneOk && emailOk && name.isNotBlank(),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        vm.updateProfile(p.copy(name = name.trim(), phone = phone, email = email.trim(), ward = ward.trim()))
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        savedFlash = true
                        scope.launch { kotlinx.coroutines.delay(1400); savedFlash = false }
                    }
                }
            }
        }

        SectionTitle("Notifications")
        CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(Modifier.padding(vertical = 4.dp)) {
                NotifRow(Icons.Filled.ReportProblem, "Civic Alerts", "Updates on your reports and ward works", vm.notif["civic"] == true) { vm.setNotif("civic", it) }
                NotifRow(Icons.Filled.Event, "Local Events", "New events near you in ${city.name}", vm.notif["events"] == true) { vm.setNotif("events", it) }
                NotifRow(Icons.Filled.Campaign, "Emergency Broadcasts", "Floods, outages and safety alerts", vm.notif["emergency"] == true) { vm.setNotif("emergency", it) }
            }
        }

        SectionTitle("Saved")
        val bookmarked = vm.events.filter { vm.savedEvents[it.id] == true }
        val savedNews = vm.news.count { vm.savedNews[it.id] == true }
        CityCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(Modifier.padding(vertical = 4.dp)) {
                SavedRow(Icons.Filled.Place, "Saved places", "${vm.savedPlaces.size}")
                vm.savedPlaces.take(3).forEach { pl ->
                    Text("• ${pl.name}", style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted, modifier = Modifier.padding(start = 56.dp, bottom = 4.dp))
                }
                SavedRow(Icons.Filled.Bookmark, "Bookmarked events", "${bookmarked.size}")
                bookmarked.take(4).forEach { e ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenEvent(e.id) }.padding(start = 56.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(e.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text("${e.dateLabel} • ${e.venue}", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 1)
                        }
                        if (e.hostedByYou) {
                            Text("Hosted by you", style = MaterialTheme.typography.labelSmall, color = CityColors.SaffronText)
                            Spacer(Modifier.width(6.dp))
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CityColors.Muted, modifier = Modifier.size(18.dp))
                    }
                }
                SavedRow(Icons.Filled.Bookmark, "Saved news", "$savedNews")
            }
        }

        OutlinedButton(
            onClick = { confirmSignOut = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CityColors.Red.copy(alpha = 0.6f)),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFFCA5A5))
            Spacer(Modifier.width(8.dp))
            Text("Sign out", color = Color(0xFFFCA5A5), modifier = Modifier.padding(vertical = 6.dp))
        }
    }

    if (photoSheet) {
        ModalBottomSheet(onDismissRequest = { photoSheet = false }, containerColor = CityColors.Surface) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text("Profile photo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                SheetAction(Icons.Filled.PhotoCamera, "Take a photo") { photoSheet = false; camera.launch(null) }
                SheetAction(Icons.Filled.Image, "Choose from gallery") {
                    photoSheet = false
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                if (p.photoUri != null) {
                    SheetAction(Icons.Filled.Delete, "Remove photo", tint = CityColors.Red) {
                        photoSheet = false
                        vm.updateProfile(vm.profile.copy(photoUri = null))
                    }
                }
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out of Shehar Saathi?") },
            text = { Text("You'll need a new one-time code to sign back in. Your profile stays on this phone.") },
            confirmButton = { TextButton(onClick = { confirmSignOut = false; vm.signOut() }) { Text("Sign out", color = Color(0xFFFCA5A5)) } },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(), style = MaterialTheme.typography.labelMedium, color = CityColors.Muted,
        modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun ProfileField(
    label: String, value: String, prefix: String? = null, placeholder: String? = null,
    keyboard: KeyboardType = KeyboardType.Text, error: String? = null, readOnly: Boolean = false, onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, readOnly = readOnly,
        prefix = prefix?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it, color = CityColors.Muted) } },
        isError = error != null,
        supportingText = (error ?: if (readOnly) "Used to sign in, so it can't be changed here" else null)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = CityColors.SurfaceHigh, focusedContainerColor = CityColors.SurfaceHigh,
            unfocusedBorderColor = Color.Transparent, focusedBorderColor = CityColors.Indigo, cursorColor = CityColors.Saffron,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NotifRow(icon: ImageVector, title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(sub, color = CityColors.Muted) },
        leadingContent = { Icon(icon, null, tint = if (on) CityColors.Saffron else CityColors.Muted) },
        trailingContent = {
            Switch(
                checked = on, onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedTrackColor = CityColors.Saffron, checkedThumbColor = Color.White),
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onChange(!on) },
    )
}

@Composable
private fun SavedRow(icon: ImageVector, title: String, count: String) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, null, tint = CityColors.Indigo) },
        trailingContent = { Text(count, style = MaterialTheme.typography.titleMedium, color = CityColors.IndigoDeep) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, tint: Color = CityColors.Ink, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, color = tint) },
        leadingContent = { Icon(icon, null, tint = tint) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp),
    )
}
