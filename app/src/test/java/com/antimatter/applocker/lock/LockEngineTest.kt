package com.antimatter.applocker.lock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LockEngineTest {
    private var now = 1_000_000L

    private fun engine(delayMillis: Long = 0L, enabled: Boolean = true): LockEngine =
        LockEngine { now }.also {
            it.enabled = enabled
            it.protectedPackages = setOf("a", "b")
            it.relockDelayMillis = delayMillis
        }

    @Test
    fun unprotectedAppNeverLocks() {
        assertNull(engine().onForegroundChanged("home"))
    }

    @Test
    fun protectedAppLocksWhenOpened() {
        assertEquals("a", engine().onForegroundChanged("a"))
    }

    @Test
    fun disabledEngineNeverLocks() {
        assertNull(engine(enabled = false).onForegroundChanged("a"))
    }

    @Test
    fun stayingInAnUnlockedAppDoesNotLockAgain() {
        val e = engine()
        e.onForegroundChanged("a")
        e.onUnlocked("a")
        assertNull(e.onForegroundChanged("a"))
    }

    @Test
    fun immediateDelayRelocksAfterLeaving() {
        val e = engine(delayMillis = 0L)
        e.onForegroundChanged("a")
        e.onUnlocked("a")
        e.onForegroundChanged("home")
        assertEquals("a", e.onForegroundChanged("a"))
    }

    @Test
    fun quickReturnWithinDelayDoesNotLock() {
        val e = engine(delayMillis = 30_000L)
        e.onForegroundChanged("a")
        e.onUnlocked("a")
        e.onForegroundChanged("home")
        now += 10_000L
        assertNull(e.onForegroundChanged("a"))
    }

    @Test
    fun returnAfterDelayLocks() {
        val e = engine(delayMillis = 30_000L)
        e.onForegroundChanged("a")
        e.onUnlocked("a")
        e.onForegroundChanged("home")
        now += 31_000L
        assertEquals("a", e.onForegroundChanged("a"))
    }

    @Test
    fun switchingBetweenUnlockedAppsWithinDelayDoesNotAskAgain() {
        val e = engine(delayMillis = 30_000L)
        e.onForegroundChanged("a")
        e.onUnlocked("a")
        assertEquals("b", e.onForegroundChanged("b"))
        e.onUnlocked("b")
        now += 5_000L
        assertNull(e.onForegroundChanged("a"))
        now += 5_000L
        assertNull(e.onForegroundChanged("b"))
    }

    @Test
    fun screenOffCountsAsLeavingWithImmediateDelay() {
        val e = engine(delayMillis = 0L)
        e.onForegroundChanged("a")
        e.onUnlocked("a")
        e.onScreenOff()
        assertEquals("a", e.onScreenOn())
    }

    @Test
    fun shortScreenOffWithinDelayStaysUnlocked() {
        val e = engine(delayMillis = 30_000L)
        e.onForegroundChanged("a")
        e.onUnlocked("a")
        e.onScreenOff()
        now += 5_000L
        assertNull(e.onScreenOn())
    }

    @Test
    fun screenOnWithNothingProtectedInFrontDoesNothing() {
        val e = engine()
        e.onForegroundChanged("home")
        e.onScreenOff()
        assertNull(e.onScreenOn())
    }

    @Test
    fun duplicateEventsDoNotRelaunchTheLock() {
        val e = engine()
        assertEquals("a", e.onForegroundChanged("a"))
        now += 200L
        assertNull(e.onForegroundChanged("a"))
        now += 2_000L
        assertEquals("a", e.onForegroundChanged("a"))
    }

    @Test
    fun closingTheLockWithoutUnlockingLocksAgainNextTime() {
        val e = engine()
        assertEquals("a", e.onForegroundChanged("a"))
        e.onLockClosed("a")
        e.onForegroundChanged("home")
        assertEquals("a", e.onForegroundChanged("a"))
    }
}
