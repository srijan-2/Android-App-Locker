package com.antimatter.applocker.ui.monitoring

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.antimatter.applocker.R
import com.antimatter.applocker.service.MonitoringRuntime
import com.antimatter.applocker.service.MonitoringStatus
import com.antimatter.applocker.service.isMonitoringEnabled
import com.antimatter.applocker.service.monitoringStatus
import com.antimatter.applocker.ui.components.GroupedRow
import com.antimatter.applocker.ui.components.GroupedSection
import com.antimatter.applocker.ui.components.LargeTitle
import com.antimatter.applocker.ui.theme.LockerSuccess
import com.antimatter.applocker.ui.theme.LockerWarning
import com.antimatter.applocker.ui.theme.Spacing

// White text on the standard accent is about 3.7:1; this slightly deeper blue is about 4.7:1.
private val ButtonBlue = Color(0xFF0071E3)

@Composable
fun MonitoringSetupScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    var enabledInSettings by remember { mutableStateOf(isMonitoringEnabled(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { enabledInSettings = isMonitoringEnabled(context) }
    val connected by MonitoringRuntime.connected.collectAsStateWithLifecycle()
    val status = monitoringStatus(enabledInSettings, connected)

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
        LargeTitle("Monitoring")

        GroupedSection(
            footer = when (status) {
                MonitoringStatus.Active -> "Protected apps will now ask for your PIN or fingerprint."
                MonitoringStatus.NotRunning ->
                    "The switch is on, but Android hasn’t started the service. Turn it off and on again."
                MonitoringStatus.Off ->
                    "Monitoring is what notices a protected app opening. It needs one switch in Android settings."
            }
        ) {
            GroupedRow(
                title = "Status",
                value = when (status) {
                    MonitoringStatus.Active -> "On"
                    MonitoringStatus.NotRunning -> "Not running"
                    MonitoringStatus.Off -> "Off"
                },
                dotColor = if (status == MonitoringStatus.Active) LockerSuccess else LockerWarning
            )
        }

        if (status != MonitoringStatus.Active) {
            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonBlue,
                    contentColor = Color.White
                )
            ) {
                Text("Open Accessibility settings", style = MaterialTheme.typography.labelLarge)
            }

            InfoBlock(
                title = "Steps",
                body = "1. Tap the button above.\n" +
                    "2. Open Installed apps (or Downloaded apps) and choose $appName.\n" +
                    "3. Turn it on and confirm.\n\n" +
                    "On newer Android versions, if the switch is greyed out, open $appName’s " +
                    "App info, tap the three-dot menu and choose Allow restricted settings."
            )
        }

        InfoBlock(
            title = "How it works",
            body = "$appName uses an Android accessibility service to notice when a protected " +
                "app comes to the front, then asks for your PIN or fingerprint. It only runs " +
                "when Android reports that a window changed, so it uses almost no battery. " +
                "After a restart, Android turns it back on by itself."
        )
        InfoBlock(
            title = "What it can’t see",
            body = "$appName asks Android for window-change events only. It cannot read the " +
                "text on your screen or what you type."
        )
        InfoBlock(
            title = "Limits",
            body = "A protected app may show for a split second before the lock appears. " +
                "Anyone who can unlock your phone can turn monitoring off in Android settings, " +
                "so this protects against casual snooping, not a determined attacker.\n\n" +
                "To make that harder, you can also protect the Settings app on the Apps tab. " +
                "Be careful: if you forget your PIN, you won’t be able to open Settings."
        )
    }
}

@Composable
private fun InfoBlock(title: String, body: String) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
