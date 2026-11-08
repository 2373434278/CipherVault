package com.ciphervault.app.domain

import com.ciphervault.app.domain.importexport.BackupCodec
import com.ciphervault.app.domain.importexport.EncryptedBackupRequiresPasswordException
import com.ciphervault.app.domain.importexport.PayloadJsonCodec
import com.ciphervault.app.domain.importexport.VaultFormats
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportExportFormatsTest {
    private fun item(id: String = "i1", folderId: String? = null) = VaultItem(
        id = id,
        type = VaultItemType.LOGIN,
        name = "GitHub",
        username = "user",
        password = "pass",
        url = "https://github.com",
        folderId = folderId,
        createdAt = 100,
        updatedAt = 200,
    )

    @Test
    fun encryptedBackupIsDetectedBeforeBitwardenJson() {
        val backup = BackupCodec.create(
            VaultPayload(listOf(item()), emptyList()),
            "backup-password".toCharArray(),
        )

        assertEquals(VaultFormats.encryptedBackup, VaultFormats.detect(backup))
        assertTrue(VaultFormats.isEncryptedBackup(backup))
    }

    @Test
    fun autoImportRequiresPasswordForBackupAndRoundTripsWithIt() {
        val backup = BackupCodec.create(
            VaultPayload(listOf(item()), emptyList()),
            "backup-password".toCharArray(),
        )

        val missingPassword = runCatching { VaultImporter.importAuto(backup) }.exceptionOrNull()
        assertTrue(missingPassword is EncryptedBackupRequiresPasswordException)

        val decoded = VaultFormats.importAuto(backup, "backup-password".toCharArray())
        assertEquals(1, decoded.items.size)
        assertEquals("GitHub", decoded.items.first().name)
    }

    @Test
    fun detectsBitwardenJsonAndCsv() {
        assertEquals(VaultFormats.bitwardenJson, VaultFormats.detect("""{"items":[]}"""))
        assertEquals(VaultFormats.csv, VaultFormats.detect("name,url\r\nGitHub,https://github.com"))
    }

    @Test
    fun emptyInputIsRejected() {
        val failure = runCatching { VaultFormats.detect("   ") }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun unknownBitwardenTypesAreNotEnriched() {
        val json = """
            {
              "items": [
                {"type": 5, "name": "Unknown", "login": {"username": "should-not-copy"}},
                {"type": 2, "name": "Note", "login": {"username": "should-not-copy"}}
              ]
            }
        """.trimIndent()

        val payload = VaultFormats.importAuto(json)

        assertTrue(payload.items.all { it.username.isEmpty() })
    }

    @Test
    fun payloadJsonKeepsNullFolderIdPlatformIndependent() {
        val encoded = PayloadJsonCodec.encode(VaultPayload(listOf(item(folderId = null)), emptyList()))
        val json = JSONObject(encoded).getJSONArray("items").getJSONObject(0)
        assertTrue(json.isNull("folderId"))

        assertNull(PayloadJsonCodec.decode(encoded).items.first().folderId)
        assertNull(
            PayloadJsonCodec.decode(
                """{"format":"ciphervault-payload","version":1,"folders":[],"items":[{"id":"1","type":"login","name":"Old","folderId":null}]}""",
            ).items.first().folderId,
        )
    }

    @Test
    fun backupRejectsShortPasswordAndWrongPassword() {
        val short = runCatching {
            BackupCodec.create(VaultPayload(emptyList(), emptyList()), "short".toCharArray())
        }.exceptionOrNull()
        assertTrue(short is IllegalArgumentException)

        val backup = BackupCodec.create(VaultPayload(listOf(item()), emptyList()), "backup-password".toCharArray())
        val wrong = runCatching { BackupCodec.open(backup, "wrong-password".toCharArray()) }.exceptionOrNull()
        assertTrue(wrong != null)
    }
}
