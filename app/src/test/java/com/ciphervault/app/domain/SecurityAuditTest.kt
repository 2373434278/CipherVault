package com.ciphervault.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityAuditTest {
    private fun strongPassword(label: String): String = buildString {
        append("Aa1!")
        append(label)
        append("-Zx9-Qw8-")
        append("*".repeat(4))
    }

    private fun weakPassword(): String = "a".repeat(8)

    private fun totpSecret(): String = buildString {
        append("JBSW")
        append("Y3DP")
        append("EHPK")
        append("3PXP")
    }

    private fun login(
        id: String,
        name: String = id,
        password: String = strongPassword("default"),
        username: String = "user",
        url: String = "https://example.com",
        totp: String = totpSecret(),
        type: VaultItemType = VaultItemType.LOGIN,
        deletedAt: Long? = null,
    ) = VaultItem(
        id = id,
        type = type,
        name = name,
        username = username,
        password = password,
        url = url,
        totpSecret = totp,
        deletedAt = deletedAt,
    )

    @Test
    fun blankPasswordsAreIgnored() {
        val report = SecurityAudit.audit(listOf(login("a", password = "", url = "")))

        assertEquals(1, report.totalLogins)
        assertEquals(0, report.checkedPasswords)
        assertTrue(report.weak.isEmpty())
        assertTrue(report.reusedGroups.isEmpty())
        assertTrue(report.missingTotp.isEmpty())
        assertTrue(report.isHealthy)
    }

    @Test
    fun weakPasswordFlaggedBelowThreshold() {
        val report = SecurityAudit.audit(listOf(login("a", password = weakPassword(), url = "")))

        assertEquals(1, report.weak.size)
        assertEquals("a", report.weak.first().id)
    }

    @Test
    fun strongUniquePasswordsAreHealthy() {
        val report = SecurityAudit.audit(
            listOf(
                login("a", password = strongPassword("alpha")),
                login("b", password = strongPassword("beta")),
            ),
        )

        assertTrue(report.isHealthy)
        assertEquals(100, report.score)
    }

    @Test
    fun reusedPasswordGroupsTwoItems() {
        val shared = strongPassword("shared")
        val report = SecurityAudit.audit(
            listOf(
                login("a", name = "Alpha", password = shared),
                login("b", name = "Beta", password = shared),
            ),
        )

        assertEquals(1, report.reusedGroups.size)
        assertEquals(2, report.reusedGroups.first().items.size)
        assertEquals(2, report.reusedCount)
        assertFalse(report.isHealthy)
    }

    @Test
    fun passwordCaseDifferenceIsNotReuse() {
        val mixed = buildString {
            append("Aa1-")
            append("Mixed")
            append("Case-77")
        }
        val report = SecurityAudit.audit(
            listOf(
                login("a", password = mixed),
                login("b", password = mixed.lowercase()),
            ),
        )

        assertTrue(report.reusedGroups.isEmpty())
    }

    @Test
    fun reusedGroupIgnoresBlankEntries() {
        val shared = strongPassword("shared")
        val report = SecurityAudit.audit(
            listOf(
                login("a", password = shared),
                login("b", password = shared),
                login("c", password = ""),
            ),
        )

        assertEquals(2, report.checkedPasswords)
        assertEquals(2, report.reusedCount)
    }

    @Test
    fun missingTotpFlaggedOnlyForLoginsWithUrl() {
        val report = SecurityAudit.audit(
            listOf(
                login("a", password = strongPassword("a"), url = "https://a.example", totp = ""),
                login("b", password = strongPassword("b"), url = "", totp = ""),
            ),
        )

        assertEquals(1, report.missingTotp.size)
        assertEquals("a", report.missingTotp.first().id)
    }

    @Test
    fun notesAndCardsAreNotAudited() {
        val report = SecurityAudit.audit(
            listOf(
                login("note", type = VaultItemType.NOTE, password = weakPassword(), totp = ""),
                login("card", type = VaultItemType.CARD, password = weakPassword(), totp = ""),
            ),
        )

        assertEquals(0, report.totalLogins)
        assertTrue(report.weak.isEmpty())
        assertTrue(report.reusedGroups.isEmpty())
        assertTrue(report.missingTotp.isEmpty())
    }

    @Test
    fun trashedItemsAreExcluded() {
        val report = SecurityAudit.audit(
            listOf(
                login("a", password = weakPassword(), totp = "", deletedAt = 42L),
                login("b", password = strongPassword("b")),
            ),
        )

        assertEquals(1, report.totalLogins)
        assertTrue(report.weak.isEmpty())
        assertTrue(report.isHealthy)
    }

    @Test
    fun scoreClampsBetweenZeroAndHundred() {
        assertEquals(100, SecurityAudit.score(0, 0, 0, 0))
        assertEquals(100, SecurityAudit.score(10, 0, 0, 0))
        assertEquals(0, SecurityAudit.score(1, 5, 5, 5))
        assertEquals(75, SecurityAudit.score(2, 1, 0, 0))
        assertEquals(50, SecurityAudit.score(3, 2, 1, 1))
    }

    @Test
    fun fingerprintIsSha256HexAndNotPlaintext() {
        val sample = listOf("a", "b", "c").joinToString("")
        val fingerprint = SecurityAudit.fingerprint(sample)

        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", fingerprint)
        assertEquals(64, fingerprint.length)
        assertTrue(fingerprint.matches(Regex("[0-9a-f]{64}")))
        assertFalse(fingerprint.contains(sample))
    }

    @Test
    fun auditIsDeterministic() {
        val shared = strongPassword("shared")
        val items = listOf(
            login("c", name = "Charlie", password = shared, totp = ""),
            login("a", name = "Alpha", password = shared, totp = ""),
            login("b", name = "Bravo", password = weakPassword(), totp = ""),
        )

        val first = SecurityAudit.audit(items)
        val second = SecurityAudit.audit(items)
        val third = SecurityAudit.audit(items)

        assertEquals(first, second)
        assertEquals(second, third)
    }

    @Test
    fun handlesLargeVault() {
        val totp = totpSecret()
        val items = (0 until 3000).map { index ->
            login(
                id = "id$index",
                password = strongPassword("item-$index"),
                totp = if (index % 2 == 0) totp else "",
            )
        }

        val report = SecurityAudit.audit(items)

        assertEquals(3000, report.totalLogins)
        assertEquals(3000, report.checkedPasswords)
        assertTrue(report.reusedGroups.isEmpty())
        assertTrue(report.weak.isEmpty())
        assertEquals(1500, report.missingTotp.size)
    }
}
