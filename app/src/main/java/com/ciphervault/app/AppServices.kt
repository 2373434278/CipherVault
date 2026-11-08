package com.ciphervault.app

import com.ciphervault.app.core.port.BiometricKeyringPort
import com.ciphervault.app.core.port.ClipboardGateway
import com.ciphervault.app.core.port.SettingsGateway
import com.ciphervault.app.session.SessionManager

/**
 * 应用级服务集合。
 *
 * UI 与服务只依赖这些窄接口/会话门面，不再直接依赖 [AppContainer] 的具体实现。
 */
interface AppServices {
    val settingsStore: SettingsGateway
    val keyStoreVault: BiometricKeyringPort
    val sessionManager: SessionManager
    val clipboardHelper: ClipboardGateway
}
