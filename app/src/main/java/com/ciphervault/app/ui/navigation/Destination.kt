package com.ciphervault.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.ui.graphics.vector.ImageVector
import com.ciphervault.app.R
import com.ciphervault.app.domain.VaultItemType

/** 顶部级导航目标；底栏与路由共用，避免 Int 索引在三个文件里各写一份。 */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    VAULT(Routes.VAULT, R.string.vault, Icons.Outlined.Shield),
    GENERATOR(Routes.GENERATOR, R.string.generator, Icons.Outlined.AutoAwesome),
    SETTINGS(Routes.SETTINGS, R.string.settings, Icons.Outlined.Settings),
}

/** 路由常量与参数构造器；URI 必须与历史值逐字一致。 */
object Routes {
    const val VAULT = "vault"
    const val GENERATOR = "generator"
    const val SETTINGS = "settings"
    const val ITEM = "item/{id}"
    const val EDIT_ITEM = "item/{id}/edit"
    const val NEW_ITEM = "new?type={type}"
    const val SECURITY = "settings/security"
    const val SECURITY_HEALTH = "settings/security-health"
    const val IMPORT_EXPORT = "settings/import-export"
    const val FOLDERS = "settings/folders"
    const val TRASH = "settings/trash"

    fun item(id: String): String = "item/$id"

    fun editItem(id: String): String = "item/$id/edit"

    fun newItem(type: VaultItemType): String = "new?type=${type.storage}"
}
