package com.antimatter.applocker.ui.navigation

sealed class Route(val path: String) {
    data object Protection : Route("protection")
    data object Apps : Route("apps")
    data object Settings : Route("settings")
    data object PinCreate : Route("pin_create")
    data object PinChange : Route("pin_change")
    data object MonitoringSetup : Route("monitoring_setup")
}
