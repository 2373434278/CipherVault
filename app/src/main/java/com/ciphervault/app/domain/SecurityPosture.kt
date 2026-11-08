package com.ciphervault.app.domain

/**
 * 设置页展示的安全态势分数。
 *
 * 基础 60 分，生物识别解锁 +20，截屏保护 +20。
 */
object SecurityPosture {
    private const val BASE = 60
    private const val BIOMETRIC_BONUS = 20
    private const val SCREENSHOT_BONUS = 20

    fun score(biometricEnabled: Boolean, screenshotProtection: Boolean): Int =
        BASE +
            (if (biometricEnabled) BIOMETRIC_BONUS else 0) +
            (if (screenshotProtection) SCREENSHOT_BONUS else 0)
}
