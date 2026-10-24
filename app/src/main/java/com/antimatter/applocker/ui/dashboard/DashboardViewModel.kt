package com.antimatter.applocker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.antimatter.applocker.data.PinRepository
import com.antimatter.applocker.data.PinState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(repository: PinRepository) : ViewModel() {

    /** Null until the first value has loaded from storage. */
    val pinState: StateFlow<PinState?> = repository.state
        .map<PinState, PinState?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        fun factory(repository: PinRepository) = viewModelFactory {
            initializer { DashboardViewModel(repository) }
        }
    }
}
