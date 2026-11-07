package com.sheharsaathi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sheharsaathi.app.data.AirDetail
import com.sheharsaathi.app.data.CityViewModel
import com.sheharsaathi.app.data.DayWx
import com.sheharsaathi.app.data.WeatherDetail
import com.sheharsaathi.app.data.backend.Times
import com.sheharsaathi.app.data.compass
import com.sheharsaathi.app.data.headline
import com.sheharsaathi.app.data.indiaAqiLabel
import com.sheharsaathi.app.data.usAqiAdvice
import com.sheharsaathi.app.data.usAqiLabel
import com.sheharsaathi.app.data.uvLabel
import com.sheharsaathi.app.data.weatherText
import com.sheharsaathi.app.ui.components.CityCard
import com.sheharsaathi.app.ui.theme.CityColors
import java.util.Locale
import kotlin.math.roundToInt

private fun codeIcon(code: Int, isDay: Boolean): ImageVector = when {
    code in setOf(95, 96, 99) -> Icons.Filled.Thunderstorm
    code in 51..67 || code in 80..82 -> Icons.Filled.WaterDrop
    code in 71..86 -> Icons.Filled.Grain
    code in setOf(2, 3, 45, 48) -> Icons.Filled.Cloud
    !isDay -> Icons.Filled.NightsStay
    else -> Icons.Filled.WbSunny
}

private fun codeTint(code: Int, isDay: Boolean): Color = when {
    code in setOf(95, 96, 99) -> Color(0xFFFACC15)
    code in 51..86 -> Color(0xFF60A5FA)
    code in setOf(2, 3, 45, 48) -> Color(0xFFCBD5E1)
    !isDay -> Color(0xFFC7D2FE)
    else -> Color(0xFFFBBF24)
}

private fun aqiTone(aqi: Int): Color = when {
    aqi <= 50 -> Color(0xFF22C55E)
    aqi <= 100 -> Color(0xFFEAB308)
    aqi <= 150 -> Color(0xFFF97316)
    aqi <= 200 -> Color(0xFFEF4444)
    aqi <= 300 -> Color(0xFFA855F7)
    else -> Color(0xFF9F1239)
}

private fun heroBrush(d: WeatherDetail): Brush {
    val c = d.now.code
    val colors = when {
        c in 51..99 -> listOf(Color(0xFF1E293B), Color(0xFF334155))
        !d.now.isDay -> listOf(Color(0xFF0B1026), Color(0xFF312E81))
        c in setOf(2, 3, 45, 48) -> listOf(Color(0xFF1E3A5F), Color(0xFF475569))
        else -> listOf(Color(0xFF1D4ED8), Color(0xFF0EA5E9))
    }
    return Brush.verticalGradient(colors)
}

