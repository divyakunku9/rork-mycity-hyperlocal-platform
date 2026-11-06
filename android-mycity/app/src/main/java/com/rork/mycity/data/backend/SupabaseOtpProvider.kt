package com.rork.mycity.data.backend

import com.rork.mycity.data.OtpChannel
import com.rork.mycity.data.OtpProvider
import com.rork.mycity.data.OtpSendResult
import com.rork.mycity.data.OtpTarget

/**
 * Real one-time codes through Supabase Auth: SMS to +91 numbers, or email.
 * The sign-in screens use it exactly like the old demo provider.
 */
class SupabaseOtpProvider(private val sb: Supabase) : OtpProvider {
    override val isDemo = false

    /** Set after a failed [verify] so the screen can say why (wrong code vs no connection). */
    var lastError: String? = null
        private set

    override suspend fun send(target: OtpTarget): OtpSendResult =
        when (val r = sb.sendOtp(phone = target.phoneE164(), email = target.emailOrNull())) {
            is Res.Ok -> OtpSendResult.Sent()
            is Res.Err -> OtpSendResult.Failed(r.message)
        }

    override suspend fun verify(target: OtpTarget, code: String): Boolean =
        when (val r = sb.verifyOtp(code, phone = target.phoneE164(), email = target.emailOrNull())) {
            is Res.Ok -> { lastError = null; true }
            is Res.Err -> { lastError = r.message; false }
        }

    private fun OtpTarget.phoneE164() = if (channel == OtpChannel.PHONE) "+91" + value.filter { it.isDigit() }.takeLast(10) else null
    private fun OtpTarget.emailOrNull() = if (channel == OtpChannel.EMAIL) value else null
}
