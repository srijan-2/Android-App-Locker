package com.antimatter.applocker.service

enum class MonitoringStatus { Off, NotRunning, Active }

/**
 * [enabledInSettings]: the switch in Android's accessibility settings is on.
 * [connected]: Android has actually started our service in this app process.
 */
fun monitoringStatus(enabledInSettings: Boolean, connected: Boolean): MonitoringStatus = when {
    !enabledInSettings -> MonitoringStatus.Off
    connected -> MonitoringStatus.Active
    else -> MonitoringStatus.NotRunning
}
