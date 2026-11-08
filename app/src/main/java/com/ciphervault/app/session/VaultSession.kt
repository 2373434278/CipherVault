package com.ciphervault.app.session

import com.ciphervault.app.core.port.AeadPort
import com.ciphervault.app.core.port.OpenedVaultStore
import com.ciphervault.app.core.port.SettingsGateway
import com.ciphervault.app.core.port.VaultStoreOpener
import com.ciphervault.app.crypto.KdfParams
import com.ciphervault.app.data.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 保险库会话状态机，唯一持有 [Mutex]。
 *
 * 不变式：**状态发布与数据库/密钥生命周期在同一临界区**。
 * - 打开：先打开存储，再发布 `Unlocked`；
 * - 关闭：先发布 `Locked`，再关闭存储并清零 vaultKey。
 *
 * 因此不会出现 `Locked` 之后又发布 `Unlocked`，也不会让 UI 在资源关闭后继续访问
 * repository。
 */
class VaultSession(
    private val opener: VaultStoreOpener,
    private val keyEnvelope: KeyEnvelope,
    private val settings: SettingsGateway,
    private val aead: AeadPort,
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var opened: OpenedVaultStore? = null
    private var vaultKey: ByteArray? = null

    /** 当前已解锁的 repository；未解锁时为 null。 */
    val repository: VaultRepository?
        get() = (_state.value as? SessionState.Unlocked)?.repository

    suspend fun initialize() = mutex.withLock {
        if (_state.value !is SessionState.Loading) return@withLock
        _state.value = if (settings.readMeta() == null) SessionState.NeedsSetup else SessionState.Locked
    }

    suspend fun create(params: KdfParams, password: CharArray): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.Default) {
            runCatching {
                val sealed = keyEnvelope.create(password, params)
                var adopted = false
                try {
                    settings.saveMeta(sealed.meta)
                    openInner(sealed.vaultKey)
                    adopted = true
                } finally {
                    if (!adopted) aead.wipe(sealed.vaultKey)
                }
            }.onFailure {
                closeInner()
                _state.value = SessionState.NeedsSetup
            }
        }
    }

    suspend fun unlockWithPassword(password: CharArray): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.Default) {
            runCatching {
                val meta = settings.readMeta() ?: error("Vault not found")
                val key = keyEnvelope.unwrap(password, meta)
                var adopted = false
                try {
                    openInner(key)
                    adopted = true
                } finally {
                    if (!adopted) aead.wipe(key)
                }
            }
        }
    }

    /** 用已解包/由其他认证路径得到的 vaultKey 打开会话。 */
    suspend fun openWithKey(key: ByteArray): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching { openInner(key) }
        }
    }

    /** 在会话互斥区内使用 vaultKey；锁定后返回 null。 */
    suspend fun <T> withVaultKey(block: suspend (ByteArray) -> T): T? = mutex.withLock {
        val key = vaultKey ?: return@withLock null
        block(key)
    }

    suspend fun close() = mutex.withLock {
        _state.value = SessionState.Locked
        closeInner()
    }

    suspend fun markNeedsSetup() = mutex.withLock {
        _state.value = SessionState.NeedsSetup
        closeInner()
    }

    private suspend fun openInner(key: ByteArray) {
        closeInner()
        val store = try {
            opener.open(key)
        } catch (throwable: Throwable) {
            aead.wipe(key)
            throw throwable
        }
        opened = store
        vaultKey = key
        _state.value = SessionState.Unlocked(store.repository)
    }

    private suspend fun closeInner() {
        val previous = opened
        val previousKey = vaultKey
        opened = null
        vaultKey = null
        if (previous != null) {
            withContext(Dispatchers.IO) { previous.close() }
        }
        aead.wipe(previousKey)
    }
}
