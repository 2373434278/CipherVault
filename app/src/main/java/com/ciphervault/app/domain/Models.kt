package com.ciphervault.app.domain

import com.ciphervault.app.data.FolderEntity
import com.ciphervault.app.data.VaultItemEntity

/** 条目类型；`storage` 同时用于数据库、CSV、路由与筛选，属于持久化契约。 */
enum class VaultItemType(val storage: String) {
    LOGIN("login"),
    NOTE("note"),
    CARD("card"),
    IDENTITY("identity");

    companion object {
        /** 数据库/路由/筛选使用的 storage 值；未知值回退登录项。 */
        fun from(value: String?): VaultItemType =
            entries.firstOrNull { it.storage == value } ?: LOGIN

        /** CSV 类型列：支持英文名、Bitwarden 数字码与 `secure_note` 别名。 */
        fun fromCsv(value: String?): VaultItemType = when (value?.trim()?.lowercase()) {
            "note", "secure_note", "2" -> NOTE
            "card", "3" -> CARD
            "identity", "4" -> IDENTITY
            else -> LOGIN
        }

        /** Bitwarden 数字类型码：1=登录，2=笔记，3=银行卡，4=身份，未知回退登录。 */
        fun fromBitwarden(code: Int): VaultItemType = when (code) {
            2 -> NOTE
            3 -> CARD
            4 -> IDENTITY
            else -> LOGIN
        }
    }
}

/**
 * 统一条目模型。
 *
 * 四类条目共用同一结构：登录项使用顶层凭证字段，银行卡/身份信息使用 [extra]；
 * 字段键的持久化契约见 [ItemFieldKeys]。
 */
data class VaultItem(
    val id: String,
    val type: VaultItemType,
    val name: String,
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val totpSecret: String = "",
    val totpAlgorithm: String = "SHA1",
    val totpDigits: Int = 6,
    val totpPeriod: Int = 30,
    val extra: Map<String, String> = emptyMap(),
    val folderId: String? = null,
    val favorite: Boolean = false,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
) {
    val hasTotp: Boolean get() = totpSecret.isNotBlank()

    /** 从 URL 提取主机名；去掉协议、路径与端口。 */
    val domain: String? get() = url.trim().lowercase()
        .removePrefix("https://").removePrefix("http://").substringBefore('/')
        .substringBefore(':').takeIf { it.isNotBlank() }
}

/** 文件夹；`sortOrder` 决定列表顺序。 */
data class Folder(
    val id: String,
    val name: String,
    val sortOrder: Int = 0,
)

fun VaultItemEntity.toDomain(): VaultItem = VaultItem(
    id = id,
    type = VaultItemType.from(type),
    name = name,
    username = username.orEmpty(),
    password = password.orEmpty(),
    url = url.orEmpty(),
    notes = notes.orEmpty(),
    totpSecret = totpSecret.orEmpty(),
    totpAlgorithm = totpAlgorithm,
    totpDigits = totpDigits,
    totpPeriod = totpPeriod,
    extra = ExtraCodec.decode(payloadJson),
    folderId = folderId,
    favorite = favorite,
    pinned = pinned,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun VaultItem.toEntity(): VaultItemEntity = VaultItemEntity(
    id = id,
    type = type.storage,
    name = name,
    username = username.ifBlank { null },
    password = password.ifBlank { null },
    url = url.ifBlank { null },
    notes = notes.ifBlank { null },
    totpSecret = totpSecret.ifBlank { null },
    totpAlgorithm = totpAlgorithm,
    totpDigits = totpDigits,
    totpPeriod = totpPeriod,
    payloadJson = ExtraCodec.encode(extra),
    folderId = folderId,
    favorite = favorite,
    pinned = pinned,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun FolderEntity.toDomain(): Folder = Folder(id, name, sortOrder)
