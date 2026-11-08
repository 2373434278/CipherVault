package com.ciphervault.app.ui.state

import com.ciphervault.app.domain.TotpSettings
import com.ciphervault.app.domain.VaultItem
import com.ciphervault.app.domain.VaultItemType

/**
 * 条目编辑表单的不可变快照。
 *
 * 用单一对象替代十余个独立 `remember` 变量；`pinned` 必须原样带回 [toItem]，
 * 否则编辑已有条目会意外取消置顶。
 */
data class ItemDraft(
    val type: VaultItemType = VaultItemType.LOGIN,
    val name: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val totp: TotpSettings = TotpSettings(),
    val extra: Map<String, String> = emptyMap(),
    val folderId: String? = null,
    val favorite: Boolean = false,
    val pinned: Boolean = false,
    val createdAt: Long = 0L,
) {
    fun toItem(id: String, now: Long = System.currentTimeMillis()): VaultItem = VaultItem(
        id = id,
        type = type,
        name = name,
        username = username,
        password = password,
        url = url,
        notes = notes,
        totpSecret = totp.secret.trim(),
        totpAlgorithm = totp.algorithm,
        totpDigits = totp.digits,
        totpPeriod = totp.period,
        extra = extra,
        folderId = folderId,
        favorite = favorite,
        pinned = pinned,
        createdAt = if (createdAt == 0L) now else createdAt,
        updatedAt = now,
    )

    companion object {
        fun from(item: VaultItem): ItemDraft = ItemDraft(
            type = item.type,
            name = item.name,
            username = item.username,
            password = item.password,
            url = item.url,
            notes = item.notes,
            totp = TotpSettings(
                secret = item.totpSecret,
                algorithm = item.totpAlgorithm,
                digits = item.totpDigits,
                period = item.totpPeriod,
            ),
            extra = item.extra,
            folderId = item.folderId,
            favorite = item.favorite,
            pinned = item.pinned,
            createdAt = item.createdAt,
        )
    }
}
