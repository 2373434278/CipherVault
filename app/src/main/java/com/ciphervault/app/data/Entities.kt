package com.ciphervault.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 保险库条目表。
 *
 * 空字符串在领域映射时转为 null；[payloadJson] 保存银行卡/身份信息的 extra 字段。
 */
@Entity(
    tableName = "vault_items",
    indices = [Index("folderId"), Index("deletedAt"), Index("type")],
)
data class VaultItemEntity(
    @PrimaryKey val id: String,
    val type: String,
    val name: String,
    val username: String? = null,
    val password: String? = null,
    val url: String? = null,
    val notes: String? = null,
    val totpSecret: String? = null,
    val totpAlgorithm: String = "SHA1",
    val totpDigits: Int = 6,
    val totpPeriod: Int = 30,
    val payloadJson: String? = null,
    val folderId: String? = null,
    val favorite: Boolean = false,
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** 文件夹表；删除文件夹时条目由事务解绑。 */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int = 0,
)
