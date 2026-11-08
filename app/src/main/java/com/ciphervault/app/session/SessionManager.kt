package com.ciphervault.app.session

import android.util.Log
import com.ciphervault.app.core.port.AeadPort
import com.ciphervault.app.core.port.BiometricKeyringPort
import com.ciphervault.app.core.port.KdfPort
import com.ciphervault.app.core.port.SettingsGateway
import com.ciphervault.app.core.port.VaultStoreOpener
import com.ciphervault.app.crypto.Crypto
import com.ciphervault.app.crypto.Kdf
import com.ciphervault.app.crypto.KdfPolicy
import com.ciphervault.app.data.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher

/**
 * 会话门面。
 *
 * 对外保持原有 API，内部委托 [VaultSession]（状态与资源生命周期）、[KeyEnvelope]
 * （主密码信封）与 [BiometricUnlock]（生物识别）。
 *
 * 线程/锁约定：所有会改变会话状态的方法都是 `suspend`，由 [VaultSession] 的单把
 * Mutex 串行化；`lock()` 也必须是 `suspend`，不得在生命周期回调线程直接关闭数据库。
 */
class SessionManager(
    private val settings: SettingsGateway,
    private val keyring: BiometricKeyringPort,
    opener: VaultStoreOpener,
    private val lowRamDevice: Boolean,
    private val aead: AeadPort = Crypto,
    kdf: KdfPort = Kdf,
) {
    private val keyEnvelope = KeyEnvelope(kdf, aead)
    private val biometricUnlock = BiometricUnlock(keyring, settings, aead)
    private val vaultSession = VaultSession(opener, keyEnvelope, settings, aead)

    val state: StateFlow<SessionState> = vaultSession.state

    private val _pendingBiometricEnrollment = MutableStateFlow(false)
    val pendingBiometricEnrollment: StateFlow<Boolean> = _pendingBiometricEnrollment.asStateFlow()

    suspend fun initialize() = vaultSession.initialize()

    suspend fun createVault(password: CharArray, requestBiometric: Boolean): Result<Unit> {
        val result = runCatching {
            require(password.size >= 8) { "Master password must be at least 8 characters" }
            vaultSession.create(KdfPolicy.forNewVault(lowRamDevice), password).getOrThrow()
        }
        if (result.isFailure) {
            Log.e(TAG, "Failed to create vault", result.exceptionOrNull())
            vaultSession.markNeedsSetup()
        } else {
            _pendingBiometricEnrollment.value = requestBiometric
        }
        aead.wipe(password)
        return result
    }

    suspend fun unlockWithPassword(password: CharArray): Result<Unit> {
        val result = vaultSession.unlockWithPassword(password)
        val error = result.exceptionOrNull()
        if (error != null) {
            if (error is AEADBadTagException || error.cause is AEADBadTagException) {
                Log.w(TAG, "Master password rejected")
            } else {
                Log.e(TAG, "Unlock failed", error)
            }
        }
        aead.wipe(password)
        return result
    }

    suspend fun prepareBiometricCipher(): Cipher? = biometricUnlock.prepareCipher()

    suspend fun completeBiometricUnlock(cipher: Cipher): Result<Unit> {
        val result = biometricUnlock.unwrap(cipher).fold(
            onSuccess = { key -> vaultSession.openWithKey(key) },
            onFailure = { Result.failure(it) },
        )
        result.exceptionOrNull()?.let { Log.e(TAG, "Biometric unlock failed", it) }
        return result
    }

    suspend fun enableBiometricWithCipher(cipher: Cipher): Result<Unit> {
        val result = vaultSession.withVaultKey { key -> biometricUnlock.enable(cipher, key) }
            ?: Result.failure(IllegalStateException("Vault is locked"))
        result.exceptionOrNull()?.let { Log.e(TAG, "Failed to enable biometric unlock", it) }
        if (result.isSuccess) _pendingBiometricEnrollment.value = false
        return result
    }

    suspend fun disableBiometric() {
        biometricUnlock.disable()
        _pendingBiometricEnrollment.value = false
    }

    fun dismissBiometricEnrollment() {
        _pendingBiometricEnrollment.value = false
    }

    fun currentRepository(): VaultRepository? = vaultSession.repository

    suspend fun lock() = vaultSession.close()

    private companion object {
        const val TAG = "SessionManager"
    }
}
