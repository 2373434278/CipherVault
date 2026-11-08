package com.ciphervault.app.domain

import com.ciphervault.app.crypto.KdfParams
import org.json.JSONObject

/**
 * 保险库元数据：KDF 参数、被包裹的 vaultKey、可选的生物识别密钥。
 *
 * 元数据本身不含主密码或明文密钥；`wrappedVaultKey` 只能由主密码派生的 KEK 解开。
 */
data class VaultMeta(
    val version: Int = 1,
    val kdf: KdfParams,
    val wrappedVaultKeyB64: String,
    val biometricWrappedKeyB64: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val biometricEnabled: Boolean get() = !biometricWrappedKeyB64.isNullOrBlank()
}

/** 元数据 JSON 编解码器；字段名是持久化契约。 */
object VaultMetaCodec {
    fun encode(meta: VaultMeta): String = JSONObject().apply {
        put("version", meta.version)
        put("createdAt", meta.createdAt)
        put("wrappedVaultKey", meta.wrappedVaultKeyB64)
        if (meta.biometricWrappedKeyB64 != null) put("biometricWrappedKey", meta.biometricWrappedKeyB64)
        put("kdf", JSONObject().apply {
            put("id", meta.kdf.id)
            put("salt", meta.kdf.saltB64)
            put("t", meta.kdf.tCost)
            put("m", meta.kdf.mCostKib)
            put("p", meta.kdf.parallelism)
        })
    }.toString()

    fun decode(raw: String): VaultMeta? = runCatching {
        val root = JSONObject(raw)
        val kdf = root.getJSONObject("kdf")
        VaultMeta(
            version = root.optInt("version", 1),
            createdAt = root.optLong("createdAt", 0L),
            wrappedVaultKeyB64 = root.getString("wrappedVaultKey"),
            biometricWrappedKeyB64 = root.optString("biometricWrappedKey", "").takeIf { it.isNotBlank() },
            kdf = KdfParams(
                id = kdf.getString("id"),
                saltB64 = kdf.getString("salt"),
                tCost = kdf.getInt("t"),
                mCostKib = kdf.getInt("m"),
                parallelism = kdf.getInt("p"),
            ),
        )
    }.getOrNull()
}
