package com.ciphervault.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ciphervault.app.R
import com.ciphervault.app.ui.item.ItemTypeVisuals
import com.ciphervault.app.data.VaultRepository
import com.ciphervault.app.domain.VaultItem
import com.ciphervault.app.ui.components.ConfirmDialog
import com.ciphervault.app.ui.components.EmptyState
import com.ciphervault.app.ui.components.ObsidianIconButton
import com.ciphervault.app.ui.components.ObsidianScaffold
import com.ciphervault.app.ui.components.ObsidianTopBar
import com.ciphervault.app.ui.components.SettingRow
import kotlinx.coroutines.launch

/** 回收站：恢复、单条永久删除与清空。 */
@Composable
fun TrashScreen(
    repository: VaultRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val trash by repository.trash.collectAsStateWithLifecycle(initialValue = emptyList())
    var showEmpty by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<VaultItem?>(null) }

    ObsidianScaffold(
        topBar = {
            ObsidianTopBar(
                title = stringResource(R.string.trash),
                onBack = onBack,
                actions = {
                    if (trash.isNotEmpty()) {
                        TextButton(onClick = { showEmpty = true }) {
                            Text(stringResource(R.string.empty_trash_action), color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (trash.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Shield,
                    title = stringResource(R.string.empty_trash),
                    hint = pluralStringResource(R.plurals.trash_desc, 0, 0),
                )
            } else {
                trash.forEach { item ->
                    SettingRow(
                        title = item.name,
                        subtitle = item.username.ifBlank { ItemTypeVisuals.label(item.type) },
                        onClick = null,
                        trailing = {
                            ObsidianIconButton(
                                icon = Icons.Outlined.Restore,
                                contentDescription = stringResource(R.string.restore),
                                onClick = { scope.launch { repository.restore(item.id) } },
                            )
                            ObsidianIconButton(
                                icon = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.delete_permanently),
                                onClick = { deleteTarget = item },
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showEmpty) {
        ConfirmDialog(
            title = stringResource(R.string.empty_trash_action),
            body = stringResource(R.string.delete_forever_body),
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            destructive = true,
            onConfirm = {
                scope.launch { repository.emptyTrash() }
                showEmpty = false
            },
            onDismiss = { showEmpty = false },
        )
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.delete_forever_confirm)) },
            text = { Text(stringResource(R.string.delete_forever_body)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deletePermanently(item.id) }
                    deleteTarget = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
