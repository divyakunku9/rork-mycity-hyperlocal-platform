package com.rork.mycity.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume

enum class LocationSource { DEFAULT, GPS, MANUAL }

object LocationService {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun isLocationOn(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    /** One-shot fix from the phone: fresh fix first, last-known as a fallback. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(context: Context): Location? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = buildList {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) add(LocationManager.GPS_PROVIDER)
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) add(LocationManager.NETWORK_PROVIDER)
        }
        for (provider in providers.reversed()) { // network first: it's faster indoors
            val fix = withTimeoutOrNull(8_000) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val signal = CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    try {
                        LocationManagerCompat.getCurrentLocation(
                            lm, provider, signal, ContextCompat.getMainExecutor(context),
                        ) { loc -> if (cont.isActive) cont.resume(loc) }
                    } catch (_: Exception) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }
            if (fix != null) return fix
        }
        return providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }
}

@Serializable
private data class NominatimItem(
    @SerialName("place_id") val placeId: Long = 0,
    val lat: String,
    val lon: String,
    val name: String? = null,
    @SerialName("display_name") val displayName: String = "",
    val type: String? = null,
    val category: String? = null,
    val address: Map<String, String> = emptyMap(),
)

/** OpenStreetMap Nominatim geocoder, restricted to the current city. */
object Geocoder {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client by lazy { HttpClient(Android) }
    private const val UA = "MyCity-Android/1.0 (com.rork.mycity)"

    suspend fun search(query: String): List<Place> = withContext(Dispatchers.IO) {
        runCatching {
            val text = client.get("https://nominatim.openstreetmap.org/search") {
                header("User-Agent", UA)
                header("Accept-Language", "en")
                parameter("q", query)
                parameter("format", "jsonv2")
                parameter("addressdetails", 1)
                parameter("limit", 10)
                parameter("countrycodes", "in")
                val c = CurrentCity.city
                parameter("viewbox", "${c.west},${c.north},${c.east},${c.south}")
                parameter("bounded", 1)
            }.bodyAsText()
            json.decodeFromString<List<NominatimItem>>(text).mapNotNull { it.toPlace() }
        }.getOrDefault(emptyList())
    }

    suspend fun reverse(lat: Double, lng: Double): Place? = withContext(Dispatchers.IO) {
        runCatching {
            val text = client.get("https://nominatim.openstreetmap.org/reverse") {
                header("User-Agent", UA)
                header("Accept-Language", "en")
                parameter("lat", lat)
                parameter("lon", lng)
                parameter("format", "jsonv2")
                parameter("zoom", 17)
                parameter("addressdetails", 1)
            }.bodyAsText()
            json.decodeFromString<NominatimItem>(text).toPlace()?.copy(lat = lat, lng = lng)
        }.getOrNull()
    }

    private fun NominatimItem.toPlace(): Place? {
        val la = lat.toDoubleOrNull() ?: return null
        val lo = lon.toDoubleOrNull() ?: return null
        val area = address["suburb"] ?: address["neighbourhood"] ?: address["city_district"]
            ?: address["quarter"] ?: address["town"] ?: address["city"] ?: CurrentCity.city.name
        val title = name?.takeIf { it.isNotBlank() }
            ?: address["road"]?.let { r -> address["house_number"]?.let { "$it, $r" } ?: r }
            ?: area
        val kind = when {
            type == "station" || category == "railway" -> PlaceKind.METRO
            category in setOf("amenity", "leisure", "tourism", "shop") -> PlaceKind.VENUE
            else -> PlaceKind.LANDMARK
        }
        val sub = listOfNotNull(
            address["road"]?.takeIf { it != title },
            area.takeIf { it != title },
        ).distinct().joinToString(", ").ifBlank { displayName.split(",").drop(1).take(2).joinToString(",").trim() }
        return Place("osm-$placeId", title, sub.ifBlank { CurrentCity.city.name }, la, lo, kind)
    }

    /** Short "Area, City" label for headers. */
    fun headerLabel(p: Place): String {
        val city = CurrentCity.city.name
        val area = p.area.split(",").map { it.trim() }.lastOrNull { it.isNotBlank() && it != city && it != "Bangalore" && it != "Bombay" }
        return "${area ?: p.name}, $city"
    }
}