/** Full weather and air-quality summary for where the user is, opened from the header chip. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(vm: CityViewModel, onBack: () -> Unit) {
    LaunchedEffect(vm.userPlace) { vm.loadWeatherDetail() }
    val d = vm.weatherDetail

    Column(Modifier.fillMaxSize().background(CityColors.Canvas)) {
        Row(
            Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = CityColors.Ink) }
            Column(Modifier.weight(1f)) {
                Text("Weather", style = MaterialTheme.typography.titleLarge, color = CityColors.Ink)
                Text(vm.locationLabel, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 1)
            }
            if (vm.weatherDetailLoading) CircularProgressIndicator(Modifier.padding(end = 16.dp).size(20.dp), strokeWidth = 2.dp, color = CityColors.Indigo)
            else IconButton(onClick = { vm.loadWeatherDetail() }) { Icon(Icons.Filled.Refresh, "Refresh weather", tint = CityColors.Muted) }
        }

        PullToRefreshBox(
            isRefreshing = vm.weatherDetailLoading && d != null,
            onRefresh = { vm.loadWeatherDetail() },
            modifier = Modifier.fillMaxSize(),
        ) {
            if (d == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (vm.weatherDetailLoading) CircularProgressIndicator(color = CityColors.Indigo)
                    else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Forecast unavailable", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { vm.loadWeatherDetail() }) { Text("Try again") }
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().navigationBarsPadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item(key = "hero") { Hero(d) }
                    item(key = "hourly") { Hourly(d) }
                    item(key = "air") { d.air?.let { AirQuality(it) } ?: Section("Air quality") { Text("Air-quality data isn't available here right now.", color = CityColors.Muted) } }
                    item(key = "daily") { Daily(d.days) }
                    item(key = "details") { Details(d) }
                    d.days.firstOrNull()?.let { today -> item(key = "sun") { Sun(today, d.days.getOrNull(1)) } }
                    item(key = "credit") {
                        Text(
                            "Forecast: Open-Meteo, using national weather service models. Air quality: Copernicus (CAMS) via Open-Meteo. " +
                                "Updated ${Times.ago(d.fetchedAt).lowercase()}.",
                            style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, trailing: String? = null, content: @Composable () -> Unit) {
    CityCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title.uppercase(Locale.ENGLISH), style = MaterialTheme.typography.labelMedium, color = CityColors.Muted, modifier = Modifier.weight(1f))
                trailing?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = CityColors.Muted) }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun Hero(d: WeatherDetail) {
    val today = d.days.firstOrNull()
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(heroBrush(d)).padding(20.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Now ${d.now.tempC} degrees, ${d.now.condition}" },
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("${d.now.tempC}°", fontSize = 84.sp, fontWeight = FontWeight.Light, color = Color.White, lineHeight = 88.sp)
                    Text(d.now.condition, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("Feels like ${d.now.feelsC}°", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                }
                Icon(codeIcon(d.now.code, d.now.isDay), null, tint = codeTint(d.now.code, d.now.isDay), modifier = Modifier.padding(top = 12.dp).size(72.dp))
            }
            if (today != null) {
                Spacer(Modifier.height(10.dp))
                Text("Day ${today.maxC}° · Night ${today.minC}°", style = MaterialTheme.typography.titleSmall, color = Color.White)
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.2f)))
            Spacer(Modifier.height(10.dp))
            Text(d.headline(), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.92f))
        }
    }
}

@Composable
private fun Hourly(d: WeatherDetail) {
    Section("Next 24 hours") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(d.hours) { h ->
                Column(
                    Modifier.width(58.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (h.label == "Now") CityColors.IndigoSoft else Color.Transparent)
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(h.label, style = MaterialTheme.typography.labelMedium, color = if (h.label == "Now") CityColors.Ink else CityColors.Muted)
                    Spacer(Modifier.height(8.dp))
                    Icon(codeIcon(h.code, h.isDay), null, tint = codeTint(h.code, h.isDay), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("${h.tempC}°", style = MaterialTheme.typography.titleMedium, color = CityColors.Ink)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.WaterDrop, null, tint = Color(0xFF60A5FA).copy(alpha = if (h.rainPct > 0) 1f else 0.35f), modifier = Modifier.size(11.dp))
                        Text("${h.rainPct}%", style = MaterialTheme.typography.labelSmall, color = if (h.rainPct >= 30) Color(0xFF93C5FD) else CityColors.Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun AirQuality(a: AirDetail) {
    val tone = aqiTone(a.usAqi)
    Section("Air quality", trailing = "US AQI") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(72.dp).border(4.dp, tone, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("${a.usAqi}", style = MaterialTheme.typography.headlineSmall, color = CityColors.Ink, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(usAqiLabel(a.usAqi), style = MaterialTheme.typography.titleMedium, color = tone)
                Spacer(Modifier.height(2.dp))
                Text(usAqiAdvice(a.usAqi), style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
            }
        }
        Spacer(Modifier.height(14.dp))
        AqiScale(a.usAqi)
        a.indiaAqi?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                "On India's CPCB scale this is about $it (${indiaAqiLabel(it)}), estimated from current PM2.5 and PM10.",
                style = MaterialTheme.typography.bodySmall, color = CityColors.Muted,
            )
        }
        Spacer(Modifier.height(14.dp))
        val items = listOfNotNull(
            a.pm25?.let { Triple("PM2.5", it, "µg/m³") }, a.pm10?.let { Triple("PM10", it, "µg/m³") },
            a.o3?.let { Triple("O₃", it, "µg/m³") }, a.no2?.let { Triple("NO₂", it, "µg/m³") },
            a.so2?.let { Triple("SO₂", it, "µg/m³") }, a.co?.let { Triple("CO", it / 1000.0, "mg/m³") },
        )
        items.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (name, v, unit) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(CityColors.SurfaceHigh).padding(10.dp)) {
                        Text(name, style = MaterialTheme.typography.labelMedium, color = CityColors.Muted)
                        Text(if (v >= 10) "${v.roundToInt()}" else String.format(Locale.US, "%.1f", v), style = MaterialTheme.typography.titleMedium, color = CityColors.Ink)
                        Text(unit, style = MaterialTheme.typography.labelSmall, color = CityColors.Muted)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (a.hours.size > 1) {
            Spacer(Modifier.height(6.dp))
            Text("Next 24 hours", style = MaterialTheme.typography.labelMedium, color = CityColors.Muted)
            Spacer(Modifier.height(8.dp))
            val top = (a.hours.maxOf { it.second }).coerceAtLeast(50).toFloat()
            Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                a.hours.forEach { (_, v) ->
                    Box(Modifier.weight(1f).fillMaxHeight(fraction = (v / top).coerceIn(0.05f, 1f)).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(aqiTone(v)))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(a.hours.first().first, style = MaterialTheme.typography.labelSmall, color = CityColors.Muted, modifier = Modifier.weight(1f))
                Text(a.hours[a.hours.size / 2].first, style = MaterialTheme.typography.labelSmall, color = CityColors.Muted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text(a.hours.last().first, style = MaterialTheme.typography.labelSmall, color = CityColors.Muted, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
private fun AqiScale(aqi: Int) {
    val bands = listOf(50 to Color(0xFF22C55E), 50 to Color(0xFFEAB308), 50 to Color(0xFFF97316), 50 to Color(0xFFEF4444), 100 to Color(0xFFA855F7), 200 to Color(0xFF9F1239))
    BoxWithConstraints(Modifier.fillMaxWidth().height(14.dp), contentAlignment = Alignment.CenterStart) {
        Row(Modifier.fillMaxWidth().height(6.dp).align(Alignment.Center).clip(CircleShape)) {
            bands.forEach { (w, c) -> Box(Modifier.weight(w.toFloat()).fillMaxHeight().background(c)) }
        }
        val x = maxWidth * (aqi.coerceIn(0, 500) / 500f)
        Box(Modifier.offset(x = (x - 7.dp).coerceAtLeast(0.dp)).size(14.dp).background(Color.White, CircleShape).border(3.dp, aqiTone(aqi), CircleShape))
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        listOf("0", "100", "200", "300", "500").forEachIndexed { i, s ->
            Text(s, style = MaterialTheme.typography.labelSmall, color = CityColors.Muted, modifier = Modifier.weight(1f), textAlign = if (i == 4) TextAlign.End else TextAlign.Start)
        }
    }
}

@Composable
private fun Daily(days: List<DayWx>) {
    if (days.isEmpty()) return
    val lo = days.minOf { it.minC }; val hi = days.maxOf { it.maxC }
    val span = (hi - lo).coerceAtLeast(1).toFloat()
    Section("${days.size}-day forecast") {
        days.forEachIndexed { i, d ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(CityColors.Outline.copy(alpha = 0.6f)))
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(64.dp)) {
                    Text(d.label, style = MaterialTheme.typography.titleSmall, color = CityColors.Ink)
                    Text(d.date, style = MaterialTheme.typography.labelSmall, color = CityColors.Muted)
                }
                Icon(codeIcon(d.code, true), weatherText(d.code), tint = codeTint(d.code, true), modifier = Modifier.size(24.dp))
                Text(
                    if (d.rainPct > 0) "${d.rainPct}%" else "",
                    style = MaterialTheme.typography.labelSmall, color = Color(0xFF93C5FD),
                    modifier = Modifier.width(40.dp).padding(start = 6.dp),
                )
                Text("${d.minC}°", style = MaterialTheme.typography.bodyMedium, color = CityColors.Muted, modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
                Row(Modifier.weight(1f).padding(horizontal = 8.dp).height(6.dp).clip(CircleShape).background(CityColors.SurfaceHigh)) {
                    val start = (d.minC - lo) / span; val len = ((d.maxC - d.minC) / span).coerceAtLeast(0.04f)
                    if (start > 0f) Spacer(Modifier.weight(start))
                    Box(Modifier.weight(len).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(Color(0xFF60A5FA), Color(0xFFF59E0B)))))
                    val rest = 1f - start - len
                    if (rest > 0f) Spacer(Modifier.weight(rest))
                }
                Text("${d.maxC}°", style = MaterialTheme.typography.bodyMedium, color = CityColors.Ink, modifier = Modifier.width(34.dp))
            }
        }
    }
}

@Composable
private fun Details(d: WeatherDetail) {
    val tiles = listOf(
        Tile(Icons.Filled.WaterDrop, "Humidity", "${d.now.humidity}%", "Dew point ${d.dewPointC}°"),
        Tile(Icons.Filled.Navigation, "Wind", "${d.windKmh} km/h", "${compass(d.windDeg)} · gusts ${d.gustKmh} km/h", rotate = d.windDeg + 180f),
        Tile(Icons.Filled.LightMode, "UV index", String.format(Locale.US, "%.0f", d.days.firstOrNull()?.uvMax ?: d.uvNow), uvLabel(d.days.firstOrNull()?.uvMax ?: d.uvNow) + " today"),
        Tile(Icons.Filled.Compress, "Pressure", "${d.pressureHpa} hPa", "At sea level"),
        Tile(Icons.Filled.Visibility, "Visibility", if (d.visibilityKm >= 10) "${d.visibilityKm.roundToInt()} km" else String.format(Locale.US, "%.1f km", d.visibilityKm), if (d.visibilityKm >= 10) "Clear view" else "Reduced"),
        Tile(Icons.Filled.Cloud, "Cloud cover", "${d.cloudPct}%", if (d.precipMm > 0) String.format(Locale.US, "%.1f mm rain now", d.precipMm) else "No rain now"),
        Tile(Icons.Filled.Thermostat, "Feels like", "${d.now.feelsC}°", if (d.now.feelsC > d.now.tempC) "Humidity makes it feel warmer" else if (d.now.feelsC < d.now.tempC) "Wind makes it feel cooler" else "Same as actual"),
        Tile(Icons.Filled.Umbrella, "Rain today", "${d.days.firstOrNull()?.rainPct ?: 0}%", String.format(Locale.US, "%.1f mm expected", d.days.firstOrNull()?.rainMm ?: 0.0)),
    )
    Section("Today's details") {
        tiles.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { t -> DetailTile(t, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class Tile(val icon: ImageVector, val label: String, val value: String, val note: String, val rotate: Float = 0f)

@Composable
private fun DetailTile(t: Tile, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(CityColors.SurfaceHigh).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(t.icon, null, tint = CityColors.Indigo, modifier = Modifier.size(16.dp).rotate(t.rotate))
            Spacer(Modifier.width(6.dp))
            Text(t.label, style = MaterialTheme.typography.labelMedium, color = CityColors.Muted)
        }
        Spacer(Modifier.height(6.dp))
        Text(t.value, style = MaterialTheme.typography.titleLarge, color = CityColors.Ink)
        Text(t.note, style = MaterialTheme.typography.labelSmall, color = CityColors.Muted, maxLines = 2)
    }
}

@Composable
private fun Sun(today: DayWx, tomorrow: DayWx?) {
    Section("Sun") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SunItem(Icons.Filled.WbSunny, "Sunrise", today.sunrise, Modifier.weight(1f))
            SunItem(Icons.Filled.WbTwilight, "Sunset", today.sunset, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        val h = today.daylightMin / 60; val m = today.daylightMin % 60
        val diff = tomorrow?.let { it.daylightMin - today.daylightMin }
        Text(
            "${h}h ${m}m of daylight" + when {
                diff == null || diff == 0 -> ""
                diff > 0 -> " · tomorrow ${diff}m longer"
                else -> " · tomorrow ${-diff}m shorter"
            },
            style = MaterialTheme.typography.bodySmall, color = CityColors.Muted,
        )
    }
}

@Composable
private fun SunItem(icon: ImageVector, label: String, time: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(CityColors.SaffronSoft, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = CityColors.Saffron, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = CityColors.Muted)
            Text(time, style = MaterialTheme.typography.titleMedium, color = CityColors.Ink)
        }
    }
}
