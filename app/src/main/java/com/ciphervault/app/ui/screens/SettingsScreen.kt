package com.ciphervault.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ciphervault.app.AppServices
import com.ciphervault.app.R
import com.ciphervault.app.data.SettingsDefaults
import com.ciphervault.app.data.VaultRepository
import com.ciphervault.app.domain.AutoLockPolicy
import com.ciphervault.app.domain.SecurityPosture
import com.ciphervault.app.ui.components.GlassCard
import com.ciphervault.app.ui.components.ObsidianBottomBar
import com.ciphervault.app.ui.components.ObsidianScaffold
import com.ciphervault.app.ui.components.ChoiceDialog
import com.ciphervault.app.ui.components.ObsidianTopBar
import com.ciphervault.app.ui.navigation.Routes
import com.ciphervault.app.ui.navigation.TopLevelDestination
import kotlinx.coroutines.launch
import com.ciphervault.app.ui.components.ScoreRing
import com.ciphervault.app.ui.components.SecondaryButton
import com.ciphervault.app.ui.components.SettingRow
import com.ciphervault.app.util.LocaleHelper

/** 设置首页：安全态势、子页面入口、语言与关于。 */
@Composable
fun SettingsScreen(
    container: AppServices,
    repository: VaultRepository,
    onNavigate: (String) -> Unit,
    onTab: (TopLevelDestination) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val meta by container.settingsStore.metaFlow.collectAsStateWithLifecycle(initialValue = null)
    val trash by repository.trash.collectAsStateWithLifecycle(initialValue = emptyList())
    val folders by repository.folders.collectAsStateWithLifecycle(initialValue = emptyList())
    val autoLock by container.settingsStore.autoLockMinutes
        .collectAsStateWithLifecycle(initialValue = SettingsDefaults.AUTO_LOCK_MINUTES)
    val screenshot by container.settingsStore.screenshotProtection
        .collectAsStateWithLifecycle(initialValue = SettingsDefaults.SCREENSHOT_PROTECTION)
    var autofillEnabled by remember { mutableStateOf(isAutofillEnabled(context)) }
    var showLanguage by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf(LocaleHelper.getLanguage(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) autofillEnabled = isAutofillEnabled(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val biometric = meta?.biometricEnabled == true
    val score = SecurityPosture.score(biometric, screenshot)
    val autoLockLabel = when (AutoLockPolicy.mode(autoLock)) {
        AutoLockPolicy.Mode.NEVER -> stringResource(R.string.auto_lock_never)
        AutoLockPolicy.Mode.IMMEDIATE -> stringResource(R.string.auto_lock_immediate)
        AutoLockPolicy.Mode.AFTER_DELAY -> pluralStringResource(R.plurals.auto_lock_minutes, autoLock, autoLock)
    }
    val languageLabel = when (language) {
        LocaleHelper.CHINESE -> stringResource(R.string.language_zh)
        LocaleHelper.ENGLISH -> stringResource(R.string.language_en)
        else -> stringResource(R.string.language_system)
    }

    ObsidianScaffold(
        topBar = {
            ObsidianTopBar(
                title = stringResource(R.string.settings),
                subtitle = stringResource(R.string.settings_subtitle),
            )
        },
        bottomBar = { ObsidianBottomBar(selected = TopLevelDestination.SETTINGS, onSelect = onTab) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ScoreRing(score)
                    Column {
                        Text(
                            text = stringResource(R.string.security_score, score),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(if (biometric && screenshot) R.string.security_good else R.string.security_basic),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            SettingRow(
                title = stringResource(R.string.security_health),
                subtitle = stringResource(R.string.security_health_desc),
                icon = Icons.Outlined.HealthAndSafety,
                onClick = { onNavigate(Routes.SECURITY_HEALTH) },
            )
            Spacer(Modifier.height(4.dp))
            SettingRow(
                title = stringResource(R.string.security_unlock),
                subtitle = stringResource(R.string.security_unlock_desc, autoLockLabel),
                icon = Icons.Outlined.Shield,
                onClick = { onNavigate(Routes.SECURITY) },
            )
            SettingRow(
                title = stringResource(R.string.autofill),
                subtitle = if (autofillEnabled) stringResource(R.string.autofill_on) else stringResource(R.string.autofill_off),
                icon = Icons.Outlined.Key,
                onClick = { openAutofillSettings(context) },
            )
            SettingRow(
                title = stringResource(R.string.import_export),
                subtitle = stringResource(R.string.import_export_desc),
                icon = Icons.Outlined.UploadFile,
                onClick = { onNavigate(Routes.IMPORT_EXPORT) },
            )
            SettingRow(
                title = stringResource(R.string.folders),
                subtitle = stringResource(R.string.folders_desc) + " · ${folders.size}",
                icon = Icons.Outlined.Folder,
                onClick = { onNavigate(Routes.FOLDERS) },
            )
            SettingRow(
                title = stringResource(R.string.trash),
                subtitle = pluralStringResource(R.plurals.trash_desc, trash.size, trash.size),
                icon = Icons.Outlined.Delete,
                onClick = { onNavigate(Routes.TRASH) },
            )
            SettingRow(
                title = stringResource(R.string.language),
                subtitle = languageLabel,
                icon = Icons.Outlined.Language,
                onClick = { showLanguage = true },
            )
            SettingRow(
                title = stringResource(R.string.about),
                subtitle = stringResource(R.string.about_desc),
                icon = Icons.Outlined.Info,
                onClick = { showAbout = true },
            )
            Spacer(Modifier.height(8.dp))
            SecondaryButton(
                text = stringResource(R.string.lock_now),
                icon = Icons.Outlined.Lock,
                onClick = { scope.launch { container.sessionManager.lock() } },
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "CipherVault v1.0.0 · local-first",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showLanguage) {
        ChoiceDialog(
            title = stringResource(R.string.language),
            options = listOf(
                LocaleHelper.SYSTEM to stringResource(R.string.language_system),
                LocaleHelper.CHINESE to stringResource(R.string.language_zh),
                LocaleHelper.ENGLISH to stringResource(R.string.language_en),
            ),
            selected = language,
            onDismiss = { showLanguage = false },
            onSelect = { value ->
                LocaleHelper.setLanguage(context, value)
                language = value
                showLanguage = false
                (context as? androidx.fragment.app.FragmentActivity)?.recreate()
            },
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.about)) },
            text = { Text(stringResource(R.string.about_body)) },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }
}

/** 检查系统是否已选择本应用作为 Autofill 服务。 */
internal fun isAutofillEnabled(context: Context): Boolean {
    val value = Settings.Secure.getString(context.contentResolver, "autofill_service")
    return value?.startsWith(context.packageName) == true
}

/** 打开系统 Autofill 服务选择页；部分 ROM 无此页面时静默失败。 */
internal fun openAutofillSettings(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
        .setData(Uri.parse("package:${context.packageName}"))
    runCatching { context.startActivity(intent) }
        .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }
}
