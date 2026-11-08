package com.ciphervault.app.core.port

import com.ciphervault.app.data.VaultRepository

/** 已打开的保险库存储；关闭后不得再访问 repository。 */
interface OpenedVaultStore {
    val repository: VaultRepository

    fun close()
}

/**
 * 保险库存储打开端口。
 *
 * 实现负责 SQLCipher 可用性检查、打开数据库并返回可用 repository；
 * 调用方可从任意调度器调用，返回时数据库已可读写。
 */
interface VaultStoreOpener {
    suspend fun open(key: ByteArray): OpenedVaultStore
}
