package com.ciphervault.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ciphervault.app.AppServices
import com.ciphervault.app.R
import com.ciphervault.app.data.VaultRepository
import com.ciphervault.app.domain.TotpGenerator
import com.ciphervault.app.domain.VaultItem
import com.ciphervault.app.domain.VaultItemType
import com.ciphervault.app.domain.VaultSort
import com.ciphervault.app.domain.sortVaultItems
import com.ciphervault.app.ui.components.ConfirmDialog
import com.ciphervault.app.ui.components.EmptyState
import com.ciphervault.app.ui.components.FilterPill
import com.ciphervault.app.ui.components.ObsidianBottomBar
import com.ciphervault.app.ui.item.ItemPresentation
import com.ciphervault.app.ui.item.ItemTypeVisuals
import com.ciphervault.app.ui.navigation.TopLevelDestination
import com.ciphervault.app.ui.screenparts.QuickActionsSheet
import com.ciphervault.app.ui.screenparts.SortSheet
import com.ciphervault.app.ui.screenparts.VaultRow
import com.ciphervault.app.ui.state.VaultListStateHolder
import com.ciphervault.app.ui.components.ObsidianIconButton
import com.ciphervault.app.ui.components.ObsidianScaffold
import com.ciphervault.app.ui.components.ObsidianTopBar
import com.ciphervault.app.ui.components.SearchField
import com.ciphervault.app.ui.components.SettingRow
import com.ciphervault.app.core.port.ClipboardGateway
import kotlinx.coroutines.launch

/** 保险库列表：搜索、类型/收藏/文件夹筛选、排序与长按快捷操作。 */
@Composable
fun VaultScreen(
    container: AppServices,
    repository: VaultRepository,
    onOpenItem: (String) -> Unit,
    onEditItem: (String) -> Unit,
    onAdd: () -> Unit,
    onTab: (TopLevelDestination) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val items by repository.items.collectAsStateWithLifecycle(initialValue = emptyList())
    val folders by repository.folders.collectAsStateWithLifecycle(initialValue = emptyList())
    val sortMode by container.settingsStore.vaultSortMode.collectAsStateWithLifecycle(initialValue = VaultSort.PINNED)
    val listState = remember(repository, container.settingsStore) {
        VaultListStateHolder(repository, container.settingsStore, scope)
    }
    val filter = listState.filter
    val filtered = remember(items, filter) { listState.visibleItems(items) }
    val sorted = remember(filtered, sortMode) { sortVaultItems(filtered, sortMode) }

    ObsidianScaffold(
        topBar = {
            ObsidianTopBar(
                title = stringResource(R.string.vault),
                subtitle = pluralStringResource(R.plurals.vault_subtitle, items.size, items.size),
                actions = {
                    ObsidianIconButton(
                        icon = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = stringResource(R.string.sort),
                        onClick = { listState.showSort = true },
                    )
                    ObsidianIconButton(
                        icon = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.new_item),
                        onClick = onAdd,
                        primary = true,
                    )
                },
            )
        },
        bottomBar = { ObsidianBottomBar(selected = TopLevelDestination.VAULT, onSelect = onTab) },
    ) {
        Column(Modifier.fillMaxSize().padding(top = 4.dp)) {
            SearchField(
                query = listState.query,
                onQueryChange = { listState.query = it },
                placeholder = stringResource(R.string.search_hint),
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterPill(
                        text = stringResource(R.string.all),
                        selected = listState.selectedType == null && !listState.favoritesOnly,
                        onClick = listState::selectAll,
                    )
                }
                item {
                    FilterPill(
                        text = stringResource(R.string.favorites),
                        selected = listState.favoritesOnly,
                        onClick = listState::selectFavorites,
                    )
                }
                item {
                    FilterPill(
                        text = stringResource(R.string.logins),
                        selected = listState.selectedType == VaultItemType.LOGIN,
                        onClick = { listState.selectType(VaultItemType.LOGIN) },
                    )
                }
                item {
                    FilterPill(
                        text = stringResource(R.string.cards),
                        selected = listState.selectedType == VaultItemType.CARD,
                        onClick = { listState.selectType(VaultItemType.CARD) },
                    )
                }
                item {
                    FilterPill(
                        text = stringResource(R.string.notes),
                        selected = listState.selectedType == VaultItemType.NOTE,
                        onClick = { listState.selectType(VaultItemType.NOTE) },
                    )
                }
                item {
                    FilterPill(
                        text = stringResource(R.string.identities),
                        selected = listState.selectedType == VaultItemType.IDENTITY,
                        onClick = { listState.selectType(VaultItemType.IDENTITY) },
                    )
                }
            }
            if (folders.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterPill(
                            text = stringResource(R.string.all),
                            selected = listState.selectedFolderId == null,
                            onClick = { listState.selectFolder(null) },
                        )
                    }
                    items(folders, key = { it.id }) { folder ->
                        FilterPill(
                            text = folder.name,
                            selected = listState.selectedFolderId == folder.id,
                            onClick = { listState.selectFolder(folder.id) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (filtered.isEmpty()) {
                    item {
                        EmptyState(
                            icon = if (listState.query.isBlank()) Icons.Outlined.Shield else Icons.Outlined.Search,
                            title = stringResource(if (listState.query.isBlank()) R.string.empty_vault else R.string.empty_search),
                            hint = stringResource(if (listState.query.isBlank()) R.string.empty_vault_hint else R.string.search_hint),
                        )
                    }
                } else {
                    items(sorted, key = { it.id }) { item ->
                        VaultRow(
                            item = item,
                            onClick = { onOpenItem(item.id) },
                            onLongClick = { listState.showActions(item) },
                        )
                    }
                }
            }
        }
    }

    if (listState.showSort) {
        SortSheet(
            current = sortMode,
            onSelect = listState::setSort,
            onDismiss = { listState.showSort = false },
        )
    }

    listState.actionItem?.let { item ->
        QuickActionsSheet(
            item = item,
            clipboard = container.clipboardHelper,
            onToggleFavorite = { listState.toggleFavorite(item) },
            onTogglePinned = { listState.togglePinned(item) },
            onEdit = { onEditItem(item.id) },
            onDelete = { listState.requestDelete(item) },
            onDismiss = listState::hideActions,
        )
    }

    listState.deleteItem?.let { item ->
        ConfirmDialog(
            title = stringResource(R.string.delete_confirm),
            body = stringResource(R.string.delete_confirm_body),
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            destructive = true,
            onConfirm = { listState.confirmDelete(item) },
            onDismiss = listState::cancelDelete,
        )
    }
}
