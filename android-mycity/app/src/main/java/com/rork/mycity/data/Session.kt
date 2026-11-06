package com.rork.mycity.data

import android.content.Context
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class OtpChannel { PHONE, EMAIL }

data class OtpTarget(val channel: OtpChannel, val value: String) {
    /** "+91 98765 43210" or the email as typed. */
    val display: String
        get() = if (channel == OtpChannel.PHONE) "+91 ${value.take(5)} ${value.drop(5)}" else value
}

sealed interface OtpSendResult {
    /** Code sent. [demoCode] is only set by the demo provider so the UI can show it. */
    data class Sent(val demoCode: String? = null) : OtpSendResult
    data class Failed(val message: String) : OtpSendResult
}

/**
 * Sends and checks one-time codes. Swap [DemoOtpProvider] for an SMS (Twilio / MSG91)
 * and email backed provider once the keys are available; the screens stay the same.
 */
interface OtpProvider {
    val isDemo: Boolean
    suspend fun send(target: OtpTarget): OtpSendResult
    suspend fun verify(target: OtpTarget, code: String): Boolean
}

/** Generates codes on the device and shows them in-app. No SMS or email leaves the phone. */
class DemoOtpProvider : OtpProvider {
    override val isDemo = true
    private val issued = HashMap<String, Pair<String, Long>>()

    override suspend fun send(target: OtpTarget): OtpSendResult {
        delay(900)
        val code = (0 until 6).joinToString("") { Random.nextInt(10).toString() }
        issued[target.value] = code to System.currentTimeMillis()
        return OtpSendResult.Sent(demoCode = code)
    }

    override suspend fun verify(target: OtpTarget, code: String): Boolean {
        delay(700)
        val (expected, at) = issued[target.value] ?: return false
        val fresh = System.currentTimeMillis() - at < 10 * 60_000
        return fresh && expected == code
    }
}

data class UserProfile(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val ward: String = "",
    val photoUri: String? = null,
) {
    val initials: String
        get() {
            val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            return when {
                parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
                parts.size == 1 -> parts[0].take(2).uppercase()
                email.isNotBlank() -> email.take(2).uppercase()
                else -> "MC"
            }
        }

    val firstName: String get() = name.trim().split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "there"
}

/** Small persistent store for session, profile, city and notification settings. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("mycity", Context.MODE_PRIVATE)

    var signedIn: Boolean
        get() = sp.getBoolean("signed_in", false)
        set(v) = sp.edit().putBoolean("signed_in", v).apply()

    var cityId: String?
        get() = sp.getString("city", null)
        set(v) = sp.edit().putString("city", v).apply()

    var profile: UserProfile
        get() = UserProfile(
            name = sp.getString("p_name", "") ?: "",
            phone = sp.getString("p_phone", "") ?: "",
            email = sp.getString("p_email", "") ?: "",
            ward = sp.getString("p_ward", "") ?: "",
            photoUri = sp.getString("p_photo", null),
        )
        set(p) = sp.edit()
            .putString("p_name", p.name).putString("p_phone", p.phone).putString("p_email", p.email)
            .putString("p_ward", p.ward).putString("p_photo", p.photoUri).apply()

    fun flag(key: String, default: Boolean = true) = sp.getBoolean("n_$key", default)
    fun setFlag(key: String, v: Boolean) = sp.edit().putBoolean("n_$key", v).apply()

    fun clearSession() {
        sp.edit().remove("signed_in").remove("city").apply()
    }
}
