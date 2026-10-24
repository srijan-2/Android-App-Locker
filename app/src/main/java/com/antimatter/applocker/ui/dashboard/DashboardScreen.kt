package com.antimatter.applocker.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antimatter.applocker.AppLockerApp
import com.antimatter.applocker.ui.components.GroupedDivider
import com.antimatter.applocker.ui.components.GroupedRow
import com.antimatter.applocker.ui.components.GroupedSection
import com.antimatter.applocker.ui.components.LargeTitle
import com.antimatter.applocker.ui.theme.LockerWarning
import com.antimatter.applocker.ui.theme.Spacing

@Composable
fun DashboardScreen(
    onSetupPin: () -> Unit,
    onChangePin: () -> Unit
) {
    val app = LocalContext.current.applicationContext as AppLockerApp
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory(app.pinRepository)
    )
    val pinState by viewModel.pinState.collectAsStateWithLifecycle()
    val pin = pinState

    val footer = if (pin != null && !pin.isSet) {
        "Set a PIN to start protecting apps."
    } else {
        "Tap PIN to change it. Monitoring starts once you grant the required permission."
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
            // Static placeholders until monitoring exists.
            GroupedRow(title = "Monitoring", value = "Off", dotColor = LockerWarning)
            GroupedDivider()
            GroupedRow(title = "Protected apps", value = "0")
            GroupedDivider()
            GroupedRow(title = "Relock", value = "Immediately")
        }
    }
}
