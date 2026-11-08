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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ciphervault.app.R
import com.ciphervault.app.domain.FolderCounts
import com.ciphervault.app.data.VaultRepository
import com.ciphervault.app.domain.Folder
import com.ciphervault.app.ui.components.ConfirmDialog
import com.ciphervault.app.ui.components.EmptyState
import com.ciphervault.app.ui.components.ObsidianIconButton
import com.ciphervault.app.ui.components.ObsidianScaffold
import com.ciphervault.app.ui.components.ObsidianTextField
import com.ciphervault.app.ui.components.ObsidianTopBar
import com.ciphervault.app.ui.components.PrimaryButton
import com.ciphervault.app.ui.components.SettingRow
import kotlinx.coroutines.launch
import java.util.UUID

/** 文件夹管理：新建、删除；条目计数由 [FolderCounts] 一次性预计算。 */
@Composable
fun FoldersScreen(
    repository: VaultRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val folders by repository.folders.collectAsStateWithLifecycle(initialValue = emptyList())
    val items by repository.items.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }

    ObsidianScaffold(
        topBar = {
            ObsidianTopBar(
                title = stringResource(R.string.folders),
                onBack = onBack,
                actions = {
                    ObsidianIconButton(
                        icon = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.new_folder),
                        onClick = { showAdd = true },
                        primary = true,
                    )
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
            if (folders.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Folder,
                    title = stringResource(R.string.folders),
                    hint = stringResource(R.string.folders_desc),
                )
            } else {
                val counts = remember(items) { FolderCounts.byFolder(items) }
                folders.forEach { folder ->
                    val count = counts[folder.id] ?: 0
                    SettingRow(
                        title = folder.name,
                        subtitle = "$count",
                        icon = Icons.Outlined.Folder,
                        onClick = null,
                        trailing = {
                            ObsidianIconButton(
                                icon = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.delete),
                                onClick = { folderToDelete = folder },
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.new_folder)) },
            text = {
                ObsidianTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = stringResource(R.string.folder_name),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newName.trim()
                        if (name.isNotBlank()) {
                            scope.launch {
                                repository.saveFolder(Folder(UUID.randomUUID().toString(), name, folders.size))
                                newName = ""
                                showAdd = false
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    folderToDelete?.let { folder ->
        ConfirmDialog(
            title = stringResource(R.string.delete_folder_confirm),
            body = folder.name,
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            destructive = true,
            onConfirm = {
                scope.launch { repository.deleteFolder(folder.id) }
                folderToDelete = null
            },
            onDismiss = { folderToDelete = null },
        )
    }
}
