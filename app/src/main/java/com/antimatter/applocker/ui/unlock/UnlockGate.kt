package com.antimatter.applocker.ui.unlock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.antimatter.applocker.AppLockerApp
import com.antimatter.applocker.data.PinState

/**
 * Shows [content] only after the PIN (or fingerprint) unlock, once a PIN exists.
 * The unlocked state is shared, so unlocking one gated screen unlocks the others
 * until the app leaves the foreground.
 */
@Composable
fun UnlockGate(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    val app = LocalContext.current.applicationContext as AppLockerApp
    val pinState by app.pinRepository.state.collectAsStateWithLifecycle<PinState?>(initialValue = null)
    val unlocked by app.authSession.unlocked.collectAsStateWithLifecycle()
    val pin = pinState

    when {
        pin == null -> Unit
        !pin.isSet || unlocked -> content()
        else -> UnlockScreen(
            title = title,
            subtitle = subtitle,
            onUnlocked = { app.authSession.markUnlocked() }
        )
    }
}
