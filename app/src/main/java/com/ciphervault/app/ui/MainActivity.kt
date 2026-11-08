package com.ciphervault.app.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ciphervault.app.AppServices
import com.ciphervault.app.data.SettingsDefaults
import com.ciphervault.app.di.appServices
import com.ciphervault.app.domain.AutoLockPolicy
import com.ciphervault.app.ui.biometric.BiometricEnroller
import com.ciphervault.app.R
import com.ciphervault.app.session.SessionState
import com.ciphervault.app.ui.components.BiometricHelper
import com.ciphervault.app.ui.components.LogoMark
import com.ciphervault.app.ui.components.obsidianBackground
import com.ciphervault.app.ui.navigation.AppNavHost
import com.ciphervault.app.ui.screens.LockScreen
import com.ciphervault.app.ui.screens.SetupScreen
import com.ciphervault.app.ui.theme.CipherVaultTheme
import com.ciphervault.app.util.LocaleHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 单 Activity 宿主。
 *
 * 负责会话状态路由（建库/锁定/解锁）、截屏保护与自动锁；具体页面由 `AppNavHost` 提供。
 * 锁定态不渲染导航树，因此 UI 状态持有者不会跨锁持有已关闭的数据库。
 */
class MainActivity : FragmentActivity() {
    private val container by lazy { appServices() }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        lifecycleScope.launch { container.sessionManager.initialize() }
        setContent {
            CipherVaultTheme {
                val state by container.sessionManager.state.collectAsStateWithLifecycle()
                val pendingBiometric by container.sessionManager.pendingBiometricEnrollment.collectAsStateWithLifecycle()
                val screenshotProtection by container.settingsStore.screenshotProtection
                    .collectAsStateWithLifecycle(initialValue = SettingsDefaults.SCREENSHOT_PROTECTION)

                LaunchedEffect(screenshotProtection) {
                    if (screenshotProtection) {
                        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                AutoLockEffect(container)

                when (val current = state) {
                    SessionState.Loading -> SplashScreen()
                    SessionState.NeedsSetup -> SetupScreen(container.sessionManager)
                    SessionState.Locked -> LockScreen(container.sessionManager, container.settingsStore)
                    is SessionState.Unlocked -> AppNavHost(
                        container = container,
                        repository = current.repository,
                    )
                }

                if (state is SessionState.Unlocked && pendingBiometric) {
                    AlertDialog(
                        onDismissRequest = container.sessionManager::dismissBiometricEnrollment,
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = { Text(stringResource(R.string.biometric_setup_title)) },
                        text = { Text(stringResource(R.string.biometric_setup_body)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    container.sessionManager.dismissBiometricEnrollment()
                                    enrollBiometric()
                                },
                            ) {
                                Text(stringResource(R.string.enable))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = container.sessionManager::dismissBiometricEnrollment) {
                                Text(stringResource(R.string.not_now))
                            }
                        },
                    )
                }
            }
        }
    }

    private fun enrollBiometric() {
        lifecycleScope.launch {
            BiometricEnroller.enroll(
                activity = this@MainActivity,
                keyring = container.keyStoreVault,
                session = container.sessionManager,
                onUnavailable = {
                    Toast.makeText(this@MainActivity, getString(R.string.biometric_unavailable), Toast.LENGTH_SHORT).show()
                },
                onError = {
                    Toast.makeText(this@MainActivity, getString(R.string.error_generic), Toast.LENGTH_SHORT).show()
                },
            )
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().obsidianBackground(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LogoMark(size = 76)
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.splash_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** 前台生命周期观察者：按 [AutoLockPolicy] 在退到后台时立即或延迟锁定。 */
@Composable
private fun AutoLockEffect(container: AppServices) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val autoLockMinutes by container.settingsStore.autoLockMinutes
        .collectAsStateWithLifecycle(initialValue = SettingsDefaults.AUTO_LOCK_MINUTES)

    DisposableEffect(lifecycleOwner, autoLockMinutes) {
        var pending: Job? = null
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (container.sessionManager.state.value is SessionState.Unlocked) {
                        when (AutoLockPolicy.mode(autoLockMinutes)) {
                            AutoLockPolicy.Mode.NEVER -> Unit
                            AutoLockPolicy.Mode.IMMEDIATE -> scope.launch { container.sessionManager.lock() }
                            AutoLockPolicy.Mode.AFTER_DELAY -> pending = scope.launch {
                                delay(AutoLockPolicy.delayMillis(autoLockMinutes) ?: return@launch)
                                container.sessionManager.lock()
                            }
                        }
                    }
                }
                Lifecycle.Event.ON_START -> {
                    pending?.cancel()
                    pending = null
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            pending?.cancel()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
