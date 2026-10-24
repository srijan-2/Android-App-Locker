package com.antimatter.applocker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RelockDelayTest {
    @Test
    fun mapsStoredSecondsBackToTheOption() {
        assertEquals(RelockDelay.Immediately, RelockDelay.fromSeconds(0))
        assertEquals(RelockDelay.Seconds5, RelockDelay.fromSeconds(5))
        assertEquals(RelockDelay.Seconds30, RelockDelay.fromSeconds(30))
        assertEquals(RelockDelay.Minute1, RelockDelay.fromSeconds(60))
    }

    @Test
    fun unknownValueFallsBackToImmediately() {
        assertEquals(RelockDelay.Immediately, RelockDelay.fromSeconds(7))
    }
}
