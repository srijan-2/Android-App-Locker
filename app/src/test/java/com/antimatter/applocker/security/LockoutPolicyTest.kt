package com.antimatter.applocker.security

import org.junit.Assert.assertEquals
import org.junit.Test

class LockoutPolicyTest {
    @Test
    fun noLockBeforeFifthFailure() {
        for (n in 0..4) assertEquals(0L, LockoutPolicy.lockDurationMillis(n))
    }

    @Test
    fun lockoutEscalates() {
        assertEquals(30_000L, LockoutPolicy.lockDurationMillis(5))
        assertEquals(60_000L, LockoutPolicy.lockDurationMillis(6))
        assertEquals(300_000L, LockoutPolicy.lockDurationMillis(7))
        assertEquals(900_000L, LockoutPolicy.lockDurationMillis(8))
        assertEquals(900_000L, LockoutPolicy.lockDurationMillis(20))
    }
}
