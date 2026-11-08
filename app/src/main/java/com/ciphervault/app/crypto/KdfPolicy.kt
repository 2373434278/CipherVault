package com.ciphervault.app.crypto

/**
 * KDF 选择策略。
 *
 * 新建保险库与加密备份都优先使用 Argon2id；当 native 库不可用时回退 PBKDF2。
 * 参数本身仍由 [Kdf.newParams] 提供，避免调参影响历史数据。
 */
object KdfPolicy {
    fun forNewVault(lowRam: Boolean): KdfParams = Kdf.newParams(lowRam).copy(id = preferredId())

    fun forBackup(): KdfParams = Kdf.newParams(lowRam = false).copy(id = preferredId())

    private fun preferredId(): String = if (Kdf.argon2Available()) Kdf.ARGON2ID else Kdf.PBKDF2
}
