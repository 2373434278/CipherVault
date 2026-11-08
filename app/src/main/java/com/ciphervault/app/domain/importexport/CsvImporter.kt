package com.ciphervault.app.domain.importexport

import com.ciphervault.app.domain.CsvCodec
import com.ciphervault.app.domain.CsvRowSpec
import com.ciphervault.app.domain.ExtraCodec
import com.ciphervault.app.domain.Folder
import com.ciphervault.app.domain.TotpSettings
import com.ciphervault.app.domain.VaultItem
import com.ciphervault.app.domain.VaultItemType
import com.ciphervault.app.domain.VaultPayload
import java.util.UUID

/**
 * CSV 导入策略。
 *
 * 表头别名与无表头位置布局由 [CsvRowSpec] 统一解析；空行跳过，类型未知回退登录项。
 */
object CsvImporter {
    fun import(text: String): VaultPayload {
        val rows = CsvCodec.parse(text)
        require(rows.size >= 2) { "CSV has no data" }
        val layout = CsvRowSpec.layout(rows)
        val columns = layout.columns

        val folders = mutableMapOf<String, Folder>()
        val items = mutableListOf<VaultItem>()
        layout.dataRows.forEach { row ->
            if (row.all { it.isBlank() }) return@forEach
            val folderName = row.getOrNull(columns.folder).orEmpty().trim()
            val folder = if (folderName.isNotBlank()) {
                folders.getOrPut(folderName) { Folder(UUID.randomUUID().toString(), folderName, folders.size) }
            } else null
            val totpData = TotpSettings.parse(row.getOrNull(columns.totp).orEmpty())
            items.add(
                VaultItem(
                    id = UUID.randomUUID().toString(),
                    type = VaultItemType.fromCsv(row.getOrNull(columns.type)),
                    name = row.getOrNull(columns.name).orEmpty().ifBlank { "Untitled" },
                    username = row.getOrNull(columns.username).orEmpty(),
                    password = row.getOrNull(columns.password).orEmpty(),
                    url = row.getOrNull(columns.url).orEmpty(),
                    notes = row.getOrNull(columns.notes).orEmpty(),
                    totpSecret = totpData.secret,
                    totpAlgorithm = totpData.algorithm,
                    totpDigits = totpData.digits,
                    totpPeriod = totpData.period,
                    extra = ExtraCodec.fromCsv(row.getOrNull(columns.extra).orEmpty()),
                    folderId = folder?.id,
                    favorite = row.getOrNull(columns.favorite).toBooleanFlag(),
                    pinned = row.getOrNull(columns.pinned).toBooleanFlag(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
        return VaultPayload(items, folders.values.toList())
    }

    /** CSV 布尔列：`1` 或大小写不敏感的 `true`。 */
    private fun String?.toBooleanFlag(): Boolean = this == "1" || this.equals("true", ignoreCase = true)
}
