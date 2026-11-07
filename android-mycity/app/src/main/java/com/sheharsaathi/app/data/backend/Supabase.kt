package com.sheharsaathi.app.data.backend

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The project's public address and publishable key. The publishable key is meant to ship
 * inside the app: row-level security in the database decides what each user may do.
 * Never put the secret / service_role key here.
 */
object SupabaseConfig {
    const val URL = "https://vcdjdothkofhbyooecji.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_bQdDi2iGoph4CkN4dIaDYQ_246IAEWv"

    /** Cities whose content comes from the backend. Others keep the built-in sample content. */
    val liveCities = setOf("hyderabad")
}

/** A signed-in session as returned by Supabase Auth. */
data class Session(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtSec: Long,
    val userId: String,
)

/** Outcome of a backend call: either a value or a message that can be shown to the user. */
sealed interface Res<out T> {
    data class Ok<T>(val value: T) : Res<T>
    data class Err(val message: String, val status: Int = 0) : Res<Nothing>
}

fun <T> Res<T>.valueOrNull(): T? = (this as? Res.Ok<T>)?.value

/** Keeps the session across app restarts. */
class SessionStore(context: Context) {
    private val sp = context.getSharedPreferences("mycity_auth", Context.MODE_PRIVATE)

    fun load(): Session? {
        val access = sp.getString("access", null) ?: return null
        val refresh = sp.getString("refresh", null) ?: return null
        val uid = sp.getString("uid", null) ?: return null
        return Session(access, refresh, sp.getLong("exp", 0L), uid)
    }

    fun save(s: Session) {
        sp.edit().putString("access", s.accessToken).putString("refresh", s.refreshToken)
            .putLong("exp", s.expiresAtSec).putString("uid", s.userId).apply()
    }

    fun clear() {
        sp.edit().clear().apply()
    }
}

@Serializable
private data class AuthUser(val id: String)

@Serializable
private data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long = 3600,
    @SerialName("expires_at") val expiresAt: Long? = null,
    val user: AuthUser,
)

/**
 * Minimal Supabase client over plain HTTP: Auth (one-time codes), the REST API
 * (PostgREST) and Storage. Uses the app's existing Ktor client, so no new libraries.
 */
class Supabase(context: Context) {
    val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
    private val http by lazy { HttpClient(Android) }
    private val store = SessionStore(context)
    private val refreshLock = Mutex()

    var session: Session? = store.load()
        private set

    val userId: String? get() = session?.userId

    /** Called when the server says the session can no longer be renewed, so the app can ask to sign in again. */
    var onSessionLost: (() -> Unit)? = null
    val signedIn: Boolean get() = session != null

    // ------------------------------------------------------------------ Auth

    /** Sends a 6-digit code by SMS (phone as "+91XXXXXXXXXX") or by email. */
    suspend fun sendOtp(phone: String? = null, email: String? = null): Res<Unit> {
        val body = if (phone != null) """{"phone":${q(phone)},"create_user":true}"""
        else """{"email":${q(email ?: "")},"create_user":true}"""
        val r = call { http.post("${SupabaseConfig.URL}/auth/v1/otp") { base(auth = false); jsonBody(body) } }
        return when {
            r == null -> Res.Err(OFFLINE)
            r.status.value in 200..299 -> Res.Ok(Unit)
            else -> Res.Err(authError(r.status.value, r.bodyAsText()), r.status.value)
        }
    }

    /** Checks the code; on success the session is stored and [signedIn] becomes true. */
    suspend fun verifyOtp(code: String, phone: String? = null, email: String? = null): Res<Session> {
        val body = if (phone != null) """{"type":"sms","phone":${q(phone)},"token":${q(code)}}"""
        else """{"type":"email","email":${q(email ?: "")},"token":${q(code)}}"""
        val r = call { http.post("${SupabaseConfig.URL}/auth/v1/verify") { base(auth = false); jsonBody(body) } }
            ?: return Res.Err(OFFLINE)
        val text = r.bodyAsText()
        if (r.status.value !in 200..299) return Res.Err(authError(r.status.value, text), r.status.value)
        return runCatching { json.decodeFromString<AuthResponse>(text) }
            .map { Res.Ok(keep(it)) as Res<Session> }
            .getOrElse { Res.Err("Unexpected reply from the server") }
    }

    suspend fun signOut() {
        val s = session ?: return
        call {
            http.post("${SupabaseConfig.URL}/auth/v1/logout") {
                header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                header("Authorization", "Bearer ${s.accessToken}")
            }
        }
        session = null
        store.clear()
    }

    /** Swaps the refresh token for a fresh access token. Returns false if the session is no longer valid. */
    suspend fun refresh(): Boolean = refreshLock.withLock {
        val s = session ?: return@withLock false
        if (!expiresSoon(s)) return@withLock true
        val r = call {
            http.post("${SupabaseConfig.URL}/auth/v1/token") {
                parameter("grant_type", "refresh_token")
                base(auth = false)
                jsonBody("""{"refresh_token":${q(s.refreshToken)}}""")
            }
        } ?: return@withLock true // offline: keep the session and try again later
        if (r.status.value in 200..299) {
            runCatching { keep(json.decodeFromString<AuthResponse>(r.bodyAsText())) }
            true
        } else if (r.status.value in 400..499) {
            session = null; store.clear(); onSessionLost?.invoke(); false
        } else true
    }

    private fun keep(a: AuthResponse): Session {
        val exp = a.expiresAt ?: (System.currentTimeMillis() / 1000 + a.expiresIn)
        val s = Session(a.accessToken, a.refreshToken, exp, a.user.id)
        session = s
        store.save(s)
        return s
    }

