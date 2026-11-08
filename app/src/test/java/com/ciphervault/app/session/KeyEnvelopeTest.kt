package com.ciphervault.app.session

import com.ciphervault.app.crypto.Crypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.AEADBadTagException

class KeyEnvelopeTest {
    private val kdf = FakeKdf()
    private val envelope = KeyEnvelope(kdf, Crypto)

    @Test
    fun createAndUnwrapRoundTripAndWipeKek() {
        val password = "master-password".toCharArray()
        val sealed = envelope.create(password, kdf.newParams(lowRam = false))

        assertTrue(sealed.meta.kdf.id == "fake-kdf")
        assertTrue(kdf.lastDerived!!.all { it == 0.toByte() })

        val unwrapped = envelope.unwrap(password, sealed.meta)
        assertArrayEquals(sealed.vaultKey, unwrapped)
        assertTrue(kdf.lastDerived!!.all { it == 0.toByte() })
    }

    @Test
    fun wrongPasswordFailsAtAeadLayer() {
        val sealed = envelope.create("master-password".toCharArray(), kdf.newParams(lowRam = false))

        assertThrows(AEADBadTagException::class.java) {
            envelope.unwrap("wrong-password".toCharArray(), sealed.meta)
        }
    }

    @Test
    fun derivedKeysDifferByPassword() {
        val params = kdf.newParams(lowRam = false)
        val first = kdf.derive("one".toCharArray(), params)
        val second = kdf.derive("two".toCharArray(), params)

        assertTrue(!first.contentEquals(second))
        assertEquals(32, first.size)
    }
}
