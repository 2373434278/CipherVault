package com.ciphervault.app.ui.state

import com.ciphervault.app.domain.ItemFieldKeys
import com.ciphervault.app.domain.VaultItem
import com.ciphervault.app.domain.VaultItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemDraftTest {
    @Test
    fun roundTripPreservesAllFieldsIncludingPinnedAndCreatedAt() {
        val item = VaultItem(
            id = "old",
            type = VaultItemType.CARD,
            name = "Visa",
            username = "user",
            password = "pass",
            url = "https://bank.example",
            notes = "note",
            totpSecret = "secret",
            totpAlgorithm = "SHA256",
            totpDigits = 8,
            totpPeriod = 60,
            extra = mapOf(ItemFieldKeys.CARDHOLDER to "Jane", ItemFieldKeys.CVV to "123"),
            folderId = "work",
            favorite = true,
            pinned = true,
            createdAt = 1_000L,
            updatedAt = 2_000L,
        )

        val copy = ItemDraft.from(item).toItem(id = "new", now = 9_999L)

        assertEquals("new", copy.id)
        assertEquals(item.type, copy.type)
        assertEquals(item.name, copy.name)
        assertEquals(item.extra, copy.extra)
        assertEquals(item.folderId, copy.folderId)
        assertTrue(copy.favorite)
        assertTrue(copy.pinned)
        assertEquals(1_000L, copy.createdAt)
        assertEquals(9_999L, copy.updatedAt)
        assertEquals("SHA256", copy.totpAlgorithm)
        assertEquals(8, copy.totpDigits)
        assertEquals(60, copy.totpPeriod)
    }

    @Test
    fun newDraftUsesNowAndTrimsSecret() {
        val draft = ItemDraft(
            name = "New",
            totp = ItemDraft().totp.copy(secret = "  ABC  "),
        )

        val item = draft.toItem(id = "id", now = 123L)

        assertEquals(123L, item.createdAt)
        assertEquals("ABC", item.totpSecret)
        assertEquals(123L, item.updatedAt)
    }
}
