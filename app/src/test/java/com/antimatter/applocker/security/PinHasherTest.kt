package com.antimatter.applocker.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    private val salt = ByteArray(16) { it.toByte() }
    private val otherSalt = ByteArray(16) { (it + 1).toByte() }
    private val fast = 1_000

    @Test
    fun sameInputsGiveSameHash() {
        val a = PinHasher.derive("123456".toCharArray(), salt, fast)
        val b = PinHasher.derive("123456".toCharArray(), salt, fast)
        assertArrayEquals(a, b)
    }

    @Test
    fun differentPinGivesDifferentHash() {
        val a = PinHasher.derive("123456".toCharArray(), salt, fast)
        val b = PinHasher.derive("123457".toCharArray(), salt, fast)
        assertFalse(PinHasher.constantTimeEquals(a, b))
    }

    @Test
    fun differentSaltGivesDifferentHash() {
        val a = PinHasher.derive("123456".toCharArray(), salt, fast)
        val b = PinHasher.derive("123456".toCharArray(), otherSalt, fast)
        assertFalse(PinHasher.constantTimeEquals(a, b))
    }

    @Test
    fun formatValidation() {
        assertTrue(PinHasher.isValidFormat("1234"))
        assertTrue(PinHasher.isValidFormat("12345678"))
        assertFalse(PinHasher.isValidFormat("123"))
        assertFalse(PinHasher.isValidFormat("123456789"))
        assertFalse(PinHasher.isValidFormat("12a4"))
        assertFalse(PinHasher.isValidFormat(""))
    }
}
