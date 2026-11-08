package com.ciphervault.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ciphervault.app.AppServices
import com.ciphervault.app.R
import com.ciphervault.app.data.SettingsDefaults
import com.ciphervault.app.domain.AutoLockPolicy
import com.ciphervault.app.ui.biometric.BiometricEnroller
import com.ciphervault.app.ui.components.BiometricHelper
import com.ciphervault.app.ui.components.ObsidianScaffold
import com.ciphervault.app.ui.components.ChoiceDialog
import com.ciphervault.app.ui.components.ObsidianTopBar
import com.ciphervault.app.ui.components.SecurityFooter
import com.ciphervault.app.ui.components.SettingRow
import com.ciphervault.app.ui.components.SwitchRow
import com.ciphervault.app.ui.components.findFragmentActivity
import kotlinx.coroutines.launch

/** 安全设置：自动锁、剪贴板清理、截屏保护与生物识别开关。 */
@Composable
fun SecuritySettingsScreen(
    container: AppServices,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val meta by container.settingsStore.metaFlow.collectAsStateWithLifecycle(initialValue = null)
    val autoLock by container.settingsStore.autoLockMinutes
        .collectAsStateWithLifecycle(initialValue = SettingsDefaults.AUTO_LOCK_MINUTES)
    val clipboardSeconds by container.settingsStore.clipboardClearSeconds
        .collectAsStateWithLifecycle(initialValue = SettingsDefaults.CLIPBOARD_CLEAR_SECONDS)
    val screenshot by container.settingsStore.screenshotProtection
        .collectAsStateWithLifecycle(initialValue = SettingsDefaults.SCREENSHOT_PROTECTION)
    var showAutoLock by remember { mutableStateOf(false) }
    var showClipboard by remember { mutableStateOf(false) }
    var biometricBusy by remember { mutableStateOf(false) }
    val biometricEnabled = meta?.biometricEnabled == true
    val autoLockLabel = when (AutoLockPolicy.mode(autoLock)) {
        AutoLockPolicy.Mode.NEVER -> stringResource(R.string.auto_lock_never)
        AutoLockPolicy.Mode.IMMEDIATE -> stringResource(R.string.auto_lock_immediate)
        AutoLockPolicy.Mode.AFTER_DELAY -> pluralStringResource(R.plurals.auto_lock_minutes, autoLock, autoLock)
    }
    val clipLabel = if (clipboardSeconds <= 0) {
        stringResource(R.string.auto_lock_never)
    } else {
        stringResource(R.string.clipboard_clear_desc, clipboardSeconds)
    }

    fun enrollBiometric() {
        if (biometricBusy) {
            return
        }
        val activity = context.findFragmentActivity()
        if (activity == null) {
            Toast.makeText(context, context.getString(R.string.biometric_unavailable), Toast.LENGTH_SHORT).show()
            return
        }
        biometricBusy = true
        scope.launch {
            try {
                BiometricEnroller.enroll(
                    activity = activity,
                    keyring = container.keyStoreVault,
                    session = container.sessionManager,
                    onUnavailable = {
                        Toast.makeText(context, context.getString(R.string.biometric_unavailable), Toast.LENGTH_SHORT).show()
                    },
                    onError = {
                        Toast.makeText(context, context.getString(R.string.error_generic), Toast.LENGTH_SHORT).show()
                    },
                )
            } finally {
                biometricBusy = false
            }
        }
    }

    ObsidianScaffold(
        topBar = {
            ObsidianTopBar(
                title = stringResource(R.string.security_settings),
                onBack = onBack,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingRow(
                title = stringResource(R.string.auto_lock),
                subtitle = "${stringResource(R.string.auto_lock_desc)} · $autoLockLabel",
                icon = Icons.Outlined.Lock,
                onClick = { showAutoLock = true },
            )
            SettingRow(
                title = stringResource(R.string.clipboard_clear),
                subtitle = clipLabel,
                icon = Icons.Outlined.Shield,
                onClick = { showClipboard = true },
            )
            SwitchRow(
                title = stringResource(R.string.screenshot_protection),
                subtitle = stringResource(R.string.screenshot_protection_desc),
                checked = screenshot,
                onCheckedChange = { enabled ->
                    scope.launch { container.settingsStore.setScreenshotProtection(enabled) }
                },
            )
            SwitchRow(
                title = stringResource(R.string.biometric),
                subtitle = if (biometricEnabled) stringResource(R.string.enabled) else stringResource(R.string.biometric_desc),
                checked = biometricEnabled,
                onCheckedChange = { wantsEnabled ->
                    if (wantsEnabled) {
                        enrollBiometric()
                    } else {
                        scope.launch { container.sessionManager.disableBiometric() }
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
            SecurityFooter(style = MaterialTheme.typography.labelMedium)
        }
    }

    if (showAutoLock) {
        ChoiceDialog(
            title = stringResource(R.string.auto_lock),
            options = listOf(
                -1 to stringResource(R.string.auto_lock_never),
                0 to stringResource(R.string.auto_lock_immediate),
                1 to pluralStringResource(R.plurals.auto_lock_minutes, 1, 1),
                5 to pluralStringResource(R.plurals.auto_lock_minutes, 5, 5),
                15 to pluralStringResource(R.plurals.auto_lock_minutes, 15, 15),
                30 to pluralStringResource(R.plurals.auto_lock_minutes, 30, 30),
            ),
            selected = autoLock,
            onDismiss = { showAutoLock = false },
            onSelect = { value ->
                scope.launch { container.settingsStore.setAutoLockMinutes(value) }
                showAutoLock = false
            },
        )
    }

    if (showClipboard) {
        ChoiceDialog(
            title = stringResource(R.string.clipboard_clear),
            options = listOf(
                0 to stringResource(R.string.auto_lock_never),
                15 to "15 s",
                30 to "30 s",
                60 to "60 s",
            ),
            selected = clipboardSeconds,
            onDismiss = { showClipboard = false },
            onSelect = { value ->
                scope.launch { container.settingsStore.setClipboardClearSeconds(value) }
                showClipboard = false
            },
        )
    }
}
