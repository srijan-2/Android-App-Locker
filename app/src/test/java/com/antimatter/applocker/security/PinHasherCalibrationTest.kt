package com.antimatter.applocker.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherCalibrationTest {
    @Test
    fun calibrationStaysWithinBounds() {
        val iterations = PinHasher.calibrateIterations()
        assertTrue(iterations in PinHasher.MIN_ITERATIONS..PinHasher.MAX_ITERATIONS)
    }

    @Test
    fun tinyTargetClampsToMinimum() {
        assertEquals(PinHasher.MIN_ITERATIONS, PinHasher.calibrateIterations(targetMillis = 0))
    }

    @Test
    fun hugeTargetClampsToMaximum() {
        assertEquals(PinHasher.MAX_ITERATIONS, PinHasher.calibrateIterations(targetMillis = 100_000))
    }
}
