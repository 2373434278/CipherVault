package com.ciphervault.app

import android.app.ActivityManager
import android.content.Context
import com.ciphervault.app.core.port.BiometricKeyringPort
import com.ciphervault.app.core.port.ClipboardGateway
import com.ciphervault.app.core.port.SettingsGateway
import com.ciphervault.app.crypto.KeyStoreVault
import com.ciphervault.app.data.RoomVaultStore
import com.ciphervault.app.data.SettingsStore
import com.ciphervault.app.session.SessionManager
import com.ciphervault.app.util.ClipboardHelper

/**
 * 手工装配根。
 *
 * 把具体实现绑定到 [AppServices] 的窄接口与端口上；业务层不直接依赖本类。
 */
class AppContainer(context: Context) : AppServices {
    private val appContext = context.applicationContext
    private val sqlCipherReady: Boolean = runCatching { System.loadLibrary("sqlcipher") }.isSuccess

    override val settingsStore: SettingsGateway = SettingsStore(appContext)
    override val keyStoreVault: BiometricKeyringPort = KeyStoreVault()
    override val clipboardHelper: ClipboardGateway = ClipboardHelper(appContext, settingsStore)

    override val sessionManager = SessionManager(
        settings = settingsStore,
        keyring = keyStoreVault,
        opener = RoomVaultStore(appContext, sqlCipherReady),
        lowRamDevice = (appContext.getSystemService(ActivityManager::class.java))?.isLowRamDevice == true,
    )
}
