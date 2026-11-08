package com.ciphervault.app.crypto

import com.ciphervault.app.core.port.AeadPort
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM 加解密实现。
 *
 * 线格式：`IV(12 字节) || ciphertext+tag`。AAD 绑定应用标识与版本，
 * 修改 AAD 会导致历史数据无法解密，因此 [aad] 不可变更。
 */
object Crypto : AeadPort {
    const val KEY_BYTES = 32
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    // AAD 是线格式契约：`CipherVault:v1` 一旦改变，旧密文会验证失败。
    private val aad = "CipherVault:v1".toByteArray(Charsets.UTF_8)
    private val random = SecureRandom()

    fun randomBytes(size: Int): ByteArray = ByteArray(size).also(random::nextBytes)

    /** 返回 `IV || ciphertext`，调用方直接持久化整个返回值。 */

    override fun randomKey(): ByteArray = randomBytes(KEY_BYTES)

    override fun encrypt(key: ByteArray, plaintext: ByteArray): ByteArray {
        val iv = randomBytes(IV_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad)
        return iv + cipher.doFinal(plaintext)
    }

    override fun decrypt(key: ByteArray, payload: ByteArray): ByteArray {
        require(payload.size > IV_BYTES) { "Invalid ciphertext" }
        val iv = payload.copyOfRange(0, IV_BYTES)
        val ciphertext = payload.copyOfRange(IV_BYTES, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad)
        return cipher.doFinal(ciphertext)
    }

    override fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    override fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    /** 原地清零调用方传入的缓冲区；调用后原数组内容不可再用。 */
    override fun wipe(bytes: ByteArray?) {
        bytes?.fill(0)
    }

    /** 原地清零口令字符数组；调用后原数组内容不可再用。 */
    override fun wipe(chars: CharArray?) {
        chars?.fill('\u0000')
    }
}
