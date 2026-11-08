package com.ciphervault.app.domain

/**
 * 文件夹条目计数。
 *
 * 一次性构建 `folderId -> count`，避免在文件夹列表里对每个文件夹重新扫描全部条目。
 */
object FolderCounts {
    fun byFolder(items: List<VaultItem>): Map<String, Int> = buildMap {
        items.forEach { item ->
            val folderId = item.folderId ?: return@forEach
            put(folderId, (this[folderId] ?: 0) + 1)
        }
    }
}
