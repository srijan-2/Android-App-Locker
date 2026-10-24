package com.antimatter.applocker.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinRulesTest {
    @Test
    fun repeatedDigitsAreWeak() {
        assertTrue(PinRules.isWeak("000000"))
        assertTrue(PinRules.isWeak("999999"))
    }

    @Test
    fun straightRunsAreWeak() {
        assertTrue(PinRules.isWeak("123456"))
        assertTrue(PinRules.isWeak("012345"))
        assertTrue(PinRules.isWeak("654321"))
    }

    @Test
    fun ordinaryPinsAreNotWeak() {
        assertFalse(PinRules.isWeak("483920"))
        assertFalse(PinRules.isWeak("135792"))
        assertFalse(PinRules.isWeak("112233"))
    }
}
