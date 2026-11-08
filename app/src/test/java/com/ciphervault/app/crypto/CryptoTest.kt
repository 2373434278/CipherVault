package com.ciphervault.app.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.GeneralSecurityException

class CryptoTest {
    @Test
    fun aesGcmRoundTrip() {
        val key = Crypto.randomKey()
        val plaintext = "vault-secret-测试".toByteArray()
        val encrypted = Crypto.encrypt(key, plaintext)
        assertArrayEquals(plaintext, Crypto.decrypt(key, encrypted))
    }

    @Test
    fun wrongKeyFails() {
        val encrypted = Crypto.encrypt(Crypto.randomKey(), "secret".toByteArray())
        assertThrows(GeneralSecurityException::class.java) {
            Crypto.decrypt(Crypto.randomKey(), encrypted)
        }
    }

    @Test
    fun encodingRoundTrip() {
        val bytes = Crypto.randomBytes(32)
        assertArrayEquals(bytes, Crypto.decode(Crypto.encode(bytes)))
    }
}
