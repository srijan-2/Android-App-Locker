package com.antimatter.applocker.service

import org.junit.Assert.assertEquals
import org.junit.Test

class MonitoringStateTest {
    @Test
    fun offWhenSwitchIsOff() {
        assertEquals(MonitoringStatus.Off, monitoringStatus(enabledInSettings = false, connected = false))
        assertEquals(MonitoringStatus.Off, monitoringStatus(enabledInSettings = false, connected = true))
    }

    @Test
    fun activeWhenOnAndRunning() {
        assertEquals(MonitoringStatus.Active, monitoringStatus(enabledInSettings = true, connected = true))
    }

    @Test
    fun notRunningWhenOnButNotStarted() {
        assertEquals(MonitoringStatus.NotRunning, monitoringStatus(enabledInSettings = true, connected = false))
    }
}
