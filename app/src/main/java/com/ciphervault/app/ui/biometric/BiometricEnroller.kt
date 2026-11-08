package com.ciphervault.app.ui.biometric

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.ciphervault.app.R
import com.ciphervault.app.core.port.BiometricKeyringPort
import com.ciphervault.app.session.SessionManager
import com.ciphervault.app.ui.components.BiometricHelper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 生物识别注册流程（模板方法式）：能力检查 → 创建 Cipher → 系统认证 → 持久化包裹密钥。
 *
 * 由主界面首次解锁提示与安全设置页共用，避免两份安全敏感流程各自漂移。
 */
object BiometricEnroller {
    suspend fun enroll(
        activity: FragmentActivity,
        keyring: BiometricKeyringPort,
        session: SessionManager,
        onUnavailable: () -> Unit,
        onError: () -> Unit,
    ) {
        if (!BiometricHelper.canAuthenticate(activity)) {
            onUnavailable()
            return
        }
        val cipher = runCatching { keyring.createEncryptCipher() }.getOrNull()
        if (cipher == null) {
            onError()
            return
        }
        val result = suspendCancellableCoroutine<BiometricPrompt.AuthenticationResult?> { continuation ->
            BiometricHelper.authenticate(
                activity = activity,
                title = activity.getString(R.string.biometric_prompt_title),
                subtitle = activity.getString(R.string.biometric_prompt_subtitle),
                negativeText = activity.getString(R.string.cancel),
                cipher = cipher,
                onSuccess = { continuation.resume(it) },
                onError = { _, _ -> continuation.resume(null) },
            )
        }
        val authenticatedCipher = result?.cryptoObject?.cipher
        if (authenticatedCipher == null) {
            onError()
            return
        }
        if (session.enableBiometricWithCipher(authenticatedCipher).isFailure) {
            onError()
        }
    }
}
