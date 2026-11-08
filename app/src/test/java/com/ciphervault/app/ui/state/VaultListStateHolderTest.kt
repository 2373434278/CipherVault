package com.ciphervault.app.ui.state

import com.ciphervault.app.data.VaultRepository
import com.ciphervault.app.domain.VaultItem
import com.ciphervault.app.domain.VaultItemType
import com.ciphervault.app.session.FakeSettings
import com.ciphervault.app.session.FakeVaultDao
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultListStateHolderTest {
    private fun holder() = VaultListStateHolder(
        repository = VaultRepository(FakeVaultDao()),
        settings = FakeSettings(),
        scope = TestScope(),
    )

    private fun item(
        id: String,
        type: VaultItemType = VaultItemType.LOGIN,
        favorite: Boolean = false,
        folderId: String? = null,
    ) = VaultItem(id = id, type = type, name = id, favorite = favorite, folderId = folderId)

    @Test
    fun filterFacetsAreMutuallyExclusiveForTypeAndFavorites() {
        val state = holder()

        state.selectFavorites()
        assertTrue(state.favoritesOnly)
        assertNull(state.selectedType)

        state.selectType(VaultItemType.CARD)
        assertFalse(state.favoritesOnly)
        assertEquals(VaultItemType.CARD, state.selectedType)

        state.selectAll()
        assertFalse(state.favoritesOnly)
        assertNull(state.selectedType)
    }

    @Test
    fun folderSelectionIsIndependent() {
        val state = holder()

        state.selectFolder("work")
        assertEquals("work", state.selectedFolderId)
        state.selectAll()
        assertEquals("work", state.selectedFolderId)
        state.selectFolder(null)
        assertNull(state.selectedFolderId)
    }

    @Test
    fun visibleItemsAppliesFilter() {
        val state = holder()
        val items = listOf(
            item("login", VaultItemType.LOGIN, favorite = true, folderId = "work"),
            item("card", VaultItemType.CARD, folderId = "personal"),
        )

        state.selectType(VaultItemType.LOGIN)
        assertEquals(listOf("login"), state.visibleItems(items).map { it.id })

        state.selectAll()
        state.selectFolder("personal")
        assertEquals(listOf("card"), state.visibleItems(items).map { it.id })
    }
}
