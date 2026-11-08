package com.ciphervault.app.data

import android.content.Context
import com.ciphervault.app.core.port.OpenedVaultStore
import com.ciphervault.app.core.port.VaultStoreOpener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLCipher + Room 的存储打开适配器。
 *
 * 负责 native 库可用性检查、打开数据库并触发首次 `writableDatabase`，
 * 从而把 Android/native 细节从会话状态机中隔离出去。
 */
class RoomVaultStore(
    private val context: Context,
    private val sqlCipherReady: Boolean,
) : VaultStoreOpener {
    override suspend fun open(key: ByteArray): OpenedVaultStore = withContext(Dispatchers.IO) {
        check(sqlCipherReady) { "SQLCipher native library is unavailable" }
        val database = VaultDatabaseFactory.open(context, key).also {
            it.openHelper.writableDatabase
        }
        object : OpenedVaultStore {
            override val repository: VaultRepository = VaultRepository(database.vaultDao())

            override fun close() {
                runCatching { database.close() }
            }
        }
    }
}
