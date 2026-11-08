package com.ciphervault.app.domain

import java.net.URLEncoder

/**
 * `otpauth://` URI 构建器。
 *
 * 输出顺序与参数名是 CSV 导出格式的一部分，必须与 [OtpauthParser] 保持可逆。
 */
object OtpauthUri {
    fun build(label: String, settings: TotpSettings): String {
        val encodedLabel = URLEncoder.encode(label, Charsets.UTF_8.name())
        val encodedSecret = URLEncoder.encode(settings.secret, Charsets.UTF_8.name())
        return "otpauth://totp/$encodedLabel" +
            "?secret=$encodedSecret" +
            "&algorithm=${settings.algorithm}" +
            "&digits=${settings.digits}" +
            "&period=${settings.period}"
    }
}
