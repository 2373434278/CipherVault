package com.ciphervault.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpTest {
    private val secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

    @Test
    fun rfc6238Sha1Vectors() {
        assertEquals("94287082", code(59, "SHA1"))
        assertEquals("07081804", code(1111111109, "SHA1"))
        assertEquals("14050471", code(1111111111, "SHA1"))
        assertEquals("89005924", code(1234567890, "SHA1"))
        assertEquals("69279037", code(2000000000, "SHA1"))
        assertEquals("65353130", code(20000000000, "SHA1"))
    }

    @Test
    fun rfc6238Sha256Vectors() {
        assertEquals("46119246", code(59, "SHA256"))
        assertEquals("68084774", code(1111111109, "SHA256"))
        assertEquals("91819424", code(1234567890, "SHA256"))
    }

    @Test
    fun rfc6238Sha512Vectors() {
        assertEquals("90693936", code(59, "SHA512"))
        assertEquals("25091201", code(1111111109, "SHA512"))
        assertEquals("93441116", code(1234567890, "SHA512"))
    }

    @Test
    fun countdownIsWithinPeriod() {
        val result = TotpGenerator.generate(secret, timestampMillis = 59_000)
        assertEquals(1, result.remainingSeconds)
        assertTrue(result.progress in 0f..1f)
    }

    @Test
    fun parsesOtpauthUri() {
        val parsed = OtpauthParser.parse(
            "otpauth://totp/GitHub:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub&algorithm=SHA256&digits=8&period=60",
        )
        assertEquals("GitHub", parsed?.issuer)
        assertEquals("user@example.com", parsed?.account)
        assertEquals("JBSWY3DPEHPK3PXP", parsed?.secret)
        assertEquals("SHA256", parsed?.algorithm)
        assertEquals(8, parsed?.digits)
        assertEquals(60, parsed?.period)
    }

    @Test
    fun rejectsInvalidOtpauthUri() {
        assertNull(OtpauthParser.parse("https://example.com"))
        assertNull(OtpauthParser.parse("otpauth://totp/NoSecret?issuer=Test"))
        assertNull(OtpauthParser.parse("otpauth://hotp/Test?secret=JBSWY3DPEHPK3PXP"))
    }

    @Test
    fun decodesBase32CaseInsensitive() {
        val spaces = Base32.decode("jbsw y3dp ehpk 3pxp")
        assertTrue(spaces.isNotEmpty())
        assertEquals(Base32.decode("JBSWY3DPEHPK3PXP").toList(), spaces.toList())
    }

    private fun code(epochSeconds: Long, algorithm: String): String =
        TotpGenerator.generate(
            secret = secretFor(algorithm),
            algorithm = algorithm,
            digits = 8,
            period = 30,
            timestampMillis = epochSeconds * 1000,
        ).code

    private fun secretFor(algorithm: String): String = when (algorithm) {
        "SHA256" -> "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA===="
        "SHA512" -> "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNA="
        else -> secret
    }
}
