package com.ciphervault.app.session

import com.ciphervault.app.crypto.Crypto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class VaultSessionTest {
    private val settings = FakeSettings()
    private val kdf = FakeKdf()
    private val opener = FakeOpener()
    private val session = VaultSession(opener, KeyEnvelope(kdf, Crypto), settings, Crypto)

    @Test
    fun initializeMovesFromLoadingToNeedsSetupOrLocked() = runTest {
        session.initialize()
        assertEquals(SessionState.NeedsSetup, session.state.value)

        settings.saveMeta(envelopeMeta())
        val second = VaultSession(opener, KeyEnvelope(kdf, Crypto), settings, Crypto)
        second.initialize()
        assertEquals(SessionState.Locked, second.state.value)
    }

    @Test
    fun openPublishesUnlockedAndClosePublishesLocked() = runTest {
        assertTrue(session.openWithKey(byteArrayOf(1, 2, 3)).isSuccess)
        assertTrue(session.state.value is SessionState.Unlocked)
        assertNotNull(session.repository)

        session.close()

        assertEquals(SessionState.Locked, session.state.value)
        assertNull(session.repository)
        assertEquals(1, opener.openCount)
        assertEquals(1, opener.closeCount)
    }

    @Test
    fun closeDuringOpenWaitsForOpenAndNeverPublishesUnlockedAfterLocked() = runBlocking {
        opener.gate = CompletableDeferred()
        val published = CopyOnWriteArrayList<SessionState>()
        val collector = launch { session.state.toList(published) }

        val opening = async { session.openWithKey(byteArrayOf(9)) }
        withTimeout(5_000) { opener.started.await() }
        val closing = async { session.close() }

        opener.gate?.complete(Unit)
        opening.await()
        closing.await()
        withTimeout(5_000) { while (published.size < 3) kotlinx.coroutines.yield() }

        assertEquals(SessionState.Loading, published[0])
        assertTrue(published[1] is SessionState.Unlocked)
        assertEquals(SessionState.Locked, published.last())
        assertEquals(1, opener.closeCount)
        collector.cancel()
    }

    @Test
    fun createPersistsMetaAndUnlocks() = runTest {
        val result = session.create(kdf.newParams(lowRam = false), "master-password".toCharArray())

        assertTrue(result.isSuccess)
        assertTrue(session.state.value is SessionState.Unlocked)
        assertTrue(settings.metaFlow.value != null)
        assertEquals(1, opener.openCount)
    }

    @Test
    fun wrongPasswordLeavesSessionLocked() = runTest {
        session.create(kdf.newParams(lowRam = false), "master-password".toCharArray())
        session.close()

        val result = session.unlockWithPassword("wrong-password".toCharArray())

        assertTrue(result.isFailure)
        assertEquals(SessionState.Locked, session.state.value)
    }

    @Test
    fun withVaultKeyIsAvailableOnlyWhileUnlocked() = runTest {
        session.openWithKey(byteArrayOf(4, 5, 6))
        assertEquals(3, session.withVaultKey { it.size })

        session.close()
        assertNull(session.withVaultKey { it.size })
    }

    @Test
    fun failedOpenWipesKeyAndDoesNotPublishUnlocked() = runTest {
        opener.failWith = IllegalStateException("open failed")
        val key = byteArrayOf(7, 8, 9)

        val result = session.openWithKey(key)

        assertTrue(result.isFailure)
        assertTrue(key.all { it == 0.toByte() })
        assertNull(session.repository)
    }

    private fun envelopeMeta() = KeyEnvelope(kdf, Crypto)
        .create("master-password".toCharArray(), kdf.newParams(lowRam = false))
        .meta
}
