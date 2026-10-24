package com.antimatter.applocker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.antimatter.applocker.data.RelockDelay
import com.antimatter.applocker.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {

    val biometricEnabled: StateFlow<Boolean> = settings.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val relockDelay: StateFlow<RelockDelay> = settings.relockDelay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RelockDelay.Immediately)

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setBiometricEnabled(enabled) }
    }

    fun setRelockDelay(delay: RelockDelay) {
        viewModelScope.launch { settings.setRelockDelay(delay) }
    }

    companion object {
        fun factory(settings: SettingsRepository) = viewModelFactory {
            initializer { SettingsViewModel(settings) }
        }
    }
}
