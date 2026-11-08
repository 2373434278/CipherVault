package com.ciphervault.app.domain.audit

/**
 * 安全体检总分计算。
 *
 * 公式：`100 - 风险 * 100 / (登录项总数 * 单项最大风险)`，结果裁剪到 0..100。
 * 单项最大风险 = 3（弱口令）+ 2（重复使用）+ 1（缺少 TOTP）= 6。
 */
object SecurityScore {
    const val WEAK_WEIGHT = 3
    const val REUSED_WEIGHT = 2
    const val MISSING_TOTP_WEIGHT = 1
    const val MAX_RISK_PER_LOGIN = WEAK_WEIGHT + REUSED_WEIGHT + MISSING_TOTP_WEIGHT

    fun score(totalLogins: Int, weak: Int, reused: Int, missingTotp: Int): Int {
        if (totalLogins <= 0) return 100
        val risk = weak * WEAK_WEIGHT + reused * REUSED_WEIGHT + missingTotp * MISSING_TOTP_WEIGHT
        val maxRisk = totalLogins * MAX_RISK_PER_LOGIN
        return (100 - risk * 100 / maxRisk).coerceIn(0, 100)
    }
}
