package com.ciphervault.app.ui.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratorStateHolderTest {
    @Test
    fun initialGenerationUsesDefaultOptions() {
        val holder = GeneratorStateHolder()

        assertFalse(holder.passphraseMode)
        assertEquals(16, holder.generated.length)
        assertTrue(holder.strength in 0..4)
    }

    @Test
    fun optionChangesRegenerateImmediately() {
        val holder = GeneratorStateHolder()

        holder.setLength(24)
        assertEquals(24, holder.generated.length)

        holder.setUppercase(false)
        holder.setLowercase(false)
        holder.setDigits(true)
        holder.setSymbols(false)
        assertTrue(holder.generated.all { it.isDigit() })
    }

    @Test
    fun passphraseModeHonorsWordCount() {
        val holder = GeneratorStateHolder()

        holder.setMode(true)
        assertEquals(5, holder.generated.split("-").size)

        holder.changeWordCount(3)
        assertEquals(3, holder.generated.split("-").size)
    }
}
