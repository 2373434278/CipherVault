package com.ciphervault.app.ui.screenparts

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
import com.ciphervault.app.ui.state.VaultListStateHolder
import com.ciphervault.app.ui.components.ObsidianIconButton
import com.ciphervault.app.ui.components.ObsidianScaffold
import com.ciphervault.app.ui.components.ObsidianTopBar
import com.ciphervault.app.ui.components.SearchField
import com.ciphervault.app.ui.components.SettingRow
import com.ciphervault.app.core.port.ClipboardGateway
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VaultRow(item: VaultItem, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .combinedClickable(
                onLongClickLabel = stringResource(R.string.quick_actions),
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ItemTypeVisuals.icon(item.type),
                contentDescription = null,
                tint = ItemTypeVisuals.accent(item.type),
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.pinned) {
                    Icon(
                        Icons.Outlined.PushPin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp),
                    )
                }
                if (item.favorite) {
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = ItemPresentation.subtitle(item),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item.hasTotp) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "TOTP",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 8.sp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SortSheet(
    current: VaultSort,
    onSelect: (VaultSort) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.sort),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            SortOption(R.string.sort_pinned, current == VaultSort.PINNED) { onSelect(VaultSort.PINNED) }
            SortOption(R.string.sort_updated, current == VaultSort.UPDATED) { onSelect(VaultSort.UPDATED) }
            SortOption(R.string.sort_created, current == VaultSort.CREATED) { onSelect(VaultSort.CREATED) }
            SortOption(R.string.sort_name, current == VaultSort.NAME) { onSelect(VaultSort.NAME) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SortOption(label: Int, selected: Boolean, onClick: () -> Unit) {
    SettingRow(
        title = stringResource(label),
        onClick = onClick,
        trailing = {
            if (selected) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickActionsSheet(
    item: VaultItem,
    clipboard: ClipboardGateway,
    onToggleFavorite: () -> Unit,
    onTogglePinned: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = ItemPresentation.subtitle(item),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            if (item.username.isNotBlank()) {
                SettingRow(
                    title = stringResource(R.string.copy_username),
                    icon = Icons.Outlined.Person,
                    onClick = {
                        clipboard.copy("username", item.username)
                        onDismiss()
                    },
                )
            }
            if (item.password.isNotBlank()) {
                SettingRow(
                    title = stringResource(R.string.copy_password),
                    icon = Icons.Outlined.Key,
                    onClick = {
                        clipboard.copy("password", item.password)
                        onDismiss()
                    },
                )
            }
            if (item.hasTotp) {
                SettingRow(
                    title = stringResource(R.string.copy_totp),
                    icon = Icons.Outlined.Pin,
                    onClick = {
                        copyTotp(item, clipboard)
                        onDismiss()
                    },
                )
            }
            SettingRow(
                title = stringResource(if (item.favorite) R.string.remove_favorite else R.string.favorite),
                icon = if (item.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                onClick = {
                    onToggleFavorite()
                    onDismiss()
                },
            )
            SettingRow(
                title = stringResource(if (item.pinned) R.string.unpin else R.string.pin_to_top),
                icon = Icons.Outlined.PushPin,
                onClick = {
                    onTogglePinned()
                    onDismiss()
                },
            )
            SettingRow(
                title = stringResource(R.string.edit),
                icon = Icons.Outlined.Edit,
                onClick = {
                    onDismiss()
                    onEdit()
                },
            )
            SettingRow(
                title = stringResource(R.string.move_to_trash),
                icon = Icons.Outlined.Delete,
                onClick = {
                    onDismiss()
                    onDelete()
                },
                destructive = true,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun copyTotp(item: VaultItem, clipboard: ClipboardGateway) {
    val code = runCatching {
        TotpGenerator.generate(
            secret = item.totpSecret,
            algorithm = item.totpAlgorithm,
            digits = item.totpDigits,
            period = item.totpPeriod,
            timestampMillis = System.currentTimeMillis(),
        ).code
    }.getOrNull()
    if (code != null) clipboard.copy("totp", code)
}
