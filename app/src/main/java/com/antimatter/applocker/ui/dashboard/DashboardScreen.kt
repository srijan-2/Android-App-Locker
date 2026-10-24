package com.antimatter.applocker.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.antimatter.applocker.ui.components.GroupedDivider
import com.antimatter.applocker.ui.components.GroupedRow
import com.antimatter.applocker.ui.components.GroupedSection
import com.antimatter.applocker.ui.components.LargeTitle
import com.antimatter.applocker.ui.theme.LockerWarning
import com.antimatter.applocker.ui.theme.Spacing

@Composable
fun DashboardScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        LargeTitle("Protection")

        // Static placeholders until real state is wired in.
        GroupedSection(
            footer = "Monitoring starts once you grant the required permission."
        ) {
            GroupedRow(title = "Monitoring", value = "Off", dotColor = LockerWarning)
            GroupedDivider()
            GroupedRow(title = "Protected apps", value = "0")
            GroupedDivider()
            GroupedRow(title = "Relock", value = "Immediately")
        }
    }
}