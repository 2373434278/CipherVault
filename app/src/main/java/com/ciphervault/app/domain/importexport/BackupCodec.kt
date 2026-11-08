package com.ciphervault.app.domain.importexport

import com.ciphervault.app.crypto.Crypto
import com.ciphervault.app.crypto.Kdf
import com.ciphervault.app.crypto.KdfParams
import com.ciphervault.app.crypto.KdfPolicy
import com.ciphervault.app.domain.VaultPayload
import org.json.JSONObject

/**
 * 自有加密备份信封。
 *
 * 信封字段是兼容性契约：
 * `format=ciphervault-backup`、`version=1`、`kdf{id,salt,t,m,p}`、Base64 `data`。
 * 密钥与口令在 `finally` 中清零，失败也不会留下明文。
 */
object BackupCodec {
    const val FORMAT = "ciphervault-backup"
    const val VERSION = 1

    fun create(payload: VaultPayload, password: CharArray): String {
        require(password.size >= 8) { "Backup password must be at least 8 characters" }
        val params = KdfPolicy.forBackup()
        val key = Kdf.derive(password, params)
        return try {
            val plaintext = PayloadJsonCodec.encode(payload).toByteArray(Charsets.UTF_8)
            val ciphertext = Crypto.encrypt(key, plaintext)
            JSONObject().apply {
                put("format", FORMAT)
                put("version", VERSION)
                put("createdAt", System.currentTimeMillis())
                put("kdf", kdfToJson(params))
                put("data", Crypto.encode(ciphertext))
            }.toString(2)
        } finally {
            Crypto.wipe(key)
            Crypto.wipe(password)
        }
    }

    /** 只读取 `format` 字段，不解析 KDF；用于导入前的格式探测。 */
    fun isBackup(text: String): Boolean = runCatching {
        JSONObject(text).optString("format") == FORMAT
    }.getOrDefault(false)

    fun open(text: String, password: CharArray): VaultPayload {
        val root = JSONObject(text)
        require(root.optString("format") == FORMAT) { "Unsupported backup" }
        val params = kdfFromJson(root.getJSONObject("kdf"))
        val key = Kdf.derive(password, params)
        return try {
            val plaintext = com.ciphervault.app.crypto.Crypto
                .decrypt(key, Crypto.decode(root.getString("data")))
                .toString(Charsets.UTF_8)
            PayloadJsonCodec.decode(plaintext)
        } finally {
            Crypto.wipe(key)
            Crypto.wipe(password)
        }
    }

    private fun kdfToJson(params: KdfParams): JSONObject = JSONObject().apply {
        put("id", params.id)
        put("salt", params.saltB64)
        put("t", params.tCost)
        put("m", params.mCostKib)
        put("p", params.parallelism)
    }

    private fun kdfFromJson(json: JSONObject): KdfParams = KdfParams(
        id = json.getString("id"),
        saltB64 = json.getString("salt"),
        tCost = json.getInt("t"),
        mCostKib = json.getInt("m"),
        parallelism = json.getInt("p"),
    )
}
