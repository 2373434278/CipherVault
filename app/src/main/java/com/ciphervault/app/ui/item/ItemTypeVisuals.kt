package com.ciphervault.app.ui.item

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import com.ciphervault.app.R
import com.ciphervault.app.domain.VaultItemType

/** 条目类型的图标、强调色与名称资源，列表页与详情页共用。 */
object ItemTypeVisuals {
    fun icon(type: VaultItemType): ImageVector = when (type) {
        VaultItemType.LOGIN -> Icons.Outlined.Key
        VaultItemType.CARD -> Icons.Outlined.CreditCard
        VaultItemType.NOTE -> Icons.AutoMirrored.Outlined.StickyNote2
        VaultItemType.IDENTITY -> Icons.Outlined.Person
    }

    fun accent(type: VaultItemType): Color = when (type) {
        VaultItemType.LOGIN -> Color(0xFF35E0B0)
        VaultItemType.CARD -> Color(0xFF7CC7FF)
        VaultItemType.NOTE -> Color(0xFFE2B86B)
        VaultItemType.IDENTITY -> Color(0xFFC89BFF)
    }

    @StringRes
    fun labelRes(type: VaultItemType): Int = when (type) {
        VaultItemType.LOGIN -> R.string.logins
        VaultItemType.CARD -> R.string.cards
        VaultItemType.NOTE -> R.string.notes
        VaultItemType.IDENTITY -> R.string.identities
    }

    @Composable
    fun label(type: VaultItemType): String = stringResource(labelRes(type))
}

/** UI 展示派生值；副标题逻辑集中在领域层，便于单测。 */
object ItemPresentation {
    fun subtitle(item: com.ciphervault.app.domain.VaultItem): String =
        com.ciphervault.app.domain.VaultItemSubtitle.of(item)
}
