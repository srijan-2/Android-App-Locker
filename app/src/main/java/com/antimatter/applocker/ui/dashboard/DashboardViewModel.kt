package com.antimatter.applocker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.antimatter.applocker.data.PinRepository
import com.antimatter.applocker.data.PinState
import com.antimatter.applocker.data.ProtectedAppsRepository
import com.antimatter.applocker.data.RelockDelay
import com.antimatter.applocker.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    pins: PinRepository,
    protectedApps: ProtectedAppsRepository,
    settings: SettingsRepository
) : ViewModel() {

    /** Null until the first value has loaded from storage. */
    val pinState: StateFlow<PinState?> = pins.state
        .map<PinState, PinState?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val protectedCount: StateFlow<Int> = protectedApps.packages
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val relockDelay: StateFlow<RelockDelay> = settings.relockDelay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RelockDelay.Immediately)

    companion object {
        fun factory(
            pins: PinRepository,
            protectedApps: ProtectedAppsRepository,
            settings: SettingsRepository
        ) = viewModelFactory {
            initializer { DashboardViewModel(pins, protectedApps, settings) }
        }
    }
}
