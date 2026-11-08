package com.ciphervault.app.autofill

import android.app.assist.AssistStructure
import android.text.InputType
import android.view.autofill.AutofillId

data class AutofillFields(
    val usernameId: AutofillId? = null,
    val passwordId: AutofillId? = null,
    val otpId: AutofillId? = null,
    val usernameValue: String = "",
    val passwordValue: String = "",
    val otpValue: String = "",
    val webDomain: String? = null,
    val packageName: String = "",
)

/**
 * `AssistStructure` 字段识别器。
 *
 * 启发式顺序：先识别密码框，再识别 OTP，最后把剩余普通文本框归为用户名；
 * 识别结果用于选择填充/保存目标，不参与任何加密逻辑。
 */
object AutofillParser {
    fun parse(structure: AssistStructure): AutofillFields {
        var fields = AutofillFields(
            packageName = structure.activityComponent?.packageName.orEmpty(),
        )
        for (index in 0 until structure.windowNodeCount) {
            val root = structure.getWindowNodeAt(index).rootViewNode
            if (fields.webDomain.isNullOrBlank()) fields = fields.copy(webDomain = root.webDomain)
            fields = walk(root, fields)
        }
        return fields
    }

    private fun walk(node: AssistStructure.ViewNode, initial: AutofillFields): AutofillFields {
        var fields = initial
        val id = node.autofillId
        if (id != null) {
            val value = node.autofillValue?.textValue?.toString().orEmpty()
            when {
                fields.passwordId == null && isPassword(node) ->
                    fields = fields.copy(passwordId = id, passwordValue = value)
                fields.otpId == null && isOtp(node) ->
                    fields = fields.copy(otpId = id, otpValue = value)
                fields.usernameId == null && isUsername(node) ->
                    fields = fields.copy(usernameId = id, usernameValue = value)
                fields.usernameId == null && isPlainText(node) ->
                    fields = fields.copy(usernameId = id, usernameValue = value)
            }
        }
        for (index in 0 until node.childCount) {
            fields = walk(node.getChildAt(index), fields)
        }
        return fields
    }

    // 密码框：autofill hint、文案关键字、inputType variation 三者任一命中即可。
    private fun isPassword(node: AssistStructure.ViewNode): Boolean {
        val hints = node.autofillHints?.map { it.lowercase() }.orEmpty()
        val text = searchableText(node)
        val variation = node.inputType and InputType.TYPE_MASK_VARIATION
        val className = node.inputType and InputType.TYPE_MASK_CLASS
        return hints.any { it.contains("password") } ||
            text.contains("password") || text.contains("passwd") || text.contains("pwd") ||
            (className == InputType.TYPE_CLASS_TEXT && variation in setOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            )) ||
            (className == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
    }

    // OTP：优先 hint 与文案（otp/2fa/verification code）。
    private fun isOtp(node: AssistStructure.ViewNode): Boolean {
        val hints = node.autofillHints?.map { it.lowercase() }.orEmpty()
        val text = searchableText(node)
        return hints.any { it.contains("otp") || it.contains("one_time") } ||
            text.contains("otp") || text.contains("one-time") || text.contains("verification code") ||
            text.contains("2fa") || text.contains("authenticator")
    }

    // 用户名：hint/文案包含 username/email/login，或 inputType 为邮箱变体。
    private fun isUsername(node: AssistStructure.ViewNode): Boolean {
        val hints = node.autofillHints?.map { it.lowercase() }.orEmpty()
        val text = searchableText(node)
        val variation = node.inputType and InputType.TYPE_MASK_VARIATION
        val className = node.inputType and InputType.TYPE_MASK_CLASS
        return hints.any {
            it.contains("username") || it.contains("email") || it.contains("login")
        } ||
            text.contains("email") || text.contains("username") || text.contains("user name") ||
            text.contains("account") || text.contains("login") ||
            (className == InputType.TYPE_CLASS_TEXT && variation in setOf(
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            ))
    }

    private fun isPlainText(node: AssistStructure.ViewNode): Boolean {
        val className = node.inputType and InputType.TYPE_MASK_CLASS
        val variation = node.inputType and InputType.TYPE_MASK_VARIATION
        return className == InputType.TYPE_CLASS_TEXT &&
            variation !in setOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            )
    }

    private fun searchableText(node: AssistStructure.ViewNode): String = buildString {
        append(node.hint?.toString().orEmpty())
        append(' ')
        append(node.idEntry?.toString().orEmpty())
        append(' ')
        append(node.contentDescription?.toString().orEmpty())
    }.lowercase()
}
