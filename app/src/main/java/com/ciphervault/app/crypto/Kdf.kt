package com.ciphervault.app.crypto

import com.ciphervault.app.core.port.KdfPort
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.nio.CharBuffer
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 主密码派生参数。
 *
 * 这些字段会被写入 `VaultMeta` 与加密备份信封，属于持久化契约；
 * 调整默认成本只影响新库，不会重算历史数据。
 */
data class KdfParams(
    val id: String = ARGON2ID,
    val saltB64: String,
    val tCost: Int = 3,
    val mCostKib: Int = 65536,
    val parallelism: Int = 2,
) {
    companion object {
        const val ARGON2ID = "argon2id"
        const val PBKDF2 = "pbkdf2-sha256"
    }
}

/**
 * 主密码派生策略实现。
 *
 * 优先 Argon2id；native 库不可用时回退 PBKDF2-SHA256。
 * 参数（Argon2id t=3/m=64MiB/p=2，PBKDF2 600k 次）是安全契约，不要随意调整。
 */
object Kdf : KdfPort {
    const val ARGON2ID = KdfParams.ARGON2ID
    const val PBKDF2 = KdfParams.PBKDF2
    private const val KEY_BITS = 256

    // PBKDF2 迭代次数固定；信封里写的 t/m/p 对 PBKDF2 路径不生效。
    private const val PBKDF2_ITERATIONS = 600_000

    override fun newParams(lowRam: Boolean): KdfParams = KdfParams(
        saltB64 = Crypto.encode(Crypto.randomBytes(16)),
        // 低内存设备降到 32MiB，避免 Argon2 分配失败。
        mCostKib = if (lowRam) 32768 else 65536,
    )

    override fun argon2Available(): Boolean = runCatching { Argon2Kt() }.isSuccess

    /** 按 [KdfParams.id] 派生 32 字节 KEK；未知 id 按 Argon2id 处理以兼容旧元数据。 */
    override fun derive(password: CharArray, params: KdfParams): ByteArray = when (params.id) {
        PBKDF2 -> derivePbkdf2(password, Crypto.decode(params.saltB64))
        else -> deriveArgon2(password, params)
    }

    private fun deriveArgon2(password: CharArray, params: KdfParams): ByteArray {
        val passwordBytes = charsToUtf8(password)
        return try {
            Argon2Kt().hash(
                Argon2Mode.ARGON2_ID,
                passwordBytes,
                Crypto.decode(params.saltB64),
                params.tCost,
                params.mCostKib,
                params.parallelism,
                Crypto.KEY_BYTES,
            ).rawHashAsByteArray()
        } finally {
            Crypto.wipe(passwordBytes)
        }
    }

    private fun derivePbkdf2(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun charsToUtf8(chars: CharArray): ByteArray {
        val buffer = Charsets.UTF_8.encode(CharBuffer.wrap(chars))
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        if (buffer.hasArray()) buffer.array().fill(0)
        return bytes
    }
}
