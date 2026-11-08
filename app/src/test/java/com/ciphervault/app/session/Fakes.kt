package com.ciphervault.app.session

import com.ciphervault.app.core.port.KdfPort
import com.ciphervault.app.core.port.OpenedVaultStore
import com.ciphervault.app.core.port.SettingsGateway
import com.ciphervault.app.core.port.VaultStoreOpener
import com.ciphervault.app.crypto.KdfParams
import com.ciphervault.app.data.FolderEntity
import com.ciphervault.app.data.VaultDao
import com.ciphervault.app.data.VaultItemEntity
import com.ciphervault.app.data.VaultRepository
import com.ciphervault.app.domain.VaultMeta
import com.ciphervault.app.domain.VaultSort
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.security.MessageDigest

internal class FakeKdf : KdfPort {
    var lastDerived: ByteArray? = null

    override fun newParams(lowRam: Boolean): KdfParams = KdfParams(
        id = "fake-kdf",
        saltB64 = "c2FsdA==",
        tCost = 1,
        mCostKib = 1,
        parallelism = 1,
    )

    override fun derive(password: CharArray, params: KdfParams): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest((params.saltB64 + String(password)).toByteArray())
        lastDerived = digest
        return digest
    }

    override fun argon2Available(): Boolean = false
}

internal class FakeSettings : SettingsGateway {
    override val metaFlow = MutableStateFlow<VaultMeta?>(null)
    override val autoLockMinutes = MutableStateFlow(1)
    override val clipboardClearSeconds = MutableStateFlow(30)
    override val screenshotProtection = MutableStateFlow(true)
    override val vaultSortMode = MutableStateFlow(VaultSort.PINNED)

    override suspend fun readMeta(): VaultMeta? = metaFlow.value

    override suspend fun saveMeta(meta: VaultMeta) {
        metaFlow.value = meta
    }

    override suspend fun setAutoLockMinutes(minutes: Int) {
        autoLockMinutes.value = minutes
    }

    override suspend fun setClipboardClearSeconds(seconds: Int) {
        clipboardClearSeconds.value = seconds
    }

    override suspend fun setScreenshotProtection(enabled: Boolean) {
        screenshotProtection.value = enabled
    }

    override suspend fun setVaultSort(mode: VaultSort) {
        vaultSortMode.value = mode
    }
}

internal class FakeVaultDao : VaultDao {
    override fun observeActive(): Flow<List<VaultItemEntity>> = flowOf(emptyList())

    override fun observeTrash(): Flow<List<VaultItemEntity>> = flowOf(emptyList())

    override suspend fun getActive(): List<VaultItemEntity> = emptyList()

    override suspend fun getById(id: String): VaultItemEntity? = null

    override suspend fun upsert(item: VaultItemEntity) = Unit

    override suspend fun upsertAll(items: List<VaultItemEntity>) = Unit

    override suspend fun softDelete(id: String, now: Long) = Unit

    override suspend fun restore(id: String, now: Long) = Unit

    override suspend fun deletePermanently(id: String) = Unit

    override suspend fun emptyTrash() = Unit

    override suspend fun setFavorite(id: String, favorite: Boolean, now: Long) = Unit

    override suspend fun setPinned(id: String, pinned: Boolean, now: Long) = Unit

    override fun observeFolders(): Flow<List<FolderEntity>> = flowOf(emptyList())

    override suspend fun getFolders(): List<FolderEntity> = emptyList()

    override suspend fun upsertFolder(folder: FolderEntity) = Unit

    override suspend fun upsertFolders(folders: List<FolderEntity>) = Unit

    override suspend fun deleteFolder(id: String) = Unit

    override suspend fun clearFolder(id: String, now: Long) = Unit
}

internal class FakeOpener : VaultStoreOpener {
    private val repository = VaultRepository(FakeVaultDao())

    var openCount: Int = 0
    var closeCount: Int = 0
    var gate: CompletableDeferred<Unit>? = null
    var failWith: Throwable? = null
    val started = CompletableDeferred<Unit>()

    override suspend fun open(key: ByteArray): OpenedVaultStore {
        openCount++
        started.complete(Unit)
        gate?.await()
        failWith?.let { throw it }
        return object : OpenedVaultStore {
            override val repository: VaultRepository = this@FakeOpener.repository

            override fun close() {
                closeCount++
            }
        }
    }
}
