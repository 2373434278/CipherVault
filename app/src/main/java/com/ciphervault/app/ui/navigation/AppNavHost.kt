package com.ciphervault.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ciphervault.app.AppServices
import com.ciphervault.app.data.VaultRepository
import com.ciphervault.app.domain.VaultItemType
import com.ciphervault.app.ui.screens.FoldersScreen
import com.ciphervault.app.ui.screens.GeneratorScreen
import com.ciphervault.app.ui.screens.ImportExportScreen
import com.ciphervault.app.ui.screens.ItemDetailScreen
import com.ciphervault.app.ui.screens.ItemEditScreen
import com.ciphervault.app.ui.screens.SecurityHealthScreen
import com.ciphervault.app.ui.screens.SecuritySettingsScreen
import com.ciphervault.app.ui.screens.SettingsScreen
import com.ciphervault.app.ui.screens.TrashScreen
import com.ciphervault.app.ui.screens.VaultScreen

/**
 * 已解锁状态下的导航图。
 *
 * [repository] 的生命周期严格等于本次解锁会话；锁定后整棵导航树从组合中移除。
 */
@Composable
fun AppNavHost(container: AppServices, repository: VaultRepository) {
    val navController = rememberNavController()

    fun navigateTab(destination: TopLevelDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.VAULT) {
        composable(Routes.VAULT) {
            VaultScreen(
                container = container,
                repository = repository,
                onOpenItem = { id -> navController.navigate(Routes.item(id)) },
                onEditItem = { id -> navController.navigate(Routes.editItem(id)) },
                onAdd = { navController.navigate(Routes.newItem(VaultItemType.LOGIN)) },
                onTab = ::navigateTab,
            )
        }
        composable(Routes.GENERATOR) {
            GeneratorScreen(clipboard = container.clipboardHelper, onTab = ::navigateTab)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                container = container,
                repository = repository,
                onNavigate = { route -> navController.navigate(route) },
                onTab = ::navigateTab,
            )
        }
        composable(
            route = Routes.ITEM,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id").orEmpty()
            ItemDetailScreen(
                repository = repository,
                clipboard = container.clipboardHelper,
                itemId = id,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.editItem(id)) },
            )
        }
        composable(
            route = Routes.EDIT_ITEM,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id").orEmpty()
            ItemEditScreen(
                repository = repository,
                itemId = id,
                initialType = null,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.NEW_ITEM,
            arguments = listOf(
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = VaultItemType.LOGIN.storage
                },
            ),
        ) { backStackEntry ->
            val type = VaultItemType.from(backStackEntry.arguments?.getString("type"))
            ItemEditScreen(
                repository = repository,
                itemId = null,
                initialType = type,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(Routes.SECURITY) {
            SecuritySettingsScreen(container = container, onBack = { navController.popBackStack() })
        }
        composable(Routes.SECURITY_HEALTH) {
            SecurityHealthScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onOpenItem = { id -> navController.navigate(Routes.item(id)) },
            )
        }
        composable(Routes.IMPORT_EXPORT) {
            ImportExportScreen(repository = repository, onBack = { navController.popBackStack() })
        }
        composable(Routes.FOLDERS) {
            FoldersScreen(repository = repository, onBack = { navController.popBackStack() })
        }
        composable(Routes.TRASH) {
            TrashScreen(repository = repository, onBack = { navController.popBackStack() })
        }
    }
}
