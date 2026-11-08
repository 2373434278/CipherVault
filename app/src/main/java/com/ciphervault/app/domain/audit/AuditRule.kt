package com.ciphervault.app.domain.audit

import com.ciphervault.app.domain.AuditItem
import com.ciphervault.app.domain.PasswordGenerator
import com.ciphervault.app.domain.ReusedGroup
import com.ciphervault.app.domain.SecurityAudit
import com.ciphervault.app.domain.VaultItem

/** 安全体检的输入上下文；`audited` 已排除空口令与非登录条目。 */
data class AuditContext(
    val logins: List<VaultItem>,
    val audited: List<VaultItem>,
)

/** 规则输出；保持与 [com.ciphervault.app.domain.SecurityReport] 的字段一一对应。 */
sealed interface AuditFinding {
    data class WeakPasswords(val items: List<AuditItem>) : AuditFinding
    data class ReusedPasswords(val groups: List<ReusedGroup>) : AuditFinding
    data class MissingTotp(val items: List<AuditItem>) : AuditFinding
}

/**
 * 单条安全审计规则。
 *
 * 新增规则只需实现本接口并注册到 [AuditRules.all]；[weight] 参与总分计算。
 */
interface AuditRule {
    val weight: Int
    fun evaluate(context: AuditContext): AuditFinding
}

/** 弱口令：强度不高于 [SecurityAudit.WEAK_STRENGTH_MAX]。 */
object WeakPasswordRule : AuditRule {
    override val weight: Int = SecurityScore.WEAK_WEIGHT

    override fun evaluate(context: AuditContext): AuditFinding.WeakPasswords {
        val items = context.audited
            .map { it.toAuditItem() }
            .filter { it.strength <= SecurityAudit.WEAK_STRENGTH_MAX }
            .sortedWith(
                compareBy<AuditItem> { it.strength }
                    .thenBy { it.name.lowercase() }
                    .thenBy { it.id },
            )
        return AuditFinding.WeakPasswords(items)
    }
}

/** 重复使用：按口令的 SHA-256 指纹分组，大小写与内容敏感。 */
object ReusedPasswordRule : AuditRule {
    override val weight: Int = SecurityScore.REUSED_WEIGHT

    override fun evaluate(context: AuditContext): AuditFinding.ReusedPasswords {
        val byFingerprint = linkedMapOf<String, MutableList<VaultItem>>()
        context.audited.forEach { item ->
            byFingerprint.getOrPut(SecurityAudit.fingerprint(item.password)) { mutableListOf() }.add(item)
        }
        val groups = byFingerprint.values
            .filter { it.size >= 2 }
            .map { group ->
                ReusedGroup(
                    group.map { it.toAuditItem() }
                        .sortedWith(compareBy<AuditItem> { it.name.lowercase() }.thenBy { it.id }),
                )
            }
            .sortedWith(
                compareByDescending<ReusedGroup> { it.items.size }
                    .thenBy { it.items.first().name.lowercase() }
                    .thenBy { it.items.first().id },
            )
        return AuditFinding.ReusedPasswords(groups)
    }
}

/** 缺少 TOTP：有 URL 但未配置密钥的登录项。 */
object MissingTotpRule : AuditRule {
    override val weight: Int = SecurityScore.MISSING_TOTP_WEIGHT

    override fun evaluate(context: AuditContext): AuditFinding.MissingTotp {
        val items = context.audited
            .filter { it.url.isNotBlank() && !it.hasTotp }
            .map { it.toAuditItem() }
            .sortedWith(compareBy<AuditItem> { it.name.lowercase() }.thenBy { it.id })
        return AuditFinding.MissingTotp(items)
    }
}

/** 规则注册表；顺序不影响报告字段，但保持稳定便于测试。 */
object AuditRules {
    val all: List<AuditRule> = listOf(WeakPasswordRule, ReusedPasswordRule, MissingTotpRule)

    fun evaluate(context: AuditContext): List<AuditFinding> = all.map { it.evaluate(context) }
}

internal fun VaultItem.toAuditItem(): AuditItem = AuditItem(
    id = id,
    name = name,
    username = username,
    strength = PasswordGenerator.strength(password),
)
