package com.liquidfuran.furan.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

object HmacUtil {
    private const val ALGORITHM = "HmacSHA256"
    private const val VALID_WINDOW_SECONDS = 300L  // 5 minutes

    fun computeHmac(data: String, secret: String): String {
        val mac = Mac.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), ALGORITHM)
        mac.init(keySpec)
        val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies an approval message of the form "APPROVE:{timestamp}:{hmac}".
     * Returns true iff the HMAC matches and the timestamp is within the valid window.
     */
    fun verifyApproval(message: String, secret: String): Boolean {
        val parts = message.split(":")
        if (parts.size != 3 || parts[0] != "APPROVE") return false
        val timestamp = parts[1].toLongOrNull() ?: return false
        if (abs(System.currentTimeMillis() / 1000 - timestamp) > VALID_WINDOW_SECONDS) return false
        val expected = computeHmac("${parts[0]}:${parts[1]}", secret)
        return expected == parts[2]
    }

    /**
     * Generates a cryptographically random 32-byte secret encoded as hex.
     */
    fun generateSecret(): String {
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
