package com.ciphervault.app.domain

/**
 * TOTP 配置值对象。
 *
 * 默认值与 `otpauth://` 解析规则集中在这里，避免导入、导出、编辑页各自复制
 * `SHA1 / 6 / 30` 这组默认值。
 */
data class TotpSettings(
    val secret: String = "",
    val algorithm: String = DEFAULT_ALGORITHM,
    val digits: Int = DEFAULT_DIGITS,
    val period: Int = DEFAULT_PERIOD,
) {
    /** 是否配置了密钥（仅密钥非空才算启用 TOTP）。 */
    val isConfigured: Boolean get() = secret.isNotBlank()

    companion object {
        const val DEFAULT_ALGORITHM = "SHA1"
        const val DEFAULT_DIGITS = 6
        const val DEFAULT_PERIOD = 30

        /** 从解析后的 otpauth 数据构造。 */
        fun from(data: OtpauthData): TotpSettings = TotpSettings(
            secret = data.secret,
            algorithm = data.algorithm,
            digits = data.digits,
            period = data.period,
        )

        /**
         * 解析导入值：
         * - 空串 → 默认配置；
         * - 合法 `otpauth://` → 解析出的完整配置；
         * - 其他非空串 → 视为裸 Base32 密钥，算法/位数/周期取默认值。
         */
        fun parse(raw: String): TotpSettings {
            if (raw.isBlank()) return TotpSettings()
            val parsed = OtpauthParser.parse(raw)
            return if (parsed != null) from(parsed) else TotpSettings(secret = raw.trim())
        }
    }
}

/** 把条目上的 TOTP 字段收敛成值对象。 */
fun VaultItem.totpSettings(): TotpSettings = TotpSettings(
    secret = totpSecret,
    algorithm = totpAlgorithm,
    digits = totpDigits,
    period = totpPeriod,
)
