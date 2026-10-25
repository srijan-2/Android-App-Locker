package com.antimatter.applocker.ui.setup

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antimatter.applocker.AppLockerApp
import com.antimatter.applocker.service.MonitoringRuntime
import com.antimatter.applocker.service.MonitoringStatus
import com.antimatter.applocker.service.isIgnoringBatteryOptimizations
import com.antimatter.applocker.service.isMonitoringEnabled
import com.antimatter.applocker.service.monitoringStatus
import com.antimatter.applocker.ui.components.GroupedDivider
import com.antimatter.applocker.ui.components.GroupedSection
import com.antimatter.applocker.ui.components.LargeTitle
import com.antimatter.applocker.ui.theme.LockerSuccess
import com.antimatter.applocker.ui.theme.Spacing

private enum class StepState { Done, Todo, Optional }

@Composable
fun SetupScreen(
    onClose: () -> Unit,
    onCreatePin: () -> Unit,
    onOpenMonitoring: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as AppLockerApp
    val viewModel: SetupViewModel = viewModel(
        factory = SetupViewModel.factory(
            app.pinRepository,
            app.protectedAppsRepository,
            app.settingsRepository
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val connected by MonitoringRuntime.connected.collectAsStateWithLifecycle()

    // Both switches live in Android settings, so look again whenever this screen comes back.
    var monitoringEnabled by remember { mutableStateOf(isMonitoringEnabled(context)) }
    var batteryOk by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        monitoringEnabled = isMonitoringEnabled(context)
        batteryOk = isIgnoringBatteryOptimizations(context)
    }

    val monitoring = monitoringStatus(monitoringEnabled, connected)
    val done = requiredStepsDone(state.pinSet, monitoring == MonitoringStatus.Active, state.protectedCount)

    fun requestBatteryExemption() {
        val direct = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
        try {
            context.startActivity(direct)
        } catch (e: Exception) {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onClose) { Text("Done") }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            LargeTitle("Setup")
            Text(
                text = if (done == REQUIRED_STEPS) {
                    "All set. Protected apps are locked."
                } else {
                    "$done of $REQUIRED_STEPS steps done"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.xs)
            )
        }

        GroupedSection(footer = "These three steps are needed before any app is locked.") {
            SetupRow(
                title = "Set a PIN",
                subtitle = if (state.pinSet) "Your PIN is set." else "Needed to unlock protected apps.",
                step = if (state.pinSet) StepState.Done else StepState.Todo,
                onClick = if (state.pinSet) null else onCreatePin
            )
            GroupedDivider()
            SetupRow(
                title = "Turn on monitoring",
                subtitle = when (monitoring) {
                    MonitoringStatus.Active -> "Monitoring is on."
                    MonitoringStatus.NotRunning ->
                        "Switched on, but Android hasn’t started it. Turn it off and on again."
                    MonitoringStatus.Off -> "Lets App Locker notice when a protected app opens."
                },
                step = if (monitoring == MonitoringStatus.Active) StepState.Done else StepState.Todo,
                onClick = onOpenMonitoring
            )
            GroupedDivider()
            SetupRow(
                title = "Choose apps to protect",
                subtitle = if (state.protectedCount > 0) {
                    "${state.protectedCount} apps protected."
                } else {
                    "Pick the apps to lock."
                },
                step = if (state.protectedCount > 0) StepState.Done else StepState.Todo,
                onClick = onOpenApps
            )
        }

        Column {
            Text(
                text = "OPTIONAL",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.md, bottom = Spacing.xs)
            )
            GroupedSection(
                footer = "Battery: App Locker only runs when Android reports a window change, " +
                    "so it uses almost no power. Some phones still stop background apps; " +
                    "this tells Android not to."
            ) {
                SetupRow(
                    title = "Unlock with fingerprint",
                    subtitle = if (state.fingerprintOn) "Fingerprint unlock is on." else "Unlock faster than typing.",
                    step = if (state.fingerprintOn) StepState.Done else StepState.Optional,
                    onClick = onOpenSettings
                )
                GroupedDivider()
                SetupRow(
                    title = "Allow background activity",
                    subtitle = if (batteryOk) "Android won’t restrict App Locker." else "Recommended for reliability.",
                    step = if (batteryOk) StepState.Done else StepState.Optional,
                    onClick = if (batteryOk) null else ::requestBatteryExemption
                )
            }
        }
        Spacer(Modifier.size(Spacing.lg))
    }
}

@Composable
private fun SetupRow(
    title: String,
    subtitle: String,
    step: StepState,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
            )
            .semantics {
                stateDescription = when (step) {
                    StepState.Done -> "Done"
                    StepState.Todo -> "Not done"
                    StepState.Optional -> "Optional, not done"
                }
            }
            .heightIn(min = 64.dp)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (step == StepState.Done) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = LockerSuccess,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
