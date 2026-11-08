package com.ciphervault.app.autofill

import com.ciphervault.app.data.VaultRepository
import com.ciphervault.app.session.FakeVaultDao
import com.ciphervault.app.session.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillStrategiesTest {
    @Test
    fun domainResolverPrefersWebDomainAndBuildsHttpsUrl() {
        val web = AutofillFields(webDomain = "example.com", packageName = "com.example")
        val app = AutofillFields(webDomain = null, packageName = "com.example")

        assertEquals("example.com", AutofillDomainResolver.resolve(web))
        assertEquals("com.example", AutofillDomainResolver.resolve(app))
        assertEquals("https://example.com", AutofillDomainResolver.urlFor(web))
        assertEquals("", AutofillDomainResolver.urlFor(app))
        assertEquals("Autofill", AutofillDomainResolver.displayName(""))
    }

    @Test
    fun repositoryGateUsesUnlockedStateOnly() {
        val repository = VaultRepository(FakeVaultDao())

        val unlocked = AutofillRepositoryGate.snapshot(SessionState.Unlocked(repository))
        assertTrue(unlocked is AutofillRepositoryGate.Result.Unlocked)
        assertEquals(repository, (unlocked as AutofillRepositoryGate.Result.Unlocked).repository)
        assertEquals(AutofillRepositoryGate.Result.Locked, AutofillRepositoryGate.snapshot(SessionState.Locked))
        assertEquals(AutofillRepositoryGate.Result.Locked, AutofillRepositoryGate.snapshot(SessionState.Loading))
        assertEquals(AutofillRepositoryGate.Result.Locked, AutofillRepositoryGate.snapshot(SessionState.NeedsSetup))
    }
}
