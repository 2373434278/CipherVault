package com.ciphervault.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO。
 *
 * 时间戳由调用方传入，便于仓储统一使用同一个 `now`；软删除通过 `deletedAt` 表达。
 */
@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items WHERE deletedAt IS NULL ORDER BY favorite DESC, updatedAt DESC")
    fun observeActive(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE deletedAt IS NULL")
    suspend fun getActive(): List<VaultItemEntity>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): VaultItemEntity?

    @Upsert
    suspend fun upsert(item: VaultItemEntity)

    @Upsert
    suspend fun upsertAll(items: List<VaultItemEntity>)

    @Query("UPDATE vault_items SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE vault_items SET deletedAt = NULL, updatedAt = :now WHERE id = :id")
    suspend fun restore(id: String, now: Long)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("DELETE FROM vault_items WHERE deletedAt IS NOT NULL")
    suspend fun emptyTrash()

    @Query("UPDATE vault_items SET favorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, now: Long)

    @Query("UPDATE vault_items SET pinned = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, now: Long)

    @Query("SELECT * FROM folders ORDER BY sortOrder, name")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY sortOrder, name")
    suspend fun getFolders(): List<FolderEntity>

    @Upsert
    suspend fun upsertFolder(folder: FolderEntity)

    @Upsert
    suspend fun upsertFolders(folders: List<FolderEntity>)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: String)

    @Query("UPDATE vault_items SET folderId = NULL, updatedAt = :now WHERE folderId = :id")
    suspend fun clearFolder(id: String, now: Long)

    /** 先解绑条目再删文件夹，放在同一事务里避免留下指向已删除文件夹的条目。 */
    @Transaction
    suspend fun deleteFolderCascade(id: String, now: Long) {
        clearFolder(id, now)
        deleteFolder(id)
    }
}
