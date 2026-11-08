package com.ciphervault.app.domain

/**
 * 保险库条目的列表副标题。
 *
 * 优先级：用户名 → URL → 银行卡持卡人 → 身份姓名 → 备注前 60 字。
 */
object VaultItemSubtitle {
    fun of(item: VaultItem): String = when {
        item.username.isNotBlank() -> item.username
        item.url.isNotBlank() -> item.url
        item.type == VaultItemType.CARD -> item.extra["cardholder"].orEmpty()
        item.type == VaultItemType.IDENTITY -> item.extra["fullName"].orEmpty()
        else -> item.notes.take(60)
    }
}
