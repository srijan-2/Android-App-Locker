package com.antimatter.applocker.ui.unlock

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antimatter.applocker.AppLockerApp
import com.antimatter.applocker.ui.biometric.BiometricAuthenticator
import com.antimatter.applocker.ui.biometric.BiometricStatus
import com.antimatter.applocker.ui.biometric.biometricStatus
import com.antimatter.applocker.ui.common.findFragmentActivity
import com.antimatter.applocker.ui.pin.PinDots
import com.antimatter.applocker.ui.pin.PinKeypad
import com.antimatter.applocker.ui.pin.formatCountdown
import com.antimatter.applocker.ui.theme.Spacing

/**
 * PIN pad with an optional fingerprint key. Calls [onUnlocked] once, after a correct PIN
 * or a successful fingerprint check. [header] is shown above the title (for example an app icon).
 */
@Composable
fun UnlockScreen(
    title: String,
    subtitle: String,
    onUnlocked: () -> Unit,
    header: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as AppLockerApp
    val viewModel: UnlockViewModel = viewModel(
        factory = UnlockViewModel.factory(app.pinRepository, app.settingsRepository)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val view = LocalView.current

    val activity = remember(context) { context.findFragmentActivity() }
    val authenticator = remember(activity) { activity?.let { BiometricAuthenticator(it) } }
    var status by remember { mutableStateOf(biometricStatus(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { status = biometricStatus(context) }

    val biometricOffered =
        state.biometricEnabled && status == BiometricStatus.Available && authenticator != null

    fun promptBiometric() {
        authenticator?.authenticate(
            title = "Unlock App Locker",
            negativeText = "Use PIN",
            onSuccess = viewModel::onBiometricSuccess,
            onFallback = viewModel::onBiometricFallback,
            onInvalidated = viewModel::onBiometricInvalidated
        )
    }

    var autoPrompted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(biometricOffered, state.isLoaded) {
        if (biometricOffered && state.isLoaded && !autoPrompted) {
            autoPrompted = true
            promptBiometric()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.unlocked.collect { onUnlocked() }
    }
    LaunchedEffect(state.shakeCount) {
        if (state.shakeCount > 0) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        }
    }

    val locked = state.lockSecondsLeft > 0
    val message = if (locked) {
        "Too many attempts. Try again in ${formatCountdown(state.lockSecondsLeft)}."
    } else {
        state.message.orEmpty()
    }
    val biometricAction: (() -> Unit)? = if (biometricOffered) ({ promptBiometric() }) else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        header()
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = if (state.isBusy) "Checking…" else subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.xl))

        PinDots(
            length = state.pinLength,
            filled = state.enteredCount,
            shakeTrigger = state.shakeCount
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .heightIn(min = 36.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
        )

        Spacer(Modifier.weight(1f))

        PinKeypad(
            onDigit = viewModel::onDigit,
            onDelete = viewModel::onDelete,
            enabled = state.isLoaded && !state.isBusy && !locked,
            onBiometric = biometricAction
        )
        Spacer(Modifier.height(Spacing.lg))
    }
}
