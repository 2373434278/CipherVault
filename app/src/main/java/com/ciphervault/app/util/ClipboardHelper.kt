package com.ciphervault.app.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import com.ciphervault.app.core.port.ClipboardGateway
import com.ciphervault.app.core.port.SettingsGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 剪贴板写入与定时清理。
 *
 * API 33+ 标记为敏感内容；清理前比较当前剪贴板值，避免误清用户后来复制的内容。
 */
class ClipboardHelper(context: Context, private val settings: SettingsGateway) : ClipboardGateway {
    private val clipboard = context.applicationContext.getSystemService(ClipboardManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun copy(label: String, value: String) {
        if (value.isBlank()) return
        scope.launch {
            val seconds = settings.clipboardClearSeconds.first()
            val clip = ClipData.newPlainText(label, value)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                clip.description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
            clipboard.setPrimaryClip(clip)
            if (seconds > 0) {
                delay(seconds * 1000L)
                // 只有剪贴板仍是本次写入的值时才清理，避免覆盖后续复制。
                val current = runCatching { clipboard.primaryClip?.getItemAt(0)?.text?.toString() }.getOrNull()
                if (current == value) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        clipboard.clearPrimaryClip()
                    } else {
                        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                    }
                }
            }
        }
    }
}