    private fun expiresSoon(s: Session) = s.expiresAtSec - System.currentTimeMillis() / 1000 < 60

    // ------------------------------------------------------------------ Database (PostgREST)

    /** GET /rest/v1/[table] with PostgREST query parameters, e.g. "select" to "*", "city_id" to "eq.hyderabad". */
    suspend fun select(table: String, vararg params: Pair<String, String>): Res<String> =
        rest { http.get("${SupabaseConfig.URL}/rest/v1/$table") { base(); params.forEach { (k, v) -> parameter(k, v) } } }

    /** Inserts one row (JSON object). With [upsert], an existing row with the same key is kept. */
    suspend fun insert(table: String, rowJson: String, upsert: Boolean = false, returning: Boolean = false): Res<String> =
        rest {
            http.post("${SupabaseConfig.URL}/rest/v1/$table") {
                base()
                val prefer = buildList {
                    add(if (returning) "return=representation" else "return=minimal")
                    if (upsert) add("resolution=ignore-duplicates")
                }
                header("Prefer", prefer.joinToString(","))
                jsonBody(rowJson)
            }
        }

    suspend fun update(table: String, changesJson: String, vararg filters: Pair<String, String>): Res<String> =
        rest {
            http.patch("${SupabaseConfig.URL}/rest/v1/$table") {
                base(); header("Prefer", "return=minimal")
                filters.forEach { (k, v) -> parameter(k, v) }
                jsonBody(changesJson)
            }
        }

    suspend fun delete(table: String, vararg filters: Pair<String, String>): Res<String> =
        rest {
            http.delete("${SupabaseConfig.URL}/rest/v1/$table") {
                base(); header("Prefer", "return=minimal")
                filters.forEach { (k, v) -> parameter(k, v) }
            }
        }

    /** Calls a database function, e.g. book_event. */
    suspend fun rpc(fn: String, argsJson: String): Res<String> =
        rest { http.post("${SupabaseConfig.URL}/rest/v1/rpc/$fn") { base(); jsonBody(argsJson) } }

    // ------------------------------------------------------------------ Storage

    /** Uploads (or replaces) a file and returns its storage path. */
    suspend fun upload(bucket: String, path: String, bytes: ByteArray, mime: String = "image/jpeg"): Res<String> {
        val r = rest {
            http.post("${SupabaseConfig.URL}/storage/v1/object/$bucket/$path") {
                base()
                header("x-upsert", "true")
                header("Cache-Control", "max-age=3600")
                contentType(ContentType.parse(mime))
                setBody(bytes)
            }
        }
        return when (r) {
            is Res.Ok -> Res.Ok(path)
            is Res.Err -> r
        }
    }

    fun publicUrl(bucket: String, path: String) = "${SupabaseConfig.URL}/storage/v1/object/public/$bucket/$path"

    // ------------------------------------------------------------------ Plumbing

    private fun HttpRequestBuilder.base(auth: Boolean = true) {
        header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
        val token = if (auth) session?.accessToken else null
        if (token != null) header("Authorization", "Bearer $token")
    }

    private fun HttpRequestBuilder.jsonBody(text: String) {
        contentType(ContentType.Application.Json)
        setBody(text)
    }

    /** Runs a REST call with a fresh token, retrying once after a 401. */
    private suspend fun rest(block: suspend () -> HttpResponse): Res<String> {
        if (session?.let(::expiresSoon) == true) refresh()
        var r = call(block) ?: return Res.Err(OFFLINE)
        if (r.status.value == 401 && session != null) {
            session = session?.copy(expiresAtSec = 0)
            if (refresh()) r = call(block) ?: return Res.Err(OFFLINE)
        }
        val text = r.bodyAsText()
        return if (r.status.value in 200..299) Res.Ok(text) else Res.Err(restError(text), r.status.value)
    }

    private suspend fun call(block: suspend () -> HttpResponse): HttpResponse? =
        withContext(Dispatchers.IO) { runCatching { block() }.getOrNull() }

    private fun restError(text: String): String = runCatching {
        json.parseToJsonElement(text) as JsonObject
    }.getOrNull()?.let { o ->
        (o["message"] ?: o["error"] ?: o["msg"])?.jsonPrimitive?.content
    }?.let(::friendly) ?: "Something went wrong. Please try again."

    private fun authError(status: Int, text: String): String {
        val o = runCatching { json.parseToJsonElement(text) as JsonObject }.getOrNull()
        val code = o?.get("error_code")?.jsonPrimitive?.content
        val msg = (o?.get("msg") ?: o?.get("error_description") ?: o?.get("message"))?.jsonPrimitive?.content
        return when {
            code == "otp_expired" -> "That code didn't match or has expired."
            code == "over_sms_send_rate_limit" || code == "over_email_send_rate_limit" || status == 429 ->
                "Too many codes requested. Please wait a minute and try again."
            code == "sms_send_failed" -> "We couldn't send the SMS right now. Try email instead."
            msg != null -> msg
            else -> "Sign-in failed. Please try again."
        }
    }

    /** Turns database rule messages into plain sentences; most are already written for people. */
    private fun friendly(m: String) = when {
        m.contains("row-level security", ignoreCase = true) -> "You're not allowed to do that."
        m.contains("duplicate key", ignoreCase = true) -> "Already done."
        else -> m
    }

    companion object {
        const val OFFLINE = "No connection. Check your internet and try again."

        /** JSON string literal. */
        fun q(s: String): String = kotlinx.serialization.json.JsonPrimitive(s).toString()
    }
}
