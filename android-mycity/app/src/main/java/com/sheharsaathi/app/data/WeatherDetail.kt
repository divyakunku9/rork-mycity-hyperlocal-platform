package com.sheharsaathi.app.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/** Everything the full weather screen shows. Times are already local to the place. */
data class WeatherDetail(
    val now: Weather,
    val windKmh: Int,
    val windDeg: Int,
    val gustKmh: Int,
    val pressureHpa: Int,
    val visibilityKm: Double,
    val dewPointC: Int,
    val cloudPct: Int,
    val uvNow: Double,
    val precipMm: Double,
    val hours: List<HourWx>,
    val days: List<DayWx>,
    val air: AirDetail?,
    val fetchedAt: Long,
)

data class HourWx(val label: String, val tempC: Int, val code: Int, val isDay: Boolean, val rainPct: Int)

data class DayWx(
    val label: String,       // "Today", "Mon"
    val date: String,        // "8 Nov"
    val code: Int,
    val maxC: Int,
    val minC: Int,
    val rainPct: Int,
    val rainMm: Double,
    val uvMax: Double,
    val sunrise: String,     // "6:17 AM"
    val sunset: String,
    val daylightMin: Int,
)

data class AirDetail(
    val usAqi: Int,
    val pm25: Double?, val pm10: Double?, val o3: Double?, val no2: Double?, val so2: Double?, val co: Double?,
    val hours: List<Pair<String, Int>>,
) {
    /** Rough reading on India's CPCB scale, from the particulate levels alone. */
    val indiaAqi: Int? get() {
        val a = pm25?.let { sub(it, listOf(0.0 to 0, 30.0 to 50, 60.0 to 100, 90.0 to 200, 120.0 to 300, 250.0 to 400, 380.0 to 500)) }
        val b = pm10?.let { sub(it, listOf(0.0 to 0, 50.0 to 50, 100.0 to 100, 250.0 to 200, 350.0 to 300, 430.0 to 400, 510.0 to 500)) }
        return listOfNotNull(a, b).maxOrNull()
    }
    private fun sub(c: Double, bp: List<Pair<Double, Int>>): Int {
        for (i in 1 until bp.size) {
            val (cLo, iLo) = bp[i - 1]; val (cHi, iHi) = bp[i]
            if (c <= cHi) return (iLo + (c - cLo) / (cHi - cLo) * (iHi - iLo)).roundToInt()
        }
        return 500
    }
}

fun usAqiLabel(aqi: Int): String = when {
    aqi <= 50 -> "Good"
    aqi <= 100 -> "Moderate"
    aqi <= 150 -> "Unhealthy for sensitive groups"
    aqi <= 200 -> "Unhealthy"
    aqi <= 300 -> "Very unhealthy"
    else -> "Hazardous"
}

fun usAqiAdvice(aqi: Int): String = when {
    aqi <= 50 -> "Air quality is good. A great time to be outdoors."
    aqi <= 100 -> "Acceptable for most people. Unusually sensitive people should limit long, heavy exertion outdoors."
    aqi <= 150 -> "Children, older adults and people with asthma or heart conditions should cut down long or heavy outdoor activity."
    aqi <= 200 -> "Everyone may feel effects. Limit time outdoors and consider a mask (N95) in traffic."
    aqi <= 300 -> "Health alert. Avoid outdoor exertion; keep windows closed."
    else -> "Emergency conditions. Stay indoors and use air purification if you can."
}

fun indiaAqiLabel(aqi: Int): String = when {
    aqi <= 50 -> "Good"
    aqi <= 100 -> "Satisfactory"
    aqi <= 200 -> "Moderate"
    aqi <= 300 -> "Poor"
    aqi <= 400 -> "Very poor"
    else -> "Severe"
}

fun uvLabel(uv: Double): String = when {
    uv < 3 -> "Low"
    uv < 6 -> "Moderate"
    uv < 8 -> "High"
    uv < 11 -> "Very high"
    else -> "Extreme"
}

fun compass(deg: Int): String {
    val names = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    return names[(((deg % 360) + 360) % 360 * 16 + 180) / 360 % 16]
}

/** One-line summary of the hours ahead, like a forecaster would say it. */
fun WeatherDetail.headline(): String {
    val today = days.firstOrNull() ?: return now.condition
    val next = hours.take(12)
    val rainSoon = next.maxOfOrNull { it.rainPct } ?: 0
    // The sky most of the next 12 hours, not the day's worst moment.
    val skyCode = next.groupingBy { it.code }.eachCount().maxByOrNull { it.value }?.key ?: now.code
    val sky = weatherText(skyCode)
    val temps = if (now.isDay) "High ${maxOf(today.maxC, now.tempC)}°, low ${next.minOfOrNull { it.tempC } ?: today.minC}° tonight."
        else "Low ${next.minOfOrNull { it.tempC } ?: today.minC}° overnight, then ${days.getOrNull(1)?.maxC ?: today.maxC}° tomorrow."
    val rain = when {
        rainSoon >= 60 -> "Rain likely in the next 12 hours."
        rainSoon >= 30 -> "A chance of showers in the next 12 hours."
        else -> "No rain expected in the next 12 hours."
    }
    return "$sky ${if (now.isDay) "today" else "tonight"}. $temps $rain Winds ${compass(windDeg)} at $windKmh km/h."
}

