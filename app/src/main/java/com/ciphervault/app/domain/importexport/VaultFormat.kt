package com.ciphervault.app.domain.importexport

import com.ciphervault.app.domain.VaultPayload

/**
 * 导入导出格式策略。
 *
 * 每个实现自行负责格式探测、解码与编码；新增格式只需增加一个实现并注册到 [VaultFormats]。
 */
sealed interface VaultFormat {
    val id: String

    /** 解码是否需要口令；加密备份为 true。 */
    val requiresPassword: Boolean

    fun matches(text: String): Boolean

    fun decode(text: String, password: CharArray? = null): VaultPayload

    fun encode(payload: VaultPayload, password: CharArray? = null): String
}

/** 自动导入遇到加密备份但未提供口令时抛出。 */
class EncryptedBackupRequiresPasswordException :
    IllegalArgumentException("Encrypted backup requires a password")

/** 自有加密备份格式；探测优先级最高，避免被 JSON 分支误判。 */
object EncryptedBackupFormat : VaultFormat {
    override val id: String = BackupCodec.FORMAT
    override val requiresPassword: Boolean = true

    override fun matches(text: String): Boolean = BackupCodec.isBackup(text)

    override fun decode(text: String, password: CharArray?): VaultPayload {
        if (password == null) throw EncryptedBackupRequiresPasswordException()
        return BackupCodec.open(text, password)
    }

    override fun encode(payload: VaultPayload, password: CharArray?): String {
        requireNotNull(password) { "Encrypted backup requires a password" }
        return BackupCodec.create(payload, password)
    }
}

/** Bitwarden 导出 JSON；仅支持导入。 */
object BitwardenJsonFormat : VaultFormat {
    override val id: String = "bitwarden-json"
    override val requiresPassword: Boolean = false

    override fun matches(text: String): Boolean = text.trimStart().startsWith("{")

    override fun decode(text: String, password: CharArray?): VaultPayload = BitwardenImporter.import(text)

    override fun encode(payload: VaultPayload, password: CharArray?): String =
        error("Bitwarden export is not supported")
}

/** CSV 格式；作为非 JSON 文本的兜底格式。 */
object CsvFormat : VaultFormat {
    override val id: String = "csv"
    override val requiresPassword: Boolean = false

    override fun matches(text: String): Boolean = true

    override fun decode(text: String, password: CharArray?): VaultPayload = CsvImporter.import(text)

    override fun encode(payload: VaultPayload, password: CharArray?): String =
        CsvExporter.export(payload.items, payload.folders)
}

/**
 * 格式注册表。
 *
 * 探测顺序固定为：加密备份 → Bitwarden JSON → CSV。
 * 加密备份本身也是 JSON，因此必须先于 Bitwarden 判断。
 */
object VaultFormats {
    val encryptedBackup: VaultFormat = EncryptedBackupFormat
    val bitwardenJson: VaultFormat = BitwardenJsonFormat
    val csv: VaultFormat = CsvFormat

    private val ordered: List<VaultFormat> = listOf(encryptedBackup, bitwardenJson, csv)

    fun detect(text: String): VaultFormat {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "Empty file" }
        return ordered.first { it.matches(trimmed) }
    }

    fun importAuto(text: String, password: CharArray? = null): VaultPayload {
        val trimmed = text.trim()
        return detect(trimmed).decode(trimmed, password)
    }

    fun isEncryptedBackup(text: String): Boolean = encryptedBackup.matches(text)
}
