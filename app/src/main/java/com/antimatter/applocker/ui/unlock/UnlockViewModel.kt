package com.antimatter.applocker.ui.unlock

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.antimatter.applocker.data.PinRepository
import com.antimatter.applocker.data.SettingsRepository
import com.antimatter.applocker.data.VerifyResult
import com.antimatter.applocker.security.LockoutPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UnlockUiState(
    val pinLength: Int = 4,
    val isLoaded: Boolean = false,
    val enteredCount: Int = 0,
    val message: String? = null,
    val shakeCount: Int = 0,
    val isBusy: Boolean = false,
    val lockSecondsLeft: Int = 0,
    val biometricEnabled: Boolean = false
)

class UnlockViewModel(
    private val pins: PinRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(UnlockUiState())
    val state: StateFlow<UnlockUiState> = _state.asStateFlow()

    // One-shot event, so a stale "unlocked" can never re-unlock after the session re-locks.
    private val _unlocked = Channel<Unit>(Channel.BUFFERED)
    val unlocked: Flow<Unit> = _unlocked.receiveAsFlow()

    private var current = ""
    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            pins.state.collect { pin ->
                _state.update {
                    it.copy(
                        pinLength = if (pin.isSet) pin.length else it.pinLength,
                        isLoaded = true
                    )
                }
            }
        }
        viewModelScope.launch {
            settings.biometricEnabled.collect { enabled ->
                _state.update { it.copy(biometricEnabled = enabled) }
            }
        }
    }

    fun onDigit(digit: Char) {
        val s = _state.value
        if (!s.isLoaded || s.isBusy || s.lockSecondsLeft > 0) return
        if (current.length >= s.pinLength) return
        current += digit
        _state.update { it.copy(enteredCount = current.length, message = null) }
        if (current.length == s.pinLength) verify(current)
    }

    fun onDelete() {
        val s = _state.value
        if (!s.isLoaded || s.isBusy || s.lockSecondsLeft > 0 || current.isEmpty()) return
        current = current.dropLast(1)
        _state.update { it.copy(enteredCount = current.length, message = null) }
    }

    fun onBiometricSuccess() {
        succeed()
    }

    fun onBiometricFallback(message: String?) {
        if (message != null) _state.update { it.copy(message = message) }
    }

    fun onBiometricInvalidated() {
        viewModelScope.launch {
            settings.setBiometricEnabled(false)
            _state.update {
                it.copy(
                    biometricEnabled = false,
                    message = "Your fingerprints changed, so fingerprint unlock was turned off. Use your PIN."
                )
            }
        }
    }

    private fun verify(pin: String) {
        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val start = SystemClock.elapsedRealtime()
            val result: VerifyResult? = try {
                pins.verify(pin)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Could not check PIN", e)
                null
            }
            Log.d(TAG, "PIN check took ${SystemClock.elapsedRealtime() - start} ms")
            current = ""
            when (result) {
                VerifyResult.Success, VerifyResult.NotSet -> succeed()
                is VerifyResult.WrongPin -> {
                    if (result.lockedUntilMillis > 0L) startCountdown(result.lockedUntilMillis)
                    val left = LockoutPolicy.LOCK_STARTS_AT - result.failedAttempts
                    val message = if (left > 0) {
                        "Incorrect PIN. $left ${if (left == 1) "try" else "tries"} left before a delay."
                    } else {
                        null
                    }
                    failAttempt(message)
                }
                is VerifyResult.LockedOut -> {
                    startCountdown(result.untilMillis)
                    failAttempt(null)
                }
                null -> failAttempt("Couldn’t check your PIN on this device.")
            }
        }
    }

    private fun succeed() {
        current = ""
        _state.update { it.copy(enteredCount = 0, message = null, isBusy = false) }
        _unlocked.trySend(Unit)
    }

    private fun failAttempt(message: String?) {
        _state.update {
            it.copy(
                enteredCount = 0,
                message = message,
                shakeCount = it.shakeCount + 1,
                isBusy = false
            )
        }
    }

    private fun startCountdown(untilMillis: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val remaining = untilMillis - System.currentTimeMillis()
                if (remaining <= 0L) break
                val seconds = ((remaining + 999L) / 1000L).toInt()
                _state.update { it.copy(lockSecondsLeft = seconds) }
                delay(250)
            }
            _state.update { it.copy(lockSecondsLeft = 0) }
        }
    }

    override fun onCleared() {
        current = ""
        countdownJob?.cancel()
        _unlocked.close()
    }

    companion object {
        private const val TAG = "Unlock"

        fun factory(pins: PinRepository, settings: SettingsRepository) = viewModelFactory {
            initializer { UnlockViewModel(pins, settings) }
        }
    }
}
