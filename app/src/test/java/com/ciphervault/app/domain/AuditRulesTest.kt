package com.ciphervault.app.domain

import com.ciphervault.app.domain.audit.AuditContext
import com.ciphervault.app.domain.audit.AuditFinding
import com.ciphervault.app.domain.audit.MissingTotpRule
import com.ciphervault.app.domain.audit.ReusedPasswordRule
import com.ciphervault.app.domain.audit.SecurityScore
import com.ciphervault.app.domain.audit.WeakPasswordRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditRulesTest {
    private fun login(
        id: String,
        password: String,
        url: String = "https://example.com",
        totp: String = "JBSWY3DPEHPK3PXP",
    ) = VaultItem(
        id = id,
        type = VaultItemType.LOGIN,
        name = id,
        password = password,
        url = url,
        totpSecret = totp,
    )

    private fun strong(label: String) = "Aa1!$label-Zx9-Qw8-****"

    @Test
    fun weakRuleFindsOnlyLowStrengthPasswords() {
        val items = listOf(
            login("weak", "aaaaaaaa", url = ""),
            login("strong", strong("strong"), url = ""),
            login("blank", "", url = ""),
            login("note", "aaaaaaaa", url = "").copy(type = VaultItemType.NOTE),
        )
        val logins = items.filter { it.type == VaultItemType.LOGIN && it.deletedAt == null }
        val audited = logins.filter { it.password.isNotBlank() }

        val finding = WeakPasswordRule.evaluate(AuditContext(logins, audited))

        assertEquals(listOf("weak"), finding.items.map { it.id })
    }

    @Test
    fun reusedRuleGroupsOnlyRepeatedNonBlankPasswords() {
        val shared = strong("shared")
        val items = listOf(
            login("a", shared),
            login("b", shared),
            login("c", ""),
            login("d", shared.lowercase()),
        )
        val finding = ReusedPasswordRule.evaluate(AuditContext(items, items.filter { it.password.isNotBlank() }))

        assertEquals(1, finding.groups.size)
        assertEquals(setOf("a", "b"), finding.groups.first().items.map { it.id }.toSet())
    }

    @Test
    fun missingTotpRuleNeedsUrlAndMissingSecret() {
        val items = listOf(
            login("missing", strong("a"), url = "https://a.example", totp = ""),
            login("no-url", strong("b"), url = "", totp = ""),
            login("configured", strong("c"), url = "https://c.example"),
        )
        val finding = MissingTotpRule.evaluate(AuditContext(items, items))

        assertEquals(listOf("missing"), finding.items.map { it.id })
    }

    @Test
    fun scoreFormulaMatchesLegacyValues() {
        assertEquals(6, SecurityScore.MAX_RISK_PER_LOGIN)
        assertEquals(100, SecurityScore.score(0, 0, 0, 0))
        assertEquals(100, SecurityScore.score(10, 0, 0, 0))
        assertEquals(0, SecurityScore.score(1, 5, 5, 5))
        assertEquals(75, SecurityScore.score(2, 1, 0, 0))
        assertEquals(50, SecurityScore.score(3, 2, 1, 1))
        assertEquals(SecurityScore.score(3, 2, 1, 1), SecurityAudit.score(3, 2, 1, 1))
    }

    @Test
    fun combinedReportStillMatchesRuleOutputs() {
        val shared = strong("shared")
        val items = listOf(
            login("a", shared, url = "https://a.example", totp = ""),
            login("b", shared, url = "https://b.example"),
            login("c", "aaaaaaaa", url = "", totp = ""),
        )

        val report = SecurityAudit.audit(items)

        assertEquals(listOf("c"), report.weak.map { it.id })
        assertEquals(1, report.reusedGroups.size)
        assertEquals(listOf("a"), report.missingTotp.map { it.id })
        assertTrue(report.score in 0..100)
    }
}
