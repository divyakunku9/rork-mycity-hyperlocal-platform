package com.sheharsaathi.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sheharsaathi.app.data.CityViewModel
import com.sheharsaathi.app.data.IssueStatus
import com.sheharsaathi.app.data.UserProfile
import com.sheharsaathi.app.data.Weather
import androidx.compose.animation.AnimatedContent
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.sheharsaathi.app.ui.theme.CityColors
import java.text.NumberFormat
import java.util.Locale

fun rupees(amount: Int): String =
    "₹" + NumberFormat.getIntegerInstance(Locale("en", "IN")).format(amount)

fun compactCount(n: Int): String = if (n >= 1000) String.format(Locale.US, "%.1fK", n / 1000f) else n.toString()

fun openDirections(context: Context, lat: Double, lng: Double, label: String) {
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        openUrl(context, "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
    }
}

fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app available to open this link", Toast.LENGTH_SHORT).show()
    }
}

fun dial(context: Context, number: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Dialer not available. Call $number", Toast.LENGTH_SHORT).show()
    }
}

fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        context.startActivity(Intent.createChooser(send, "Share via"))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app available to share", Toast.LENGTH_SHORT).show()
    }
}

/** Press-scale feedback for custom tappable surfaces. */
@Composable
fun rememberPressScale(interaction: MutableInteractionSource): Float {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = 600f), label = "press")
    return scale
}

/** Lets any header open the profile without threading a callback through every screen. */
val LocalOpenProfile = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun LocationHeader(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    gps: Boolean = false,
    locating: Boolean = false,
    vm: CityViewModel? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f, fill = false).clip(RoundedCornerShape(12.dp))
                .then(if (onClick != null) Modifier.clickable(onClickLabel = "Change location", onClick = onClick) else Modifier)
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                if (locating) {
                    androidx.compose.material3.CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = CityColors.Indigo)
                } else {
                    Icon(
                        if (gps) Icons.Filled.MyLocation else Icons.Filled.LocationOn, contentDescription = null,
                        tint = CityColors.Saffron, modifier = Modifier.size(if (gps) 20.dp else 24.dp),
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f, fill = false)) {
                if (caption != null) {
                    Text(caption, style = MaterialTheme.typography.labelSmall, color = CityColors.Muted, maxLines = 1)
                }
                Text(
                    if (locating) "Finding you…" else title,
                    style = MaterialTheme.typography.titleLarge,
                    color = CityColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = CityColors.Ink)
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        WeatherChip(vm?.weather)
        if (vm != null) {
            Spacer(Modifier.width(8.dp))
            val open = LocalOpenProfile.current
            ProfileAvatar(
                vm.profile, size = 38,
                modifier = Modifier.clip(CircleShape).clickable(onClickLabel = "Open profile", onClick = open),
            )
        }
    }
}

fun weatherIcon(w: Weather?): ImageVector = when {
    w == null -> Icons.Filled.WbSunny
    w.code in setOf(95, 96, 99) -> Icons.Filled.Thunderstorm
    w.code in 51..67 || w.code in 80..82 -> Icons.Filled.WaterDrop
    w.code in setOf(2, 3, 45, 48) -> Icons.Filled.Cloud
    !w.isDay -> Icons.Filled.NightsStay
    else -> Icons.Filled.WbSunny
}

fun aqiColor(aqi: Int?): Color = when {
    aqi == null -> CityColors.Muted
    aqi <= 50 -> CityColors.GreenText
    aqi <= 100 -> CityColors.SaffronText
    else -> Color(0xFFFCA5A5)
}

@Composable
fun WeatherChip(weather: Weather? = null) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(CityColors.Glass)
            .border(1.dp, CityColors.GlassBorder, CircleShape)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = weather?.let { "${it.tempC} degrees, ${it.condition}, air quality ${it.aqi ?: "unknown"}" } ?: "Loading weather"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(weatherIcon(weather), contentDescription = null, tint = CityColors.Saffron, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        AnimatedContent(weather?.tempC, label = "temp") { t ->
            Text(if (t == null) "--°" else "$t°C", style = MaterialTheme.typography.labelLarge, color = CityColors.Ink)
        }
        if (weather?.aqi != null) {
            Spacer(Modifier.width(6.dp))
            Text("AQI ${weather.aqi}", style = MaterialTheme.typography.labelLarge, color = aqiColor(weather.aqi))
        }
    }
}

/** Initials or photo avatar. */
@Composable
fun ProfileAvatar(profile: UserProfile, size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size.dp).clip(CircleShape)
            .background(Brush.linearGradient(listOf(CityColors.Saffron, Color(0xFFE85D3A))))
            .border(1.5.dp, CityColors.GlassBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (profile.photoUri != null) {
            coil3.compose.AsyncImage(
                profile.photoUri, contentDescription = "Profile photo",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.matchParentSize(),
            )
        } else {
            Text(
                profile.initials, color = Color.White, fontWeight = FontWeight.Bold,
                fontSize = (size * 0.38f).sp,
            )
        }
    }
}

/** Event header image: user photo if they added one, otherwise the bundled artwork. */
@Composable
fun EventImage(e: com.sheharsaathi.app.data.CityEvent, modifier: Modifier = Modifier, description: String? = null) {
    if (e.imageUri != null) {
        coil3.compose.AsyncImage(
            e.imageUri, contentDescription = description,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = modifier,
        )
    } else {
        androidx.compose.foundation.Image(
            androidx.compose.ui.res.painterResource(e.imageRes), contentDescription = description,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = modifier,
        )
    }
}

@Composable
fun CitySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        placeholder = { Text(placeholder, color = CityColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = CityColors.Ink) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = CityColors.Surface,
            focusedContainerColor = CityColors.SurfaceHigh,
            unfocusedBorderColor = CityColors.Outline,
            focusedBorderColor = CityColors.Indigo,
        ),
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = CityColors.Ink, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun TagLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun SoftBadge(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
    }
}

fun statusColors(status: IssueStatus): Pair<Color, Color> = when (status) {
    IssueStatus.REPORTED -> CityColors.RedSoft to CityColors.Red
    IssueStatus.UNDER_REVIEW -> CityColors.SaffronSoft to CityColors.SaffronText
    IssueStatus.IN_PROGRESS -> CityColors.IndigoSoft to CityColors.Indigo
    IssueStatus.RESOLVED -> CityColors.GreenSoft to CityColors.Green
}

@Composable
fun StatusBadge(status: IssueStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = statusColors(status)
    val animBg by animateColorAsState(bg, label = "statusBg")
    SoftBadge(status.label, animBg, fg, modifier)
}

@Composable
fun IconTile(icon: ImageVector, tint: Color, container: Color, size: Int = 44) {
    Box(
        modifier = Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.52f).dp))
    }
}

@Composable
fun CityCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = CityColors.Outline,
    container: Color = CityColors.Surface,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = if (onClick != null) rememberPressScale(interaction) else 1f
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.scale(scale),
            shape = RoundedCornerShape(18.dp),
            color = container,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            interactionSource = interaction,
        ) { content() }
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(18.dp),
            color = container,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        ) { content() }
    }
}

