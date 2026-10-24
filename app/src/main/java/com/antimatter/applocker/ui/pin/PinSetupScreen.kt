package com.antimatter.applocker.ui.pin

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antimatter.applocker.AppLockerApp
import com.antimatter.applocker.ui.theme.Spacing

@Composable
fun PinSetupScreen(mode: PinFlowMode, onClose: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as AppLockerApp
    val viewModel: PinSetupViewModel = viewModel(
        factory = PinSetupViewModel.factory(app.pinRepository, mode)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val view = LocalView.current

    LaunchedEffect(state.isDone) {
        if (state.isDone) onClose()
    }
    LaunchedEffect(state.shakeCount) {
        if (state.shakeCount > 0) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        }
    }

    val locked = state.lockSecondsLeft > 0
    val saving = state.isBusy && state.step == PinSetupStep.Confirm

    val title = when (state.step) {
        PinSetupStep.Verify -> "Enter your PIN"
        PinSetupStep.Enter ->
            if (mode == PinFlowMode.Create) "Create a PIN" else "Choose a new PIN"
        PinSetupStep.Confirm -> "Confirm your PIN"
    }
    val subtitle = when {
        saving -> "Saving…"
        state.isBusy && state.step == PinSetupStep.Verify -> "Checking…"
        state.step == PinSetupStep.Verify -> "Enter your current PIN to continue."
        state.step == PinSetupStep.Enter ->
            "Choose ${state.pinLength} digits to unlock your protected apps."
        else -> "Enter the same PIN once more."
    }
    val message = if (locked) {
        "Too many attempts. Try again in ${formatCountdown(state.lockSecondsLeft)}."
    } else {
        state.message.orEmpty()
    }
    val showLengthChoice =
        state.step == PinSetupStep.Enter && state.enteredCount == 0 && !state.isBusy

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(onClick = onClose, enabled = !saving) {
                Text("Cancel")
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = subtitle,
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
        Box(
            modifier = Modifier.heightIn(min = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showLengthChoice) {
                TextButton(onClick = viewModel::toggleLength) {
                    Text(
                        if (state.pinLength == PinSetupViewModel.SHORT_LENGTH) {
                            "Use a 6-digit PIN"
                        } else {
                            "Use a 4-digit PIN"
                        }
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        PinKeypad(
            onDigit = viewModel::onDigit,
            onDelete = viewModel::onDelete,
            enabled = !state.isBusy && !locked
        )
        Spacer(Modifier.height(Spacing.lg))
    }
}
