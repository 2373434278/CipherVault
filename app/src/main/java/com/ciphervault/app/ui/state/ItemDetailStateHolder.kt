package com.ciphervault.app.ui.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ciphervault.app.data.VaultRepository
import com.ciphervault.app.domain.VaultItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 条目详情状态持有者。
 *
 * 从 repository 的 active 列表派生当前条目；锁定后持有者随导航树销毁。
 */
@Stable
class ItemDetailStateHolder(
    repository: VaultRepository,
    itemId: String,
    scope: CoroutineScope,
) {
    val item: StateFlow<VaultItem?> = repository.items
        .map { items -> items.firstOrNull { it.id == itemId } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)

    var revealed by mutableStateOf(false)
        private set
    var showDelete by mutableStateOf(false)
        private set

    fun toggleReveal() {
        revealed = !revealed
    }

    fun requestDelete() {
        showDelete = true
    }

    fun cancelDelete() {
        showDelete = false
    }
}