@Serializable private data class DCurrent(
    val time: String = "",
    @SerialName("temperature_2m") val temp: Double = 0.0,
    @SerialName("apparent_temperature") val feels: Double = 0.0,
    @SerialName("relative_humidity_2m") val humidity: Double = 0.0,
    @SerialName("is_day") val isDay: Int = 1,
    val precipitation: Double = 0.0,
    @SerialName("weather_code") val code: Int = 0,
    @SerialName("cloud_cover") val cloud: Double = 0.0,
    @SerialName("pressure_msl") val pressure: Double = 0.0,
    @SerialName("wind_speed_10m") val wind: Double = 0.0,
    @SerialName("wind_direction_10m") val windDir: Double = 0.0,
    @SerialName("wind_gusts_10m") val gust: Double = 0.0,
    @SerialName("dew_point_2m") val dew: Double = 0.0,
    val visibility: Double = 0.0,
    @SerialName("uv_index") val uv: Double = 0.0,
)
@Serializable private data class DHourly(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temp: List<Double?> = emptyList(),
    @SerialName("weather_code") val code: List<Int?> = emptyList(),
    @SerialName("precipitation_probability") val rain: List<Int?> = emptyList(),
    @SerialName("is_day") val isDay: List<Int?> = emptyList(),
)
@Serializable private data class DDaily(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val code: List<Int?> = emptyList(),
    @SerialName("temperature_2m_max") val max: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val min: List<Double?> = emptyList(),
    @SerialName("precipitation_probability_max") val rain: List<Int?> = emptyList(),
    @SerialName("precipitation_sum") val rainMm: List<Double?> = emptyList(),
    @SerialName("uv_index_max") val uv: List<Double?> = emptyList(),
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList(),
)
@Serializable private data class DForecast(val current: DCurrent? = null, val hourly: DHourly = DHourly(), val daily: DDaily = DDaily())

@Serializable private data class ACurrent(
    @SerialName("us_aqi") val aqi: Double? = null,
    @SerialName("pm2_5") val pm25: Double? = null,
    val pm10: Double? = null,
    val ozone: Double? = null,
    @SerialName("nitrogen_dioxide") val no2: Double? = null,
    @SerialName("sulphur_dioxide") val so2: Double? = null,
    @SerialName("carbon_monoxide") val co: Double? = null,
)
@Serializable private data class AHourly(val time: List<String> = emptyList(), @SerialName("us_aqi") val aqi: List<Double?> = emptyList())
@Serializable private data class AResponse(val current: ACurrent? = null, val hourly: AHourly = AHourly())

object WeatherService {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val client by lazy { HttpClient(Android) }

    suspend fun detail(lat: Double, lng: Double): WeatherDetail? = withContext(Dispatchers.IO) {
        runCatching {
            coroutineScope {
                val f = async {
                    client.get("https://api.open-meteo.com/v1/forecast") {
                        parameter("latitude", lat); parameter("longitude", lng)
                        parameter("current", "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m,dew_point_2m,visibility,uv_index")
                        parameter("hourly", "temperature_2m,weather_code,precipitation_probability,is_day")
                        parameter("forecast_hours", 24)
                        parameter("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,uv_index_max,sunrise,sunset")
                        parameter("forecast_days", 10)
                        parameter("timezone", "auto")
                    }.bodyAsText()
                }
                val a = async {
                    runCatching {
                        client.get("https://air-quality-api.open-meteo.com/v1/air-quality") {
                            parameter("latitude", lat); parameter("longitude", lng)
                            parameter("current", "us_aqi,pm2_5,pm10,ozone,nitrogen_dioxide,sulphur_dioxide,carbon_monoxide")
                            parameter("hourly", "us_aqi")
                            parameter("forecast_hours", 24)
                            parameter("timezone", "auto")
                        }.bodyAsText()
                    }.getOrNull()
                }
                val fc = json.decodeFromString<DForecast>(f.await())
                val air = a.await()?.let { runCatching { json.decodeFromString<AResponse>(it) }.getOrNull() }
                parse(fc, air)
            }
        }.getOrNull()
    }

