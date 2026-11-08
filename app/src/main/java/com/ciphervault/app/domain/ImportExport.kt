package com.ciphervault.app.domain

import com.ciphervault.app.domain.importexport.BackupCodec
import com.ciphervault.app.domain.importexport.BitwardenImporter
import com.ciphervault.app.domain.importexport.CsvExporter
import com.ciphervault.app.domain.importexport.CsvImporter
import com.ciphervault.app.domain.importexport.PayloadJsonCodec
import com.ciphervault.app.domain.importexport.VaultFormats

/** 导入/导出的载荷容器。 */
data class VaultPayload(
    val items: List<VaultItem>,
    val folders: List<Folder>,
)

/**
 * 导入门面。
 *
 * 保留历史 API，内部委托 [VaultFormats] 的格式策略；新代码应直接使用格式注册表。
 */
object VaultImporter {
    /**
     * 自动识别格式并导入。
     *
     * 加密备份需要口令；未提供时抛出
     * [com.ciphervault.app.domain.importexport.EncryptedBackupRequiresPasswordException]。
     */
    fun importAuto(text: String): VaultPayload = VaultFormats.importAuto(text)

    fun importBitwarden(text: String): VaultPayload = BitwardenImporter.import(text)

    fun importCsv(text: String): VaultPayload = CsvImporter.import(text)
}

/**
 * 导出门面。
 *
 * 保留历史 API，内部委托格式策略与共享编解码器。
 */
object VaultExporter {
    fun exportCsv(items: List<VaultItem>, folders: List<Folder>): String =
        CsvExporter.export(items, folders)

    fun createEncryptedBackup(payload: VaultPayload, password: CharArray): String =
        BackupCodec.create(payload, password)

    fun isEncryptedBackup(text: String): Boolean = BackupCodec.isBackup(text)

    fun openEncryptedBackup(text: String, password: CharArray): VaultPayload =
        BackupCodec.open(text, password)

    internal fun payloadToJson(payload: VaultPayload): String = PayloadJsonCodec.encode(payload)

    internal fun payloadFromJson(text: String): VaultPayload = PayloadJsonCodec.decode(text)
}
