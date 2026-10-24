package com.antimatter.applocker.ui.pin

import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownFormatTest {
    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("0:00", formatCountdown(0))
        assertEquals("0:27", formatCountdown(27))
        assertEquals("1:00", formatCountdown(60))
        assertEquals("15:05", formatCountdown(905))
    }

    @Test
    fun negativeValuesClampToZero() {
        assertEquals("0:00", formatCountdown(-3))
    }
}
