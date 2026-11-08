package com.ciphervault.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class VaultSortTest {
    private fun item(
        id: String,
        name: String,
        pinned: Boolean = false,
        updatedAt: Long = 0L,
        createdAt: Long = 0L,
    ) = VaultItem(
        id = id,
        type = VaultItemType.LOGIN,
        name = name,
        pinned = pinned,
        updatedAt = updatedAt,
        createdAt = createdAt,
    )

    @Test
    fun pinnedSortPutsPinnedFirst() {
        val items = listOf(
            item("a", "Alpha", pinned = false, updatedAt = 200L),
            item("b", "Beta", pinned = true, updatedAt = 100L),
        )

        val sorted = sortVaultItems(items, VaultSort.PINNED)

        assertEquals(listOf("b", "a"), sorted.map { it.id })
    }

    @Test
    fun updatedSortPutsNewestFirst() {
        val items = listOf(
            item("a", "Alpha", updatedAt = 100L),
            item("c", "Charlie", updatedAt = 300L),
            item("b", "Bravo", updatedAt = 200L),
        )

        val sorted = sortVaultItems(items, VaultSort.UPDATED)

        assertEquals(listOf("c", "b", "a"), sorted.map { it.id })
    }

    @Test
    fun createdSortPutsNewestFirst() {
        val items = listOf(
            item("a", "Alpha", createdAt = 100L),
            item("c", "Charlie", createdAt = 300L),
            item("b", "Bravo", createdAt = 200L),
        )

        val sorted = sortVaultItems(items, VaultSort.CREATED)

        assertEquals(listOf("c", "b", "a"), sorted.map { it.id })
    }

    @Test
    fun nameSortIsCaseInsensitive() {
        val items = listOf(
            item("b", "beta"),
            item("a", "Alpha"),
            item("g", "gamma"),
        )

        val sorted = sortVaultItems(items, VaultSort.NAME)

        assertEquals(listOf("a", "b", "g"), sorted.map { it.id })
    }

    @Test
    fun nameSortBreaksTiesByUpdatedAt() {
        val items = listOf(
            item("old", "Same", updatedAt = 100L),
            item("new", "same", updatedAt = 200L),
        )

        val sorted = sortVaultItems(items, VaultSort.NAME)

        assertEquals(listOf("new", "old"), sorted.map { it.id })
    }

    @Test
    fun pinnedSortBreaksTiesByUpdatedAt() {
        val items = listOf(
            item("old", "Alpha", pinned = true, updatedAt = 100L),
            item("new", "Bravo", pinned = true, updatedAt = 200L),
        )

        val sorted = sortVaultItems(items, VaultSort.PINNED)

        assertEquals(listOf("new", "old"), sorted.map { it.id })
    }

    @Test
    fun fromLegacyFavoriteFallsBackToPinned() {
        assertEquals(VaultSort.PINNED, VaultSort.from(null))
        assertEquals(VaultSort.PINNED, VaultSort.from("unknown"))
        assertEquals(VaultSort.PINNED, VaultSort.from("favorite"))
        assertEquals(VaultSort.NAME, VaultSort.from("name"))
    }
}
