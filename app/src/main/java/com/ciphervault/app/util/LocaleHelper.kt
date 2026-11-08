package com.ciphervault.app.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * 语言偏好与应用上下文包装。
 *
 * 语言值单独存放在 SharedPreferences 中（未迁移到 DataStore）；[wrap] 会修改进程级
 * 默认 Locale，因此必须在 `attachBaseContext` 阶段调用。
 */
object LocaleHelper {
    private const val PREFS = "ciphervault_prefs"
    private const val KEY_LANGUAGE = "language"
    const val SYSTEM = "system"
    const val CHINESE = "zh"
    const val ENGLISH = "en"

    /** 读取语言偏好；未设置时为跟随系统。 */
    fun getLanguage(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, SYSTEM) ?: SYSTEM

    /** 保存语言偏好；调用方负责重建 Activity 以生效。 */
    fun setLanguage(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LANGUAGE, value).apply()
    }

    fun wrap(base: Context): Context {
        val language = getLanguage(base)
        if (language == SYSTEM) return base
        val locale = Locale.forLanguageTag(if (language == CHINESE) "zh-CN" else "en")
        // 同步进程级默认 Locale，保证日期/数字等系统格式化也跟随应用语言。
        Locale.setDefault(locale)
        LocaleList.setDefault(LocaleList(locale))
        val configuration = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLocales(LocaleList(locale))
        }
        return base.createConfigurationContext(configuration)
    }
}
