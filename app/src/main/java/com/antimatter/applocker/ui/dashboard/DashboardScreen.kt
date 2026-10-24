package com.antimatter.applocker.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antimatter.applocker.AppLockerApp
import com.antimatter.applocker.service.isMonitoringEnabled
import com.antimatter.applocker.ui.components.GroupedDivider
import com.antimatter.applocker.ui.components.GroupedRow
import com.antimatter.applocker.ui.components.GroupedSection
import com.antimatter.applocker.ui.components.LargeTitle
import com.antimatter.applocker.ui.theme.LockerSuccess
import com.antimatter.applocker.ui.theme.LockerWarning
import com.antimatter.applocker.ui.theme.Spacing

@Composable
fun DashboardScreen(
    onSetupPin: () -> Unit,
    onChangePin: () -> Unit,
    onOpenMonitoring: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as AppLockerApp
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory(
            app.pinRepository,
            app.protectedAppsRepository,
            app.settingsRepository
        )
    )
    val pinState by viewModel.pinState.collectAsStateWithLifecycle()
    val protectedCount by viewModel.protectedCount.collectAsStateWithLifecycle()
    val relockDelay by viewModel.relockDelay.collectAsStateWithLifecycle()
    val pin = pinState

    // The switch lives in Android settings, so look again whenever this screen comes back.
    var monitoringOn by remember { mutableStateOf(isMonitoringEnabled(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { monitoringOn = isMonitoringEnabled(context) }

    val footer = when {
        pin != null && !pin.isSet -> "Set a PIN to start protecting apps."
        !monitoringOn -> "Turn on monitoring so protected apps ask for your PIN."
        else -> "Tap PIN to change it."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        LargeTitle("Protection")

        GroupedSection(footer = footer) {
            GroupedRow(
                title = "PIN",
                value = when {
                    pin == null -> null
                    pin.isSet -> "${pin.length} digits"
                    else -> "Not set"
                },
                showChevron = pin != null,
                onClick = when {
                    pin == null -> null
                    pin.isSet -> onChangePin
                    else -> onSetupPin
                }
            )
            GroupedDivider()
            GroupedRow(
                title = "Monitoring",
                value = if (monitoringOn) "On" else "Off",
                dotColor = if (monitoringOn) LockerSuccess else LockerWarning,
                showChevron = true,
                onClick = onOpenMonitoring
            )
            GroupedDivider()
            GroupedRow(title = "Protected apps", value = "$protectedCount")
            GroupedDivider()
            GroupedRow(title = "Relock", value = relockDelay.shortLabel)
        }
    }
}
