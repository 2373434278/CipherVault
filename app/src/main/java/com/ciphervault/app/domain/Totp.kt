package com.ciphervault.app.domain

import java.net.URLDecoder
import java.net.URI
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** 一次 TOTP 出码结果；`progress` 用于画倒计时环。 */
data class TotpCode(
    val code: String,
    val remainingSeconds: Int,
    val period: Int,
) {
    val progress: Float get() = remainingSeconds.toFloat() / period.coerceAtLeast(1)
}

/** Base32 解码；大小写、空格、连字符与尾部 `=` 均忽略。 */
object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun decode(input: String): ByteArray {
        val clean = input.trim().replace(" ", "").replace("-", "").trimEnd('=').uppercase()
        require(clean.isNotEmpty()) { "Secret is empty" }
        val output = java.io.ByteArrayOutputStream()
        var buffer = 0
        var bits = 0
        for (char in clean) {
            val value = ALPHABET.indexOf(char)
            require(value >= 0) { "Invalid base32 character: $char" }
            buffer = (buffer shl 5) or value
            bits += 5
            if (bits >= 8) {
                bits -= 8
                output.write((buffer shr bits) and 0xff)
            }
        }
        return output.toByteArray()
    }
}

/**
 * RFC 6238 TOTP 生成器。
 *
 * 位数钳制到 4..8，周期钳制到 1..300；剩余秒数在 1..period 之间。
 */
object TotpGenerator {
    fun generate(
        secret: String,
        algorithm: String = "SHA1",
        digits: Int = 6,
        period: Int = 30,
        timestampMillis: Long = System.currentTimeMillis(),
    ): TotpCode {
        val epochSeconds = timestampMillis / 1000
        val safePeriod = period.coerceIn(1, 300)
        val safeDigits = digits.coerceIn(4, 8)
        val counter = epochSeconds / safePeriod
        val mac = Mac.getInstance(macAlgorithm(algorithm))
        mac.init(SecretKeySpec(Base32.decode(secret), "RAW"))
        val hash = mac.doFinal(counter.toBigEndianBytes())
        val offset = hash.last().toInt() and 0x0f
        val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
            ((hash[offset + 1].toInt() and 0xff) shl 16) or
            ((hash[offset + 2].toInt() and 0xff) shl 8) or
            (hash[offset + 3].toInt() and 0xff)
        val modulo = POWERS_OF_TEN[safeDigits]
        val code = (binary % modulo).toString().padStart(safeDigits, '0')
        val remaining = (safePeriod - (epochSeconds % safePeriod)).toInt().let {
            if (it == 0) safePeriod else it
        }
        return TotpCode(code, remaining, safePeriod)
    }

    private fun macAlgorithm(algorithm: String): String = when (algorithm.uppercase().replace("-", "")) {
        "SHA256" -> "HmacSHA256"
        "SHA512" -> "HmacSHA512"
        else -> "HmacSHA1"
    }

    private fun Long.toBigEndianBytes(): ByteArray = ByteArray(8) { index ->
        (this shr (56 - 8 * index)).toByte()
    }

    private val POWERS_OF_TEN = intArrayOf(1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000, 1_000_000_000)
}

/** `otpauth://` 解析结果。 */
data class OtpauthData(
    val issuer: String,
    val account: String,
    val secret: String,
    val algorithm: String,
    val digits: Int,
    val period: Int,
)

/** `otpauth://` URI 解析器；非法 URI 或无效 Base32 密钥返回 null。 */
object OtpauthParser {
    fun parse(input: String): OtpauthData? {
        val trimmed = input.trim()
        if (!trimmed.startsWith("otpauth://", ignoreCase = true)) return null
        return runCatching {
            val uri = URI(trimmed)
            if (!uri.host.equals("totp", ignoreCase = true)) return null
            val params = parseQuery(uri.rawQuery)
            val secret = params["secret"]?.trim().orEmpty()
            if (secret.isBlank()) return null
            Base32.decode(secret)
            val label = URLDecoder.decode(uri.rawPath.removePrefix("/"), Charsets.UTF_8.name())
            val labelIssuer = label.substringBefore(":", "").takeIf { label.contains(":") }
            val account = label.substringAfter(":", label).trim()
            OtpauthData(
                issuer = params["issuer"]?.trim().takeIf { !it.isNullOrBlank() } ?: labelIssuer.orEmpty(),
                account = account,
                secret = secret,
                algorithm = params["algorithm"]?.uppercase()?.takeIf { it in setOf("SHA1", "SHA256", "SHA512") } ?: "SHA1",
                digits = params["digits"]?.toIntOrNull()?.coerceIn(4, 8) ?: 6,
                period = params["period"]?.toIntOrNull()?.coerceIn(1, 300) ?: 30,
            )
        }.getOrNull()
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split('&').mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) return@mapNotNull null
            val key = URLDecoder.decode(part.substring(0, index), Charsets.UTF_8.name())
            val value = URLDecoder.decode(part.substring(index + 1), Charsets.UTF_8.name())
            key to value
        }.toMap()
    }
}
