package com.ciphervault.app.domain

import com.ciphervault.app.domain.audit.AuditContext
import com.ciphervault.app.domain.audit.AuditFinding
import com.ciphervault.app.domain.audit.AuditRules
import com.ciphervault.app.domain.audit.SecurityScore
import java.security.MessageDigest

data class AuditItem(
    val id: String,
    val name: String,
    val username: String,
    val strength: Int,
)

data class ReusedGroup(val items: List<AuditItem>)

data class SecurityReport(
    val totalLogins: Int,
    val checkedPasswords: Int,
    val weak: List<AuditItem>,
    val reusedGroups: List<ReusedGroup>,
    val missingTotp: List<AuditItem>,
) {
    val reusedCount: Int = reusedGroups.sumOf { it.items.size }
    val issueCount: Int = weak.size + reusedCount + missingTotp.size
    val score: Int = SecurityAudit.score(totalLogins, weak.size, reusedCount, missingTotp.size)
    val isHealthy: Boolean = issueCount == 0
}

object SecurityAudit {
    const val WEAK_STRENGTH_MAX = 1

    private const val HEX = "0123456789abcdef"

    fun audit(items: List<VaultItem>): SecurityReport {
        val logins = items.filter { it.type == VaultItemType.LOGIN && it.deletedAt == null }
        val audited = logins.filter { it.password.isNotBlank() }

        // 规则各自负责筛选与排序，这里只做结果归位，保持报告字段稳定。
        val findings = AuditRules.evaluate(AuditContext(logins, audited))
        val weak = findings.filterIsInstance<AuditFinding.WeakPasswords>().single().items
        val reusedGroups = findings.filterIsInstance<AuditFinding.ReusedPasswords>().single().groups
        val missingTotp = findings.filterIsInstance<AuditFinding.MissingTotp>().single().items

        return SecurityReport(
            totalLogins = logins.size,
            checkedPasswords = audited.size,
            weak = weak,
            reusedGroups = reusedGroups,
            missingTotp = missingTotp,
        )
    }

    internal fun fingerprint(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray(Charsets.UTF_8))
        return buildString(digest.size * 2) {
            digest.forEach { byte ->
                val value = byte.toInt() and 0xff
                append(HEX[value ushr 4])
                append(HEX[value and 0x0f])
            }
        }
    }

    internal fun score(totalLogins: Int, weak: Int, reused: Int, missingTotp: Int): Int =
        SecurityScore.score(totalLogins, weak, reused, missingTotp)
}
