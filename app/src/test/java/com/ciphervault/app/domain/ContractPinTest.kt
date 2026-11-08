package com.ciphervault.app.domain

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 线格式金测试：这些字符串一旦变化，历史数据、导入导出和备份兼容性都会被破坏。
 */
class ContractPinTest {
    private fun loginItem(): VaultItem = VaultItem(
        id = "item-1",
        type = VaultItemType.LOGIN,
        name = "GitHub",
        username = "user",
        password = "pass",
        url = "https://github.com",
        notes = "note",
        totpSecret = "JBSWY3DPEHPK3PXP",
        extra = mapOf("number" to "4242"),
        folderId = "folder-1",
        favorite = true,
        pinned = true,
        createdAt = 1000L,
        updatedAt = 2000L,
    )

    @Test
    fun csvWireFormatIsPinned() {
        val csv = VaultExporter.exportCsv(
            items = listOf(loginItem()),
            folders = listOf(Folder("folder-1", "Work", 0)),
        )

        val expected = listOf(
            "name,type,folder,url,username,password,notes,totp,extra_json,favorite,pinned",
            "GitHub,login,Work,https://github.com,user,pass,note," +
                "otpauth://totp/GitHub?secret=JBSWY3DPEHPK3PXP&algorithm=SHA1&digits=6&period=30," +
                "\"{\"\"number\"\":\"\"4242\"\"}\",1,1",
        ).joinToString("\r\n")

        assertEquals(expected, csv)
        assertTrue(csv.contains("\r\n"))
        assertFalse(csv.replace("\r\n", "").contains("\n"))
    }

    @Test
    fun otpauthUriIsPinned() {
        val csv = VaultExporter.exportCsv(listOf(loginItem()), emptyList())
        val uri = csv.lineSequence().drop(1).first().split(',')[7]

        assertEquals(
            "otpauth://totp/GitHub?secret=JBSWY3DPEHPK3PXP&algorithm=SHA1&digits=6&period=30",
            uri,
        )
    }

    @Test
    fun payloadJsonKeysArePinned() {
        val json = JSONObject(VaultExporter.payloadToJson(VaultPayload(listOf(loginItem()), emptyList())))

        assertEquals("ciphervault-payload", json.getString("format"))
        assertEquals(1, json.getInt("version"))
        assertTrue(json.has("folders"))
        val item = json.getJSONArray("items").getJSONObject(0)
        assertEquals(
            setOf(
                "id", "type", "name", "username", "password", "url", "notes",
                "totpSecret", "totpAlgorithm", "totpDigits", "totpPeriod", "extra",
                "folderId", "favorite", "pinned", "createdAt", "updatedAt",
            ),
            item.keys().asSequence().toSet(),
        )
    }

    @Test
    fun backupEnvelopeKeysAndVersionArePinned() {
        val password = "backup-password".toCharArray()
        val text = VaultExporter.createEncryptedBackup(
            VaultPayload(listOf(loginItem()), emptyList()),
            password,
        )
        val root = JSONObject(text)

        assertEquals("ciphervault-backup", root.getString("format"))
        assertEquals(1, root.getInt("version"))
        assertTrue(root.has("createdAt"))
        assertTrue(root.getString("data").isNotBlank())
        val kdf = root.getJSONObject("kdf")
        assertEquals(setOf("id", "salt", "t", "m", "p"), kdf.keys().asSequence().toSet())
    }

    @Test
    fun sortOrderIsPinned() {
        val items = listOf(
            VaultItem(id = "a", type = VaultItemType.LOGIN, name = "Alpha", pinned = false, createdAt = 100, updatedAt = 100),
            VaultItem(id = "b", type = VaultItemType.LOGIN, name = "bravo", pinned = true, createdAt = 300, updatedAt = 300),
            VaultItem(id = "c", type = VaultItemType.LOGIN, name = "Charlie", pinned = true, createdAt = 200, updatedAt = 200),
        )

        assertEquals(listOf("b", "c", "a"), sortVaultItems(items, VaultSort.PINNED).map { it.id })
        assertEquals(listOf("b", "c", "a"), sortVaultItems(items, VaultSort.UPDATED).map { it.id })
        assertEquals(listOf("b", "c", "a"), sortVaultItems(items, VaultSort.CREATED).map { it.id })
        assertEquals(listOf("a", "b", "c"), sortVaultItems(items, VaultSort.NAME).map { it.id })
    }
}
