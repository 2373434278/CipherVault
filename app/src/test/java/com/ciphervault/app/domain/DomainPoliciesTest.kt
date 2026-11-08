package com.ciphervault.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainPoliciesTest {
    private fun item(
        id: String,
        name: String = id,
        username: String = "",
        url: String = "",
        type: VaultItemType = VaultItemType.LOGIN,
        favorite: Boolean = false,
        folderId: String? = null,
        notes: String = "",
        extra: Map<String, String> = emptyMap(),
    ) = VaultItem(
        id = id,
        type = type,
        name = name,
        username = username,
        url = url,
        notes = notes,
        extra = extra,
        favorite = favorite,
        folderId = folderId,
    )

    @Test
    fun vaultFilterMatchesNameUsernameUrlAndFacets() {
        val items = listOf(
            item("a", name = "GitHub", username = "octocat", url = "https://github.com", favorite = true, folderId = "work"),
            item("b", name = "Bank", type = VaultItemType.CARD, folderId = "personal"),
            item("c", name = "Notes"),
        )

        assertEquals(listOf("a"), VaultFilter.apply(items, VaultFilterState(query = "git")).map { it.id })
        assertEquals(listOf("a"), VaultFilter.apply(items, VaultFilterState(query = "octo")).map { it.id })
        assertEquals(listOf("a"), VaultFilter.apply(items, VaultFilterState(query = "github.com")).map { it.id })
        assertEquals(listOf("b"), VaultFilter.apply(items, VaultFilterState(type = VaultItemType.CARD)).map { it.id })
        assertEquals(listOf("a"), VaultFilter.apply(items, VaultFilterState(favoritesOnly = true)).map { it.id })
        assertEquals(listOf("b"), VaultFilter.apply(items, VaultFilterState(folderId = "personal")).map { it.id })
        assertEquals(emptyList<String>(), VaultFilter.apply(items, VaultFilterState(query = "git", folderId = "personal")).map { it.id })
    }

    @Test
    fun folderCountsCountsActiveItemsOnce() {
        val items = listOf(
            item("a", folderId = "x"),
            item("b", folderId = "x"),
            item("c", folderId = "y"),
            item("d", folderId = null),
        )

        assertEquals(mapOf("x" to 2, "y" to 1), FolderCounts.byFolder(items))
    }

    @Test
    fun autoLockPolicySeparatesNeverImmediateAndDelay() {
        assertEquals(AutoLockPolicy.Mode.NEVER, AutoLockPolicy.mode(-1))
        assertEquals(AutoLockPolicy.Mode.IMMEDIATE, AutoLockPolicy.mode(0))
        assertEquals(AutoLockPolicy.Mode.AFTER_DELAY, AutoLockPolicy.mode(5))

        assertTrue(AutoLockPolicy.shouldLockOnStop(0))
        assertFalse(AutoLockPolicy.shouldLockOnStop(5))
        assertFalse(AutoLockPolicy.shouldLockOnStop(-1))

        assertEquals(300_000L, AutoLockPolicy.delayMillis(5))
        assertNull(AutoLockPolicy.delayMillis(0))
        assertNull(AutoLockPolicy.delayMillis(-1))
    }

    @Test
    fun securityPostureAddsBonuses() {
        assertEquals(60, SecurityPosture.score(biometricEnabled = false, screenshotProtection = false))
        assertEquals(80, SecurityPosture.score(biometricEnabled = true, screenshotProtection = false))
        assertEquals(80, SecurityPosture.score(biometricEnabled = false, screenshotProtection = true))
        assertEquals(100, SecurityPosture.score(biometricEnabled = true, screenshotProtection = true))
    }

    @Test
    fun subtitleUsesDocumentedPriority() {
        assertEquals(
            "octocat",
            VaultItemSubtitle.of(item("a", username = "octocat", url = "https://github.com", notes = "n")),
        )
        assertEquals(
            "https://github.com",
            VaultItemSubtitle.of(item("a", url = "https://github.com", notes = "n")),
        )
        assertEquals(
            "Jane Doe",
            VaultItemSubtitle.of(
                item("a", type = VaultItemType.CARD, extra = mapOf("cardholder" to "Jane Doe")),
            ),
        )
        assertEquals(
            "Jane Doe",
            VaultItemSubtitle.of(
                item("a", type = VaultItemType.IDENTITY, extra = mapOf("fullName" to "Jane Doe")),
            ),
        )
        val longNotes = "n".repeat(100)
        assertEquals(60, VaultItemSubtitle.of(item("a", notes = longNotes)).length)
    }
}
