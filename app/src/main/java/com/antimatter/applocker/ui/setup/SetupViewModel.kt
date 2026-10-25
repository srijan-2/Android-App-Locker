package com.antimatter.applocker.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.antimatter.applocker.data.PinRepository
import com.antimatter.applocker.data.ProtectedAppsRepository
import com.antimatter.applocker.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SetupUiState(
    val pinSet: Boolean = false,
    val protectedCount: Int = 0,
    val fingerprintOn: Boolean = false
)

class SetupViewModel(
    pins: PinRepository,
    protectedApps: ProtectedAppsRepository,
    settings: SettingsRepository
) : ViewModel() {

    val state: StateFlow<SetupUiState> = combine(
        pins.state,
        protectedApps.packages,
        settings.biometricEnabled
    ) { pin, packages, fingerprint ->
        SetupUiState(
            pinSet = pin.isSet,
            protectedCount = packages.size,
            fingerprintOn = fingerprint
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SetupUiState())

    companion object {
        fun factory(
            pins: PinRepository,
            protectedApps: ProtectedAppsRepository,
            settings: SettingsRepository
        ) = viewModelFactory {
            initializer { SetupViewModel(pins, protectedApps, settings) }
        }
    }
}
