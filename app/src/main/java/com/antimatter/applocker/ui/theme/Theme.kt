package com.antimatter.applocker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = LockerAccent,
    onPrimary = LockerOnAccent,
    background = LockerBackground,
    onBackground = LockerOnBackground,
    surface = LockerSurface,
    onSurface = LockerOnBackground,
    surfaceVariant = LockerSurfaceRaised,
    onSurfaceVariant = LockerMuted,
    surfaceContainer = LockerSurface,
    surfaceContainerHigh = LockerSurfaceRaised,
    surfaceContainerHighest = LockerSurfaceRaised,
    outline = LockerSeparator,
    outlineVariant = LockerSeparator,
    error = LockerDanger
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun AppLockerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}