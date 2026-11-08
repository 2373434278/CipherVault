package com.ciphervault.app.data

import com.ciphervault.app.domain.FAVORITE_FIRST_COMPARATOR
import com.ciphervault.app.domain.Folder
import com.ciphervault.app.domain.VaultItem
import com.ciphervault.app.domain.VaultItemType
import com.ciphervault.app.domain.toDomain
import com.ciphervault.app.domain.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * 保险库仓储。
 *
 * 对 UI 暴露 Flow/挂起函数，对自动填充提供按域名匹配的候选查询；
 * 实体 ↔ 领域模型转换与 `extra` 编解码由 [com.ciphervault.app.domain.VaultItem] 映射负责。
 */
class VaultRepository(private val dao: VaultDao) {
    val items: Flow<List<VaultItem>> = dao.observeActive().map { list -> list.map { it.toDomain() } }
    val trash: Flow<List<VaultItem>> = dao.observeTrash().map { list -> list.map { it.toDomain() } }
    val folders: Flow<List<Folder>> = dao.observeFolders().map { list -> list.map { it.toDomain() } }

    suspend fun getItem(id: String): VaultItem? = dao.getById(id)?.toDomain()

    /** 保存前补齐 id、名称与时间戳；`createdAt == 0` 表示新建。 */
    suspend fun saveItem(item: VaultItem): VaultItem {
        val now = System.currentTimeMillis()
        val normalized = item.copy(
            id = item.id.ifBlank { UUID.randomUUID().toString() },
            name = item.name.trim().ifBlank { "Untitled" },
            updatedAt = now,
            createdAt = if (item.createdAt == 0L) now else item.createdAt,
        )
        dao.upsert(normalized.toEntity())
        return normalized
    }

    suspend fun setFavorite(id: String, favorite: Boolean) {
        dao.setFavorite(id, favorite, System.currentTimeMillis())
    }

    suspend fun setPinned(id: String, pinned: Boolean) {
        dao.setPinned(id, pinned, System.currentTimeMillis())
    }

    suspend fun softDelete(id: String) {
        dao.softDelete(id, System.currentTimeMillis())
    }

    suspend fun restore(id: String) {
        dao.restore(id, System.currentTimeMillis())
    }

    suspend fun deletePermanently(id: String) {
        dao.deletePermanently(id)
    }

    suspend fun emptyTrash() {
        dao.emptyTrash()
    }

    suspend fun saveFolder(folder: Folder) {
        dao.upsertFolder(FolderEntity(folder.id, folder.name.trim(), folder.sortOrder))
    }

    /** 解绑条目与删除文件夹在同一事务内完成，避免留下悬空 folderId。 */
    suspend fun deleteFolder(id: String) {
        dao.deleteFolderCascade(id, System.currentTimeMillis())
    }

    suspend fun importItems(items: List<VaultItem>, folders: List<Folder>) {
        if (folders.isNotEmpty()) dao.upsertFolders(folders.map { FolderEntity(it.id, it.name, it.sortOrder) })
        if (items.isNotEmpty()) dao.upsertAll(items.map { it.toEntity() })
    }

    suspend fun getActiveItems(): List<VaultItem> = dao.getActive().map { it.toDomain() }
    suspend fun getFolders(): List<Folder> = dao.getFolders().map { it.toDomain() }

    /**
     * 自动填充候选匹配。
     *
     * 优先级：无域名 → 全部登录项；域名命中 → 命中集合；无命中 → 收藏项。
     * 每个分支都按“收藏优先、最近更新”排序。
     */
    suspend fun findForDomain(domain: String?, limit: Int = 5): List<VaultItem> {
        val all = getActiveItems().filter { it.type == VaultItemType.LOGIN }
        if (domain.isNullOrBlank()) {
            return all.sortedWith(FAVORITE_FIRST_COMPARATOR).take(limit)
        }
        val normalized = domain.lowercase().removePrefix("www.")
        val matches = all.filter { item ->
            item.domain?.removePrefix("www.")?.let { it == normalized || it.endsWith(".$normalized") } == true
        }
        if (matches.isNotEmpty()) {
            return matches.sortedWith(FAVORITE_FIRST_COMPARATOR).take(limit)
        }
        return all.filter { it.favorite }
            .sortedWith(FAVORITE_FIRST_COMPARATOR)
            .take(limit)
    }
}
