package com.antimatter.applocker.ui.pin

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.antimatter.applocker.data.PinRepository
import com.antimatter.applocker.data.VerifyResult
import com.antimatter.applocker.security.LockoutPolicy
import com.antimatter.applocker.security.PinRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Create: first-time setup. Change: verify the current PIN, then choose a new one. */
enum class PinFlowMode { Create, Change }

enum class PinSetupStep { Verify, Enter, Confirm }

data class PinSetupUiState(
    val step: PinSetupStep,
    /** Number of digits expected in the current step. */
    val pinLength: Int,
    val enteredCount: Int = 0,
    val message: String? = null,
    val shakeCount: Int = 0,
    /** True while loading, checking or saving; input is ignored. */
    val isBusy: Boolean = true,
    val lockSecondsLeft: Int = 0,
    val isDone: Boolean = false
)

class PinSetupViewModel(
    private val repository: PinRepository,
    private val mode: PinFlowMode
) : ViewModel() {

    private val startStep =
        if (mode == PinFlowMode.Change) PinSetupStep.Verify else PinSetupStep.Enter

    private val _state = MutableStateFlow(
        PinSetupUiState(step = startStep, pinLength = DEFAULT_LENGTH)
    )
    val state: StateFlow<PinSetupUiState> = _state.asStateFlow()

    // Digits are kept only in memory, only while this screen is open, and are never logged.
    private var current = ""
    private var oldPin = ""
    private var firstPin = ""
    private var storedLength = DEFAULT_LENGTH
    private var newLength = DEFAULT_LENGTH
    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            val stored = repository.state.first()
            when {
                // Guards: never create over an existing PIN, never "change" a missing one.
                mode == PinFlowMode.Create && stored.isSet -> {
                    _state.update { it.copy(isDone = true) }
                }
                mode == PinFlowMode.Change && !stored.isSet -> {
                    _state.update { it.copy(isDone = true) }
                }
                else -> {
                    if (mode == PinFlowMode.Change) {
                        storedLength = stored.length
                        newLength = stored.length
                    }
                    val length =
                        if (mode == PinFlowMode.Change) storedLength else newLength
                    _state.update { it.copy(pinLength = length, isBusy = false) }
                }
            }
        }
    }

    fun onDigit(digit: Char) {
        val s = _state.value
        if (s.isBusy || s.isDone || s.lockSecondsLeft > 0) return
        if (current.length >= s.pinLength) return
        current += digit
        _state.update { it.copy(enteredCount = current.length, message = null) }
        if (current.length == s.pinLength) onComplete()
    }

    fun onDelete() {
        val s = _state.value
        if (s.isBusy || s.isDone || s.lockSecondsLeft > 0 || current.isEmpty()) return
        current = current.dropLast(1)
        _state.update { it.copy(enteredCount = current.length, message = null) }
    }

    /** Switches the new PIN between 4 and 6 digits. Only before any digit is typed. */
    fun toggleLength() {
        val s = _state.value
        if (s.isBusy || s.step != PinSetupStep.Enter || current.isNotEmpty()) return
        newLength = if (newLength == SHORT_LENGTH) LONG_LENGTH else SHORT_LENGTH
        _state.update { it.copy(pinLength = newLength, message = null) }
    }

    private fun onComplete() {
        val pin = current
        when (_state.value.step) {
            PinSetupStep.Verify -> verifyCurrent(pin)
            PinSetupStep.Enter -> {
                when {
                    PinRules.isWeak(pin) ->
                        reject("That PIN is too easy to guess. Try another.")
                    mode == PinFlowMode.Change && pin == oldPin ->
                        reject("Choose a PIN different from your current one.")
                    else -> {
                        firstPin = pin
                        current = ""
                        _state.update {
                            it.copy(step = PinSetupStep.Confirm, enteredCount = 0)
                        }
                    }
                }
            }
            PinSetupStep.Confirm -> {
                if (pin != firstPin) {
                    firstPin = ""
                    reject("PINs don’t match. Start again.", PinSetupStep.Enter)
                } else {
                    save(pin)
                }
            }
        }
    }

    private fun verifyCurrent(pin: String) {
        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val start = SystemClock.elapsedRealtime()
            val result: VerifyResult? = try {
                repository.verify(pin)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Could not check PIN", e)
                null
            }
            Log.d(TAG, "PIN check took ${SystemClock.elapsedRealtime() - start} ms")
            current = ""
            when (result) {
                VerifyResult.Success -> {
                    oldPin = pin
                    _state.update {
                        it.copy(
                            step = PinSetupStep.Enter,
                            pinLength = newLength,
                            enteredCount = 0,
                            message = null,
                            isBusy = false
                        )
                    }
                }
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
                VerifyResult.NotSet -> _state.update { it.copy(isDone = true) }
                null -> failAttempt("Couldn’t check your PIN on this device.")
            }
        }
    }

    private fun save(pin: String) {
        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val start = SystemClock.elapsedRealtime()
            try {
                when (mode) {
                    PinFlowMode.Create -> repository.setPin(pin)
                    PinFlowMode.Change -> {
                        val result = repository.changePin(oldPin, pin)
                        check(result is VerifyResult.Success) { "changePin did not succeed" }
                    }
                }
                Log.d(TAG, "PIN saved in ${SystemClock.elapsedRealtime() - start} ms")
                clearSecrets()
                _state.update { it.copy(isBusy = false, isDone = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Could not save PIN", e)
                clearSecrets()
                resetToStart("Couldn’t save your PIN. Try again.")
            }
        }
    }

    private fun reject(message: String, toStep: PinSetupStep? = null) {
        current = ""
        _state.update {
            it.copy(
                step = toStep ?: it.step,
                enteredCount = 0,
                message = message,
                shakeCount = it.shakeCount + 1
            )
        }
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

    private fun resetToStart(message: String) {
        current = ""
        val length = if (mode == PinFlowMode.Change) storedLength else newLength
        _state.update {
            it.copy(
                step = startStep,
                pinLength = length,
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

    private fun clearSecrets() {
        current = ""
        oldPin = ""
        firstPin = ""
    }

    override fun onCleared() {
        clearSecrets()
        countdownJob?.cancel()
    }

    companion object {
        const val SHORT_LENGTH = 4
        const val LONG_LENGTH = 6
        private const val DEFAULT_LENGTH = SHORT_LENGTH
        private const val TAG = "PinSetup"

        fun factory(repository: PinRepository, mode: PinFlowMode) = viewModelFactory {
            initializer { PinSetupViewModel(repository, mode) }
        }
    }
}
