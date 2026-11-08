package com.ciphervault.app.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import com.ciphervault.app.core.port.BiometricKeyringPort
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore 生物识别密钥实现。
 *
 * 密钥别名带 `_v1` 且要求 BIOMETRIC_STRONG；生物识别重新录入会使旧密钥失效，
 * 这正是 [KeyPermanentlyInvalidatedException] 触发清理的原因。
 */
class KeyStoreVault : BiometricKeyringPort {
    companion object {
        const val ALIAS = "ciphervault_biometric_v1"
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128

        // 与 Keystore 包裹格式绑定；改动会导致已有 biometricWrappedKey 无法解密。
        private val AAD = "CipherVault:biometric:v1".toByteArray(Charsets.UTF_8)
    }

    override fun createEncryptCipher(): Cipher {
        val key = existingKey() ?: generateKey()
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
    }

    override fun createDecryptCipher(payload: ByteArray): Cipher {
        require(payload.size > IV_BYTES) { "Invalid biometric payload" }
        val key = existingKey() ?: error("Biometric key missing")
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, payload.copyOfRange(0, IV_BYTES)))
        }
    }

    override fun encryptWith(cipher: Cipher, vaultKey: ByteArray): ByteArray {
        cipher.updateAAD(AAD)
        val ciphertext = cipher.doFinal(vaultKey)
        return cipher.iv + ciphertext
    }

    override fun decryptWith(cipher: Cipher, payload: ByteArray): ByteArray {
        require(payload.size > IV_BYTES) { "Invalid biometric payload" }
        cipher.updateAAD(AAD)
        return cipher.doFinal(payload.copyOfRange(IV_BYTES, payload.size))
    }

    override fun deleteKey() {
        runCatching {
            val store = keyStore()
            if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS)
        }
    }

    private fun existingKey(): SecretKey? = runCatching {
        keyStore().getKey(ALIAS, null) as? SecretKey
    }.getOrNull()

    private fun generateKey(): SecretKey {
        val builder = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            // 新增/删除生物识别后自动失效，强制重新注册。
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(builder.build())
        return generator.generateKey()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
}
