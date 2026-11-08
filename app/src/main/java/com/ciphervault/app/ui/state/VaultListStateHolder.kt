package com.ciphervault.app.ui.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ciphervault.app.core.port.SettingsGateway
import com.ciphervault.app.data.VaultRepository
import com.ciphervault.app.domain.VaultFilter
import com.ciphervault.app.domain.VaultFilterState
import com.ciphervault.app.domain.VaultItem
import com.ciphervault.app.domain.VaultItemType
import com.ciphervault.app.domain.VaultSort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 保险库列表状态持有者。
 *
 * 持有筛选/弹层状态与列表意图；数据流仍由页面从 repository 收集。
 * 生命周期由 `remember(repository)` 绑定到已解锁会话，锁定后随导航树一起销毁。
 */
@Stable
class VaultListStateHolder(
    private val repository: VaultRepository,
    private val settings: SettingsGateway,
    private val scope: CoroutineScope,
) {
    var query by mutableStateOf("")
    var selectedType by mutableStateOf<VaultItemType?>(null)
    var favoritesOnly by mutableStateOf(false)
    var selectedFolderId by mutableStateOf<String?>(null)
    var showSort by mutableStateOf(false)
    var actionItem by mutableStateOf<VaultItem?>(null)
    var deleteItem by mutableStateOf<VaultItem?>(null)

    val filter: VaultFilterState
        get() = VaultFilterState(
            query = query,
            type = selectedType,
            favoritesOnly = favoritesOnly,
            folderId = selectedFolderId,
        )

    fun visibleItems(items: List<VaultItem>): List<VaultItem> = VaultFilter.apply(items, filter)

    fun selectAll() {
        selectedType = null
        favoritesOnly = false
    }

    fun selectFavorites() {
        favoritesOnly = true
        selectedType = null
    }

    fun selectType(type: VaultItemType) {
        selectedType = type
        favoritesOnly = false
    }

    fun selectFolder(id: String?) {
        selectedFolderId = id
    }

    fun setSort(mode: VaultSort) {
        showSort = false
        scope.launch { settings.setVaultSort(mode) }
    }

    fun toggleFavorite(item: VaultItem) {
        scope.launch { repository.setFavorite(item.id, !item.favorite) }
    }

    fun togglePinned(item: VaultItem) {
        scope.launch { repository.setPinned(item.id, !item.pinned) }
    }

    fun requestDelete(item: VaultItem) {
        deleteItem = item
    }

    fun confirmDelete(item: VaultItem) {
        deleteItem = null
        scope.launch { repository.softDelete(item.id) }
    }

    fun showActions(item: VaultItem) {
        actionItem = item
    }

    fun hideActions() {
        actionItem = null
    }

    fun cancelDelete() {
        deleteItem = null
    }
}
