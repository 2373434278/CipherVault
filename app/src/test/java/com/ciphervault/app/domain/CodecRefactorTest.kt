package com.ciphervault.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodecRefactorTest {
    @Test
    fun extraCodecRoundTripsAndFiltersBlankValues() {
        val encoded = ExtraCodec.encode(mapOf("number" to "4242", "blank" to ""))
        assertEquals(mapOf("number" to "4242"), ExtraCodec.decode(encoded))
        assertNull(ExtraCodec.encode(emptyMap()))
        assertTrue(ExtraCodec.decode("not-json").isEmpty())
    }

    @Test
    fun extraCodecCsvAndBackupSemanticsDifferOnlyOnBlankFiltering() {
        assertEquals("", ExtraCodec.toCsv(emptyMap()))
        assertEquals(mapOf("x" to "y"), ExtraCodec.fromCsv("""{"x":"y"}"""))
        assertTrue(ExtraCodec.fromCsv("").isEmpty())
        assertTrue(ExtraCodec.fromCsv("{broken").isEmpty())
        assertEquals("", ExtraCodec.intoJson(mapOf("blank" to "")).getString("blank"))
    }

    @Test
    fun totpSettingsParsesUriBareSecretAndDefaults() {
        assertEquals(
            TotpSettings(),
            TotpSettings.parse(""),
        )
        assertEquals(
            TotpSettings("JBSWY3DPEHPK3PXP", "SHA256", 8, 60),
            TotpSettings.parse(
                "otpauth://totp/Test?secret=JBSWY3DPEHPK3PXP&algorithm=SHA256&digits=8&period=60",
            ),
        )
        assertEquals(
            TotpSettings(secret = "JBSWY3DPEHPK3PXP"),
            TotpSettings.parse("JBSWY3DPEHPK3PXP"),
        )
    }

    @Test
    fun otpauthUriIsReversibleAndPinned() {
        val uri = OtpauthUri.build("GitHub", TotpSettings("JBSWY3DPEHPK3PXP"))

        assertEquals(
            "otpauth://totp/GitHub?secret=JBSWY3DPEHPK3PXP&algorithm=SHA1&digits=6&period=30",
            uri,
        )
        assertEquals("JBSWY3DPEHPK3PXP", OtpauthParser.parse(uri)?.secret)
        assertEquals(
            "My+App",
            OtpauthUri.build("My App", TotpSettings("JBSWY3DPEHPK3PXP")).substringAfter("totp/").substringBefore("?"),
        )
    }

    @Test
    fun csvRowSpecResolvesHeaderAliasesAndPositionalLayout() {
        val header = listOf(
            "title", "login_uri", "login_username", "login_password", "notes",
            "login_totp", "folder", "type", "favorite", "pinned", "extra_json",
        )
        assertTrue(CsvRowSpec.isHeader(header))

        val withHeader = CsvRowSpec.layout(listOf(header, listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k")))
        assertEquals(1, withHeader.dataRows.size)
        assertEquals(0, withHeader.columns.name)
        assertEquals(1, withHeader.columns.url)
        assertEquals(2, withHeader.columns.username)
        assertEquals(3, withHeader.columns.password)
        assertEquals(5, withHeader.columns.totp)
        assertEquals(10, withHeader.columns.extra)

        val withoutHeader = CsvRowSpec.layout(
            listOf(listOf("GitHub", "https://github.com", "user", "pass", "notes", "JBSWY3DPEHPK3PXP")),
        )
        assertEquals(1, withoutHeader.dataRows.size)
        assertEquals(0, withoutHeader.columns.name)
        assertEquals(1, withoutHeader.columns.url)
        assertEquals(5, withoutHeader.columns.totp)
        assertEquals(-1, withoutHeader.columns.folder)
        assertEquals(-1, withoutHeader.columns.type)
    }
}
