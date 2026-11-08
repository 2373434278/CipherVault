package com.ciphervault.app.domain.importexport

import com.ciphervault.app.domain.CsvCodec
import com.ciphervault.app.domain.CsvRowSpec
import com.ciphervault.app.domain.ExtraCodec
import com.ciphervault.app.domain.Folder
import com.ciphervault.app.domain.OtpauthUri
import com.ciphervault.app.domain.VaultItem
import com.ciphervault.app.domain.totpSettings

/**
 * CSV 导出策略。
 *
 * 列顺序由 [CsvRowSpec.HEADER] 固定，TOTP 以完整 `otpauth://` URI 输出。
 */
object CsvExporter {
    fun export(items: List<VaultItem>, folders: List<Folder>): String {
        val folderNames = folders.associate { it.id to it.name }
        val rows = mutableListOf(CsvRowSpec.HEADER)
        items.forEach { item ->
            rows.add(
                listOf(
                    item.name,
                    item.type.storage,
                    folderNames[item.folderId].orEmpty(),
                    item.url,
                    item.username,
                    item.password,
                    item.notes,
                    if (item.hasTotp) OtpauthUri.build(item.name, item.totpSettings()) else "",
                    ExtraCodec.toCsv(item.extra),
                    if (item.favorite) "1" else "0",
                    if (item.pinned) "1" else "0",
                ),
            )
        }
        return CsvCodec.encode(rows)
    }
}
