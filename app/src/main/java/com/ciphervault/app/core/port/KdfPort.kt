package com.ciphervault.app.core.port

import com.ciphervault.app.crypto.KdfParams

/** 主密码派生端口；实现必须保持现有 Argon2id/PBKDF2 参数。 */
interface KdfPort {
    fun newParams(lowRam: Boolean): KdfParams

    fun derive(password: CharArray, params: KdfParams): ByteArray

    fun argon2Available(): Boolean
}
