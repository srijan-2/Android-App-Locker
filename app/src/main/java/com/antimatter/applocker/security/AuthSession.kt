package com.antimatter.applocker.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory "the user has unlocked App Locker" flag. Never persisted. */
class AuthSession {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    fun markUnlocked() {
        _unlocked.value = true
    }

    fun lock() {
        _unlocked.value = false
    }
}
