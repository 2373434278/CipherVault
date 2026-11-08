package com.ciphervault.app.session

import com.ciphervault.app.core.port.AeadPort
import com.ciphervault.app.core.port.KdfPort
import com.ciphervault.app.crypto.KdfParams
import com.ciphervault.app.domain.VaultMeta

/**
 * 主密码 ↔ vaultKey 信封。
 *
 * 只负责派生 KEK、包裹/解包随机 vaultKey；调用方负责清零口令并持有返回的 vaultKey。
 * 失败时 KEK 一定在 `finally` 中清零。
 */
class KeyEnvelope(
    private val kdf: KdfPort,
    private val aead: AeadPort,
) {
    data class SealedVault(
        val meta: VaultMeta,
        val vaultKey: ByteArray,
    )

    fun create(password: CharArray, params: KdfParams): SealedVault {
        val kek = kdf.derive(password, params)
        val vaultKey = aead.randomKey()
        return try {
            val wrapped = aead.encrypt(kek, vaultKey)
            SealedVault(
                meta = VaultMeta(kdf = params, wrappedVaultKeyB64 = aead.encode(wrapped)),
                vaultKey = vaultKey,
            )
        } finally {
            aead.wipe(kek)
        }
    }

    fun unwrap(password: CharArray, meta: VaultMeta): ByteArray {
        val kek = kdf.derive(password, meta.kdf)
        return try {
            aead.decrypt(kek, aead.decode(meta.wrappedVaultKeyB64))
        } finally {
            aead.wipe(kek)
        }
    }
}
