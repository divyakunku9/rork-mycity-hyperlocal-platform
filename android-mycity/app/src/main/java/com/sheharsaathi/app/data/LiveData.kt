package com.sheharsaathi.app.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

data class Weather(
    val tempC: Int,
    val feelsC: Int,
    val code: Int,
    val isDay: Boolean,
    val humidity: Int,
    val aqi: Int?,
    val fetchedAt: Long,
) {
    val condition: String get() = weatherText(code)
    val aqiLabel: String
        get() = when (aqi) {
            null -> "—"
            in 0..50 -> "Good"
            in 51..100 -> "Moderate"
            in 101..150 -> "Sensitive"
            in 151..200 -> "Unhealthy"
            else -> "Very poor"
        }
}

fun weatherText(code: Int): String = when (code) {
    0 -> "Clear"
    1, 2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55, 56, 57 -> "Drizzle"
    61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"
    71, 73, 75, 77, 85, 86 -> "Snow"
    95, 96, 99 -> "Thunderstorm"
    else -> "Haze"
}

enum class TrendKind(val tag: String) { LANDMARK("LANDMARK"), PARK("PARK"), MARKET("MARKET"), CAFE("CAFE"), FOOD("FOOD") }

data class TrendingPlace(
    val id: String,
    val name: String,
    val kind: TrendKind,
    val lat: Double,
    val lng: Double,
    val distanceKm: Double,
)

@Serializable private data class OmCurrent(
    @SerialName("temperature_2m") val temp: Double = 0.0,
    @SerialName("apparent_temperature") val feels: Double = 0.0,
    @SerialName("weather_code") val code: Int = 0,
    @SerialName("is_day") val isDay: Int = 1,
    @SerialName("relative_humidity_2m") val humidity: Double = 0.0,
)
@Serializable private data class OmForecast(val current: OmCurrent? = null)
@Serializable private data class AqCurrent(@SerialName("us_aqi") val aqi: Double? = null)
@Serializable private data class AqResponse(val current: AqCurrent? = null)

@Serializable private data class OsmCenter(val lat: Double, val lon: Double)
@Serializable private data class OsmElement(
    val type: String = "node",
    val id: Long = 0,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OsmCenter? = null,
    val tags: Map<String, String> = emptyMap(),
)
@Serializable private data class OverpassResponse(val elements: List<OsmElement> = emptyList())

/** Free, keyless live data: Open-Meteo weather + air quality and OpenStreetMap Overpass places. */
object LiveData {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client by lazy { HttpClient(Android) }
    private const val UA = "SheharSaathi-Android/1.0 (com.sheharsaathi.app)"

    suspend fun weather(lat: Double, lng: Double): Weather? = withContext(Dispatchers.IO) {
        runCatching {
            coroutineScope {
                val f = async {
                    client.get("https://api.open-meteo.com/v1/forecast") {
                        parameter("latitude", lat); parameter("longitude", lng)
                        parameter("current", "temperature_2m,apparent_temperature,weather_code,is_day,relative_humidity_2m")
                        parameter("timezone", "auto")
                    }.bodyAsText()
                }
                val a = async {
                    runCatching {
                        client.get("https://air-quality-api.open-meteo.com/v1/air-quality") {
                            parameter("latitude", lat); parameter("longitude", lng)
                            parameter("current", "us_aqi")
                        }.bodyAsText()
                    }.getOrNull()
                }
                val cur = json.decodeFromString<OmForecast>(f.await()).current ?: return@coroutineScope null
                val aqi = a.await()?.let { runCatching { json.decodeFromString<AqResponse>(it).current?.aqi }.getOrNull() }
                Weather(
                    tempC = cur.temp.roundToInt(), feelsC = cur.feels.roundToInt(), code = cur.code,
                    isDay = cur.isDay == 1, humidity = cur.humidity.roundToInt(), aqi = aqi?.roundToInt(),
                    fetchedAt = System.currentTimeMillis(),
                )
            }
        }.getOrNull()
    }

    suspend fun trending(lat: Double, lng: Double, radiusM: Int = 3000): List<TrendingPlace> = withContext(Dispatchers.IO) {
        runCatching {
            val a = "around:$radiusM,$lat,$lng"
            val q = """
                [out:json][timeout:20];
                (
                  nwr($a)["tourism"~"attraction|museum|viewpoint|gallery"]["name"];
                  nwr($a)["historic"~"monument|fort|memorial|castle"]["name"];
                  way($a)["leisure"="park"]["name"];
                  nwr($a)["shop"="mall"]["name"];
                  nwr($a)["amenity"="marketplace"]["name"];
                  node($a)["amenity"="cafe"]["name"];
                  node($a)["amenity"="restaurant"]["name"]["cuisine"];
                );
                out center 80;
            """.trimIndent()
            val text = client.get("https://overpass-api.de/api/interpreter") {
                header("User-Agent", UA)
                parameter("data", q)
            }.bodyAsText()
            json.decodeFromString<OverpassResponse>(text).elements.mapNotNull { e ->
                val la = e.lat ?: e.center?.lat ?: return@mapNotNull null
                val lo = e.lon ?: e.center?.lon ?: return@mapNotNull null
                val name = e.tags["name:en"] ?: e.tags["name"] ?: return@mapNotNull null
                val t = e.tags
                val kind = when {
                    t["tourism"] != null || t["historic"] != null -> TrendKind.LANDMARK
                    t["leisure"] == "park" -> TrendKind.PARK
                    t["shop"] == "mall" || t["amenity"] == "marketplace" -> TrendKind.MARKET
                    t["amenity"] == "cafe" -> TrendKind.CAFE
                    else -> TrendKind.FOOD
                }
                TrendingPlace("${e.type}-${e.id}", name, kind, la, lo, Transit.distanceKm(lat, lng, la, lo))
            }
                .distinctBy { it.name.lowercase() }
                .sortedBy { it.kind.ordinal * 1.2 + it.distanceKm }
                .let { list ->
                    // Keep variety: at most 3 of each kind
                    list.groupBy { it.kind }.flatMap { (_, v) -> v.take(3) }.sortedBy { it.kind.ordinal * 1.2 + it.distanceKm }
                }
                .take(12)
        }.getOrDefault(emptyList())
    }
}