    private fun parse(fc: DForecast, air: AResponse?): WeatherDetail? {
        val c = fc.current ?: return null
        val nowHour = c.time.take(13) // "2026-11-07T18"
        val h = fc.hourly
        val hours = h.time.indices.mapNotNull { i ->
            val t = h.time[i]
            if (t.take(13) < nowHour) return@mapNotNull null
            HourWx(
                label = if (t.take(13) == nowHour) "Now" else hourLabel(t),
                tempC = (h.temp.getOrNull(i) ?: return@mapNotNull null).roundToInt(),
                code = h.code.getOrNull(i) ?: 0,
                isDay = (h.isDay.getOrNull(i) ?: 1) == 1,
                rainPct = h.rain.getOrNull(i) ?: 0,
            )
        }.take(24)
        val d = fc.daily
        val days = d.time.indices.mapNotNull { i ->
            val rise = d.sunrise.getOrNull(i).orEmpty(); val set = d.sunset.getOrNull(i).orEmpty()
            DayWx(
                label = when (i) { 0 -> "Today"; else -> weekday(d.time[i]) },
                date = shortDate(d.time[i]),
                code = d.code.getOrNull(i) ?: 0,
                maxC = (d.max.getOrNull(i) ?: return@mapNotNull null).roundToInt(),
                minC = (d.min.getOrNull(i) ?: return@mapNotNull null).roundToInt(),
                rainPct = d.rain.getOrNull(i) ?: 0,
                rainMm = d.rainMm.getOrNull(i) ?: 0.0,
                uvMax = d.uv.getOrNull(i) ?: 0.0,
                sunrise = clock(rise), sunset = clock(set),
                daylightMin = (minutes(set) - minutes(rise)).coerceAtLeast(0),
            )
        }
        val ac = air?.current
        val airDetail = ac?.aqi?.let { aqi ->
            val ah = air.hourly
            AirDetail(
                usAqi = aqi.roundToInt(),
                pm25 = ac.pm25, pm10 = ac.pm10, o3 = ac.ozone, no2 = ac.no2, so2 = ac.so2, co = ac.co,
                hours = ah.time.indices.mapNotNull { i ->
                    val t = ah.time[i]
                    if (t.take(13) < nowHour) null
                    else (ah.aqi.getOrNull(i) ?: return@mapNotNull null).let { (if (t.take(13) == nowHour) "Now" else hourLabel(t)) to it.roundToInt() }
                }.take(24),
            )
        }
        return WeatherDetail(
            now = Weather(
                tempC = c.temp.roundToInt(), feelsC = c.feels.roundToInt(), code = c.code, isDay = c.isDay == 1,
                humidity = c.humidity.roundToInt(), aqi = airDetail?.usAqi, fetchedAt = System.currentTimeMillis(),
            ),
            windKmh = c.wind.roundToInt(), windDeg = c.windDir.roundToInt(), gustKmh = c.gust.roundToInt(),
            pressureHpa = c.pressure.roundToInt(), visibilityKm = c.visibility / 1000.0, dewPointC = c.dew.roundToInt(),
            cloudPct = c.cloud.roundToInt(), uvNow = c.uv, precipMm = c.precipitation,
            hours = hours, days = days, air = airDetail, fetchedAt = System.currentTimeMillis(),
        )
    }

    // Local times arrive as "2026-11-07T18:00"; dates as "2026-11-07".
    internal fun hourLabel(t: String): String {
        val hh = t.substring(11, 13).toIntOrNull() ?: return t
        return when { hh == 0 -> "12 AM"; hh < 12 -> "$hh AM"; hh == 12 -> "12 PM"; else -> "${hh - 12} PM" }
    }
    internal fun clock(t: String): String {
        if (t.length < 16) return "—"
        val hh = t.substring(11, 13).toIntOrNull() ?: return "—"
        val mm = t.substring(14, 16)
        val h12 = when { hh == 0 -> 12; hh > 12 -> hh - 12; else -> hh }
        return "$h12:$mm ${if (hh < 12) "AM" else "PM"}"
    }
    private fun minutes(t: String): Int =
        if (t.length < 16) 0 else (t.substring(11, 13).toIntOrNull() ?: 0) * 60 + (t.substring(14, 16).toIntOrNull() ?: 0)
    private fun cal(date: String): Calendar? {
        val p = date.take(10).split("-").mapNotNull { it.toIntOrNull() }
        if (p.size != 3) return null
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ENGLISH).apply { clear(); set(p[0], p[1] - 1, p[2]) }
    }
    internal fun weekday(date: String): String =
        cal(date)?.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.ENGLISH) ?: date
    internal fun shortDate(date: String): String =
        cal(date)?.let { "${it.get(Calendar.DAY_OF_MONTH)} ${it.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH)}" } ?: date
}
