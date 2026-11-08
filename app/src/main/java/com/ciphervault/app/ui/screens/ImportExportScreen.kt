package com.ciphervault.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ciphervault.app.R
import com.ciphervault.app.data.VaultRepository
import com.ciphervault.app.domain.VaultExporter
import com.ciphervault.app.domain.VaultImporter
import com.ciphervault.app.domain.VaultPayload
import com.ciphervault.app.domain.importexport.VaultFormats
import com.ciphervault.app.ui.components.ConfirmDialog
import com.ciphervault.app.ui.state.ImportExportStateHolder
import com.ciphervault.app.ui.state.TransferDialog
import com.ciphervault.app.ui.components.GlassCard
import com.ciphervault.app.ui.components.ObsidianScaffold
import com.ciphervault.app.ui.components.ObsidianTextField
import com.ciphervault.app.ui.components.ObsidianTopBar
import com.ciphervault.app.ui.components.SettingRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 导入导出：自动识别 CSV/Bitwarden/加密备份，并处理口令与结果提示。 */
@Composable
fun ImportExportScreen(
    repository: VaultRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { ImportExportStateHolder() }

    val stamp = remember { SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) }

    val createJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        writePending(uri, state.pendingExportContent, context, scope) { ok ->
            state.showMessage(context.getString(if (ok) R.string.export_success else R.string.export_error))
            state.clearPendingExport()
        }
    }
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        writePending(uri, state.pendingExportContent, context, scope) { ok ->
            state.showMessage(context.getString(if (ok) R.string.export_success else R.string.export_error))
            state.clearPendingExport()
        }
    }

    fun importText(text: String) {
        scope.launch {
            state.startLoading()
            val outcome = withContext(Dispatchers.Default) {
                runCatching {
                    val payload = VaultImporter.importAuto(text)
                    repository.importItems(payload.items, payload.folders)
                    payload
                }
            }
            state.stopLoading()
            state.showMessage(
                outcome.fold(
                    onSuccess = { context.getString(R.string.import_success, it.items.size) },
                    onFailure = { context.getString(R.string.import_error) },
                ),
            )
        }
    }

    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            state.startLoading()
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            state.stopLoading()
            if (text.isNullOrBlank()) {
                state.showMessage(context.getString(R.string.import_error))
                return@launch
            }
            if (VaultFormats.detect(text) == VaultFormats.encryptedBackup) {
                state.openImportPassword(text)
            } else {
                importText(text)
            }
        }
    }

    fun startEncryptedExport() {
        state.exportPasswordError = null
        scope.launch {
            state.startLoading()
            val payload = VaultPayload(repository.getActiveItems(), repository.getFolders())
            val content = withContext(Dispatchers.Default) {
                runCatching { VaultExporter.createEncryptedBackup(payload, state.exportPassword.toCharArray()) }
            }
            state.stopLoading()
            val text = content.getOrNull()
            if (text == null) {
                state.exportPasswordError = context.getString(R.string.export_error)
                return@launch
            }
            state.pendingExportContent = text
            state.dismiss()
            state.resetExportPasswords()
            createJson.launch("ciphervault-backup-$stamp.json")
        }
    }

    fun startCsvExport() {
        scope.launch {
            state.startLoading()
            val content = withContext(Dispatchers.Default) {
                runCatching {
                    VaultExporter.exportCsv(repository.getActiveItems(), repository.getFolders())
                }.getOrNull()
            }
            state.stopLoading()
            if (content == null) {
                state.showMessage(context.getString(R.string.export_error))
                return@launch
            }
            state.pendingExportContent = content
            state.dismiss()
            createCsv.launch("ciphervault-export-$stamp.csv")
        }
    }

    ObsidianScaffold(
        topBar = {
            ObsidianTopBar(
                title = stringResource(R.string.import_export_title),
                onBack = onBack,
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
            GlassCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.export_encrypted_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            SettingRow(
                title = stringResource(R.string.export_encrypted),
                subtitle = stringResource(R.string.export_encrypted_desc),
                icon = Icons.Outlined.Download,
                onClick = {
                    state.resetExportPasswords()
                    state.openExportPassword()
                },
            )
            SettingRow(
                title = stringResource(R.string.export_csv),
                subtitle = stringResource(R.string.export_csv_desc),
                icon = Icons.Outlined.Description,
                onClick = state::openPlaintextWarning,
            )
            SettingRow(
                title = stringResource(R.string.import_file),
                subtitle = stringResource(R.string.import_file_desc),
                icon = Icons.Outlined.UploadFile,
                onClick = {
                    openDocument.launch(
                        arrayOf("application/json", "text/csv", "text/comma-separated-values", "text/plain", "application/octet-stream"),
                    )
                },
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    when (val dialog = state.dialog) {
        TransferDialog.ExportPassword -> {
            AlertDialog(
                onDismissRequest = state::dismiss,
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(stringResource(R.string.export_encrypted)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ObsidianTextField(
                            value = state.exportPassword,
                            onValueChange = { state.exportPassword = it; state.exportPasswordError = null },
                            label = stringResource(R.string.backup_password),
                            placeholder = stringResource(R.string.backup_password_hint),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                        ObsidianTextField(
                            value = state.exportPasswordConfirm,
                            onValueChange = { state.exportPasswordConfirm = it; state.exportPasswordError = null },
                            label = stringResource(R.string.backup_password_confirm),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                        state.exportPasswordError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (state.exportPassword.length < 8) {
                                state.exportPasswordError = context.getString(R.string.password_too_short)
                            } else if (state.exportPassword != state.exportPasswordConfirm) {
                                state.exportPasswordError = context.getString(R.string.password_mismatch)
                            } else {
                                startEncryptedExport()
                            }
                        },
                    ) {
                        Text(stringResource(R.string.export))
                    }
                },
                dismissButton = {
                    TextButton(onClick = state::dismiss) { Text(stringResource(R.string.cancel)) }
                },
            )
        }

        TransferDialog.PlaintextWarning -> {
            ConfirmDialog(
                title = stringResource(R.string.plaintext_warning),
                body = stringResource(R.string.plaintext_warning_body),
                confirmText = stringResource(R.string.export),
                dismissText = stringResource(R.string.cancel),
                destructive = true,
                onConfirm = { startCsvExport() },
                onDismiss = state::dismiss,
            )
        }

        TransferDialog.ImportPassword -> {
            AlertDialog(
                onDismissRequest = state::dismiss,
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(stringResource(R.string.backup_password)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ObsidianTextField(
                            value = state.importPassword,
                            onValueChange = { state.importPassword = it; state.importPasswordError = null },
                            label = stringResource(R.string.backup_password),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                        state.importPasswordError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val text = state.pendingImportText ?: return@TextButton
                        if (state.importPassword.isBlank()) return@TextButton
                        state.startLoading()
                        scope.launch {
                            val outcome = withContext(Dispatchers.Default) {
                                runCatching {
                                    val payload = VaultExporter.openEncryptedBackup(text, state.importPassword.toCharArray())
                                    repository.importItems(payload.items, payload.folders)
                                    payload
                                }
                            }
                            state.stopLoading()
                            state.dismiss()
                            state.resetImportPassword()
                            state.clearPendingImport()
                            state.showMessage(
                                outcome.fold(
                                    onSuccess = { context.getString(R.string.import_success, it.items.size) },
                                    onFailure = { context.getString(R.string.import_error) },
                                ),
                            )
                        }
                    }) {
                        Text(stringResource(R.string.import_action))
                    }
                },
                dismissButton = {
                    TextButton(onClick = state::dismiss) { Text(stringResource(R.string.cancel)) }
                },
            )
        }

        is TransferDialog.Message -> {
            AlertDialog(
                onDismissRequest = state::dismiss,
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(stringResource(R.string.import_export_title)) },
                text = { Text(dialog.text) },
                confirmButton = {
                    TextButton(onClick = state::dismiss) { Text(stringResource(R.string.close)) }
                },
            )
        }

        null -> Unit
    }

    if (state.loading) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {},
            title = { Text(stringResource(R.string.processing)) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                }
            },
        )
    }
}

/** 通过 SAF 把待导出文本写入 Uri；结果通过 [onDone] 回传是否成功。 */
private fun writePending(
    uri: Uri?,
    content: String?,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onDone: (Boolean) -> Unit,
) {
    if (uri == null || content == null) {
        onDone(false)
        return
    }
    scope.launch {
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }
            }.isSuccess
        }
        onDone(ok)
    }
}
