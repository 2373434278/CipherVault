package com.ciphervault.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordGeneratorTest {
    @Test
    fun respectsLengthAndCharacterSets() {
        val options = PasswordOptions(length = 24, uppercase = true, lowercase = true, digits = true, symbols = true)
        repeat(20) {
            val password = PasswordGenerator.generate(options)
            assertEquals(24, password.length)
            assertTrue(password.any { it.isUpperCase() })
            assertTrue(password.any { it.isLowerCase() })
            assertTrue(password.any { it.isDigit() })
            assertTrue(password.any { !it.isLetterOrDigit() })
        }
    }

    @Test
    fun generatePassphraseHasRequestedWords() {
        val phrase = PasswordGenerator.generatePassphrase(wordCount = 6)
        assertEquals(6, phrase.split("-").size)
    }

    @Test
    fun strengthIncreasesWithComplexity() {
        assertTrue(PasswordGenerator.strength("abc") < PasswordGenerator.strength("Abcdefgh1234!"))
        assertEquals(4, PasswordGenerator.strength("A-very-long-passphrase-123!"))
    }

    @Test
    fun csvRoundTrip() {
        val rows = listOf(
            listOf("name", "notes"),
            listOf("GitHub", "line1,\nline2 \"quoted\""),
            listOf("Example", "simple"),
        )
        val encoded = CsvCodec.encode(rows)
        val decoded = CsvCodec.parse(encoded)
        assertEquals(rows, decoded)
    }
}
