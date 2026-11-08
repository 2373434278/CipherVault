package com.ciphervault.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportExportTest {
    @Test
    fun csvRoundTripPreservesFields() {
        val folder = Folder("f1", "Work", 0)
        val item = VaultItem(
            id = "1",
            type = VaultItemType.LOGIN,
            name = "GitHub",
            username = "user",
            password = "pass",
            url = "https://github.com",
            notes = "note",
            totpSecret = "JBSWY3DPEHPK3PXP",
            folderId = "f1",
            favorite = true,
            pinned = true,
        )
        val csv = VaultExporter.exportCsv(listOf(item), listOf(folder))
        val imported = VaultImporter.importCsv(csv)
        assertEquals(1, imported.items.size)
        val result = imported.items.first()
        assertEquals("GitHub", result.name)
        assertEquals("user", result.username)
        assertEquals("pass", result.password)
        assertEquals("https://github.com", result.url)
        assertEquals("JBSWY3DPEHPK3PXP", result.totpSecret)
        assertTrue(result.favorite)
        assertTrue(result.pinned)
        assertEquals("Work", imported.folders.first().name)
        assertEquals(imported.folders.first().id, result.folderId)
    }

    @Test
    fun bitwardenJsonImportMapsLoginCardAndIdentity() {
        val json = """
            {
              "folders": [{"id":"folder-1","name":"Work"}],
              "items": [
                {
                  "type": 1,
                  "name": "GitHub",
                  "notes": "2FA",
                  "favorite": true,
                  "folderId": "folder-1",
                  "login": {
                    "username": "user",
                    "password": "pass",
                    "totp": "otpauth://totp/GitHub?secret=JBSWY3DPEHPK3PXP&algorithm=SHA256&digits=8&period=60",
                    "uris": [{"uri":"https://github.com"}]
                  }
                },
                {
                  "type": 3,
                  "name": "Visa",
                  "card": {"cardholderName":"Jiang","number":"4111111111111111","expMonth":"12","expYear":"2030","code":"123"}
                },
                {
                  "type": 4,
                  "name": "Identity",
                  "identity": {"firstName":"Jiang","lastName":"Rong","email":"j@example.com","phone":"123","address1":"Road 1","city":"Shanghai","postalCode":"200000","country":"CN"}
                }
              ]
            }
        """.trimIndent()
        val imported = VaultImporter.importBitwarden(json)
        assertEquals(3, imported.items.size)
        assertEquals(1, imported.folders.size)
        val login = imported.items.first { it.name == "GitHub" }
        assertEquals(VaultItemType.LOGIN, login.type)
        assertEquals("user", login.username)
        assertEquals("SHA256", login.totpAlgorithm)
        assertEquals(8, login.totpDigits)
        assertEquals(60, login.totpPeriod)
        assertEquals(imported.folders.first().id, login.folderId)
        val card = imported.items.first { it.name == "Visa" }
        assertEquals("4111111111111111", card.extra["number"])
        val identity = imported.items.first { it.name == "Identity" }
        assertEquals("Jiang Rong", identity.extra["fullName"])
        assertTrue(identity.extra["address"].orEmpty().contains("Shanghai"))
    }

    @Test
    fun payloadJsonPreservesPinned() {
        val item = VaultItem(
            id = "p1",
            type = VaultItemType.LOGIN,
            name = "Pinned",
            pinned = true,
        )

        val json = VaultExporter.payloadToJson(VaultPayload(listOf(item), emptyList()))
        val decoded = VaultExporter.payloadFromJson(json)

        assertTrue(decoded.items.first().pinned)
    }

    @Test
    fun payloadWithoutPinnedDefaultsToFalse() {
        val json = """
            {
              "format": "ciphervault-payload",
              "version": 1,
              "folders": [],
              "items": [{"id":"1","type":"login","name":"Old"}]
            }
        """.trimIndent()

        val decoded = VaultExporter.payloadFromJson(json)

        assertTrue(!decoded.items.first().pinned)
    }

    @Test
    fun entityMappingPreservesPinned() {
        val item = VaultItem(
            id = "p1",
            type = VaultItemType.LOGIN,
            name = "Pinned",
            pinned = true,
        )

        assertTrue(item.toEntity().pinned)
        assertTrue(item.toEntity().toDomain().pinned)
    }
}
