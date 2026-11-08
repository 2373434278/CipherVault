package com.ciphervault.app.session

import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Log
import com.ciphervault.app.core.port.AeadPort
import com.ciphervault.app.core.port.BiometricKeyringPort
import com.ciphervault.app.core.port.SettingsGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.Cipher

/**
 * 生物识别解锁协作者。
 *
 * 负责准备 Keystore Cipher、解包/包裹 vaultKey、启用/关闭生物识别。
 * `KeyPermanentlyInvalidatedException` 会触发清理失效的 Keystore 密钥与元数据。
 */
class BiometricUnlock(
    private val keyring: BiometricKeyringPort,
    private val settings: SettingsGateway,
    private val aead: AeadPort,
) {
    suspend fun prepareCipher(): Cipher? = withContext(Dispatchers.IO) {
        val meta = settings.readMeta() ?: return@withContext null
        val payload = meta.biometricWrappedKeyB64?.let { runCatching { aead.decode(it) }.getOrNull() }
            ?: return@withContext null
        val result = runCatching { keyring.createDecryptCipher(payload) }
        val failure = result.exceptionOrNull()
        if (failure != null) {
            Log.w(TAG, "Biometric key unavailable", failure)
            if (failure is KeyPermanentlyInvalidatedException) clearKeys()
            return@withContext null
        }
        result.getOrNull()
    }

    /** 解包出 vaultKey；失败时按失效类型清理密钥。 */
    suspend fun unwrap(cipher: Cipher): Result<ByteArray> = withContext(Dispatchers.Default) {
        val result = runCatching {
            val meta = settings.readMeta() ?: error("Vault not found")
            val payload = meta.biometricWrappedKeyB64?.let { aead.decode(it) }
                ?: error("Biometric unlock is not enabled")
            keyring.decryptWith(cipher, payload)
        }
        val failure = result.exceptionOrNull()
        if (failure != null) {
            Log.e(TAG, "Biometric unlock failed", failure)
            if (failure is KeyPermanentlyInvalidatedException) clearKeys()
        }
        result
    }

    suspend fun enable(cipher: Cipher, vaultKey: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val meta = settings.readMeta() ?: error("Vault not found")
            val wrapped = keyring.encryptWith(cipher, vaultKey)
            settings.saveMeta(meta.copy(biometricWrappedKeyB64 = aead.encode(wrapped)))
        }
    }

    suspend fun disable() = withContext(Dispatchers.IO) {
        val meta = settings.readMeta() ?: return@withContext
        settings.saveMeta(meta.copy(biometricWrappedKeyB64 = null))
        keyring.deleteKey()
    }

    private suspend fun clearKeys() {
        keyring.deleteKey()
        settings.readMeta()?.let { meta ->
            settings.saveMeta(meta.copy(biometricWrappedKeyB64 = null))
        }
    }

    private companion object {
        const val TAG = "BiometricUnlock"
    }
}
