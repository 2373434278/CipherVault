package com.ciphervault.app.core.port

import com.ciphervault.app.domain.VaultMeta
import com.ciphervault.app.domain.VaultSort
import kotlinx.coroutines.flow.Flow

/** 设置与保险库元数据端口。 */
interface SettingsGateway {
    val metaFlow: Flow<VaultMeta?>
    val autoLockMinutes: Flow<Int>
    val clipboardClearSeconds: Flow<Int>
    val screenshotProtection: Flow<Boolean>
    val vaultSortMode: Flow<VaultSort>

    suspend fun readMeta(): VaultMeta?

    suspend fun saveMeta(meta: VaultMeta)

    suspend fun setAutoLockMinutes(minutes: Int)

    suspend fun setClipboardClearSeconds(seconds: Int)

    suspend fun setScreenshotProtection(enabled: Boolean)

    suspend fun setVaultSort(mode: VaultSort)
}
