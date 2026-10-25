package com.antimatter.applocker.ui.setup

import org.junit.Assert.assertEquals
import org.junit.Test

class SetupProgressTest {
    @Test
    fun countsOnlyTheCompletedSteps() {
        assertEquals(0, requiredStepsDone(pinSet = false, monitoringActive = false, protectedCount = 0))
        assertEquals(1, requiredStepsDone(pinSet = true, monitoringActive = false, protectedCount = 0))
        assertEquals(2, requiredStepsDone(pinSet = true, monitoringActive = true, protectedCount = 0))
        assertEquals(3, requiredStepsDone(pinSet = true, monitoringActive = true, protectedCount = 4))
    }

    @Test
    fun allThreeIsTheRequiredTotal() {
        assertEquals(
            REQUIRED_STEPS,
            requiredStepsDone(pinSet = true, monitoringActive = true, protectedCount = 1)
        )
    }
}
