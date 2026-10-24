package com.antimatter.applocker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antimatter.applocker.AppLockerApp
import com.antimatter.applocker.data.RelockDelay
import com.antimatter.applocker.security.BiometricKey
import com.antimatter.applocker.ui.biometric.BiometricAuthenticator
import com.antimatter.applocker.ui.biometric.BiometricStatus
import com.antimatter.applocker.ui.biometric.biometricStatus
import com.antimatter.applocker.ui.common.findFragmentActivity
import com.antimatter.applocker.ui.components.GroupedCheckRow
import com.antimatter.applocker.ui.components.GroupedDivider
import com.antimatter.applocker.ui.components.GroupedRow
import com.antimatter.applocker.ui.components.GroupedSection
import com.antimatter.applocker.ui.components.GroupedSwitchRow
import com.antimatter.applocker.ui.components.LargeTitle
import com.antimatter.applocker.ui.theme.Spacing

@Composable
fun SettingsScreen(onChangePin: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as AppLockerApp
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(app.settingsRepository)
    )
    val fingerprintOn by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val relockDelay by viewModel.relockDelay.collectAsStateWithLifecycle()
    val activity = remember(context) { context.findFragmentActivity() }

    var status by remember { mutableStateOf(biometricStatus(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { status = biometricStatus(context) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun turnOnFingerprint() {
        val host = activity ?: return
        notice = null
        try {
            BiometricKey.createKey()
        } catch (e: Exception) {
            notice = "Couldn’t set up fingerprint unlock on this device."
            return
        }
        BiometricAuthenticator(host).authenticate(
            title = "Turn on fingerprint unlock",
            negativeText = "Cancel",
            onSuccess = { viewModel.setBiometricEnabled(true) },
            onFallback = { message ->
                BiometricKey.delete()
                notice = message
            },
            onInvalidated = {
                BiometricKey.delete()
                notice = "Couldn’t set up fingerprint unlock. Try again."
            }
        )
    }

    fun turnOffFingerprint() {
        notice = null
        viewModel.setBiometricEnabled(false)
        runCatching { BiometricKey.delete() }
    }

    val footer = when {
        status == BiometricStatus.Available ->
            "Use your fingerprint instead of typing your PIN. Your PIN always works as a backup."
        status == BiometricStatus.NoHardware -> "This phone has no fingerprint sensor."
        status == BiometricStatus.NoneEnrolled ->
            "Add a fingerprint in your phone’s Settings first."
        else -> "Fingerprint unlock isn’t available right now."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        LargeTitle("Settings")

        GroupedSection(footer = footer) {
            GroupedSwitchRow(
                title = "Unlock with fingerprint",
                checked = fingerprintOn,
                enabled = fingerprintOn || status == BiometricStatus.Available,
                onCheckedChange = { on -> if (on) turnOnFingerprint() else turnOffFingerprint() }
            )
        }

        notice?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = Spacing.md)
            )
        }

        Column {
            Text(
                text = "RELOCK",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.md, bottom = Spacing.xs)
            )
            GroupedSection(footer = "How long a protected app stays unlocked after you leave it.") {
                RelockDelay.entries.forEachIndexed { index, option ->
                    if (index > 0) GroupedDivider()
                    GroupedCheckRow(
                        title = option.label,
                        selected = option == relockDelay,
                        onClick = { viewModel.setRelockDelay(option) }
                    )
                }
            }
        }

        GroupedSection {
            GroupedRow(title = "Change PIN", showChevron = true, onClick = onChangePin)
        }
    }
}
