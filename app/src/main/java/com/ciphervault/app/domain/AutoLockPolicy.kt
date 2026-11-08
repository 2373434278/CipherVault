package com.ciphervault.app.domain

/**
 * 自动锁策略。
 *
 * 存储值语义：负数 = 永不自动锁；0 = 退到后台立即锁；正数 = 延迟分钟数。
 */
object AutoLockPolicy {
    const val NEVER = -1
    const val IMMEDIATE = 0

    enum class Mode { NEVER, IMMEDIATE, AFTER_DELAY }

    fun mode(minutes: Int): Mode = when {
        minutes < NEVER -> Mode.NEVER
        minutes < 0 -> Mode.NEVER
        minutes == 0 -> Mode.IMMEDIATE
        else -> Mode.AFTER_DELAY
    }

    /** 是否需要在前台生命周期 `ON_STOP` 时立即锁定。 */
    fun shouldLockOnStop(minutes: Int): Boolean = minutes == 0

    /** 延迟锁定的毫秒数；立即锁与永不锁返回 null。 */
    fun delayMillis(minutes: Int): Long? = if (minutes > 0) minutes * 60_000L else null
}
