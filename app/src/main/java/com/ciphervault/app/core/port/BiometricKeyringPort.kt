package com.ciphervault.app.core.port

import javax.crypto.Cipher

/** 生物识别 Keystore 端口；实现负责 Android Keystore 密钥与认证绑定。 */
interface BiometricKeyringPort {
    fun createEncryptCipher(): Cipher

    fun createDecryptCipher(payload: ByteArray): Cipher

    fun encryptWith(cipher: Cipher, vaultKey: ByteArray): ByteArray

    fun decryptWith(cipher: Cipher, payload: ByteArray): ByteArray

    fun deleteKey()
}
