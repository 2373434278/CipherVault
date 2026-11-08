package com.ciphervault.app.domain

import com.ciphervault.app.data.SettingsDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemFieldSpecTest {
    @Test
    fun cardAndIdentityFieldKeysAreStable() {
        assertEquals(
            listOf("cardholder", "number", "expiry", "cvv"),
            VaultItemType.CARD.extraFields.map { it.key },
        )
        assertEquals(
            listOf("fullName", "email", "phone", "idNumber", "address"),
            VaultItemType.IDENTITY.extraFields.map { it.key },
        )
        assertTrue(VaultItemType.LOGIN.extraFields.isEmpty())
        assertTrue(VaultItemType.NOTE.extraFields.isEmpty())
    }

    @Test
    fun cardFieldsAreMonoAndLabelsArePresent() {
        assertTrue(VaultItemType.CARD.extraFields.all { it.mono })
        assertTrue(VaultItemType.IDENTITY.extraFields.none { it.mono })
        assertTrue((VaultItemType.CARD.extraFields + VaultItemType.IDENTITY.extraFields).all { it.labelRes != 0 })
    }

    @Test
    fun settingsDefaultsAreStable() {
        assertEquals(1, SettingsDefaults.AUTO_LOCK_MINUTES)
        assertEquals(30, SettingsDefaults.CLIPBOARD_CLEAR_SECONDS)
        assertTrue(SettingsDefaults.SCREENSHOT_PROTECTION)
        assertFalse(SettingsDefaults.SCREENSHOT_PROTECTION.not())
    }
}
