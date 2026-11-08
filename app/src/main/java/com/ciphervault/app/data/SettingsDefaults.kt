package com.ciphervault.app.data

/** 设置的默认值；DataStore 与 UI 首帧必须共用，避免开关在首帧跳变。 */
object SettingsDefaults {
    const val AUTO_LOCK_MINUTES = 1
    const val CLIPBOARD_CLEAR_SECONDS = 30
    const val SCREENSHOT_PROTECTION = true
}
