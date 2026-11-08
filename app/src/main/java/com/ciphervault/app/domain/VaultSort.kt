package com.ciphervault.app.domain

/**
 * 保险库排序模式。
 *
 * 比较器放在枚举上，列表页、数据层与测试共用同一份 tie-break 规则：
 * 名称（忽略大小写）→ 更新时间 → id，保证结果确定。
 */
enum class VaultSort(val storage: String, val comparator: Comparator<VaultItem>) {
    PINNED("pinned", byPinned),
    UPDATED("updated", byUpdated),
    CREATED("created", byCreated),
    NAME("name", byName);

    companion object {
        /** 旧版本把“收藏”当作置顶存储，未知值统一回退 [PINNED]。 */
        fun from(value: String?): VaultSort = when (value) {
            UPDATED.storage -> UPDATED
            CREATED.storage -> CREATED
            NAME.storage -> NAME
            else -> PINNED
        }
    }
}

/** 自动填充候选排序：收藏优先，其次最近更新。 */
val FAVORITE_FIRST_COMPARATOR: Comparator<VaultItem> =
    compareByDescending<VaultItem> { it.favorite }.thenByDescending { it.updatedAt }

private val byPinned = compareByDescending<VaultItem> { it.pinned }
    .thenByDescending { it.updatedAt }
    .thenBy { it.name.lowercase() }
    .thenBy { it.id }

private val byUpdated = compareByDescending<VaultItem> { it.updatedAt }
    .thenBy { it.name.lowercase() }
    .thenBy { it.id }

private val byCreated = compareByDescending<VaultItem> { it.createdAt }
    .thenByDescending { it.updatedAt }
    .thenBy { it.name.lowercase() }
    .thenBy { it.id }

private val byName = compareBy<VaultItem> { it.name.lowercase() }
    .thenByDescending { it.updatedAt }
    .thenBy { it.id }

fun sortVaultItems(items: List<VaultItem>, sort: VaultSort): List<VaultItem> =
    items.sortedWith(sort.comparator)
