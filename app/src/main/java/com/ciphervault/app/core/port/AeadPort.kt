package com.ciphervault.app.core.port

/**
 * AES-GCM 封装端口。
 *
 * 契约与现有 `Crypto` 完全一致：`encrypt` 输出 `IV || ciphertext`，
 * AAD 固定为 `CipherVault:v1`。仅用于解耦与测试替换，不得改变字节布局。
 */
interface AeadPort {
    fun randomKey(): ByteArray

    fun encrypt(key: ByteArray, plaintext: ByteArray): ByteArray

    fun decrypt(key: ByteArray, payload: ByteArray): ByteArray

    fun encode(bytes: ByteArray): String

    fun decode(value: String): ByteArray

    fun wipe(bytes: ByteArray?)

    fun wipe(chars: CharArray?)
}
