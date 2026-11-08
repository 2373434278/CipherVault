package com.ciphervault.app.ui.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 导入导出页互斥的对话框状态。 */
sealed interface TransferDialog {
    data object ExportPassword : TransferDialog
    data object PlaintextWarning : TransferDialog
    data object ImportPassword : TransferDialog
    data class Message(val text: String) : TransferDialog
}

/**
 * 导入导出页状态持有者。
 *
 * 用单一 [dialog] 表达互斥弹窗，避免四个布尔值同时为 true；口令与待写文件内容也集中管理。
 */
@Stable
class ImportExportStateHolder {
    var loading by mutableStateOf(false)
        private set
    var dialog by mutableStateOf<TransferDialog?>(null)
        private set
    var exportPassword by mutableStateOf("")
    var exportPasswordConfirm by mutableStateOf("")
    var exportPasswordError by mutableStateOf<String?>(null)
    var importPassword by mutableStateOf("")
    var importPasswordError by mutableStateOf<String?>(null)
    var pendingExportContent by mutableStateOf<String?>(null)
    var pendingImportText by mutableStateOf<String?>(null)

    fun startLoading() {
        loading = true
    }

    fun stopLoading() {
        loading = false
    }

    fun openExportPassword() {
        exportPasswordError = null
        dialog = TransferDialog.ExportPassword
    }

    fun openPlaintextWarning() {
        dialog = TransferDialog.PlaintextWarning
    }

    fun openImportPassword(text: String) {
        pendingImportText = text
        importPassword = ""
        importPasswordError = null
        dialog = TransferDialog.ImportPassword
    }

    fun showMessage(text: String) {
        dialog = TransferDialog.Message(text)
    }

    fun dismiss() {
        dialog = null
    }

    fun clearPendingExport() {
        pendingExportContent = null
    }

    fun clearPendingImport() {
        pendingImportText = null
    }

    fun resetExportPasswords() {
        exportPassword = ""
        exportPasswordConfirm = ""
        exportPasswordError = null
    }

    fun resetImportPassword() {
        importPassword = ""
        importPasswordError = null
    }
}
