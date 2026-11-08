package com.ciphervault.app.domain

/** 保险库列表的过滤条件。 */
data class VaultFilterState(
    val query: String = "",
    val type: VaultItemType? = null,
    val favoritesOnly: Boolean = false,
    val folderId: String? = null,
)

/**
 * 保险库列表过滤策略。
 *
 * 搜索只匹配名称、用户名与 URL；类型/收藏/文件夹为与关系。
 */
object VaultFilter {
    fun apply(items: List<VaultItem>, state: VaultFilterState): List<VaultItem> = items.filter { item ->
        val matchesQuery = state.query.isBlank() ||
            item.name.contains(state.query, ignoreCase = true) ||
            item.username.contains(state.query, ignoreCase = true) ||
            item.url.contains(state.query, ignoreCase = true)
        val matchesType = state.type == null || item.type == state.type
        val matchesFavorite = !state.favoritesOnly || item.favorite
        val matchesFolder = state.folderId == null || item.folderId == state.folderId
        matchesQuery && matchesType && matchesFavorite && matchesFolder
    }
}
