package com.ciphervault.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class PasswordGeneratorRandomTest {
    @Test
    fun injectedRandomMakesGenerationDeterministic() {
        val first = PasswordGenerator.generate(PasswordOptions(length = 24), Random(42))
        val second = PasswordGenerator.generate(PasswordOptions(length = 24), Random(42))

        assertEquals(first, second)
        assertEquals(24, first.length)
    }

    @Test
    fun everyEnabledClassAppearsAndAmbiguousCharactersAreAbsent() {
        repeat(50) {
            val password = PasswordGenerator.generate(
                PasswordOptions(length = 32, uppercase = true, lowercase = true, digits = true, symbols = true),
                Random(it.toLong()),
            )
            assertTrue(password.any { it.isUpperCase() })
            assertTrue(password.any { it.isLowerCase() })
            assertTrue(password.any { it.isDigit() })
            assertTrue(password.any { !it.isLetterOrDigit() })
            assertFalse(password.any { it in "IlO01" })
        }
    }

    @Test
    fun passphraseHonorsWordCountAndSeparator() {
        val passphrase = PasswordGenerator.generatePassphrase(wordCount = 6, separator = "-", random = Random(7))

        assertEquals(6, passphrase.split("-").size)
        assertTrue(passphrase.split("-").all { it.first().isUpperCase() })
    }

    @Test
    fun lengthIsClampedToSupportedRange() {
        assertEquals(8, PasswordGenerator.generate(PasswordOptions(length = 1), Random(1)).length)
        assertEquals(64, PasswordGenerator.generate(PasswordOptions(length = 200), Random(1)).length)
    }
}
