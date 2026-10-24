package com.antimatter.applocker.ui.pin

import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val KeySize = 76.dp

@Composable
fun PinDots(
    length: Int,
    filled: Int,
    shakeTrigger: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shake = remember { Animatable(0f) }

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger == 0) return@LaunchedEffect
        val animationsEnabled = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) > 0f
        if (!animationsEnabled) return@LaunchedEffect
        for (target in listOf(-14f, 14f, -10f, 10f, -5f, 5f, 0f)) {
            shake.animateTo(target, tween(durationMillis = 45))
        }
    }

    Row(
        modifier = modifier
            .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
            .semantics { contentDescription = "$filled of $length digits entered" },
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        repeat(length) { index -> Dot(isFilled = index < filled) }
    }
}

@Composable
private fun Dot(isFilled: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isFilled) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pinDotScale"
    )
    val fill by animateColorAsState(
        targetValue = if (isFilled) MaterialTheme.colorScheme.onBackground else Color.Transparent,
        animationSpec = tween(durationMillis = 120),
        label = "pinDotFill"
    )
    val ring by animateColorAsState(
        targetValue = if (isFilled) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 120),
        label = "pinDotRing"
    )
    Box(
        modifier = Modifier
            .size(14.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(fill, CircleShape)
            .border(1.5.dp, ring, CircleShape)
    )
}

@Composable
fun PinKeypad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val view = LocalView.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { digit ->
                    DigitKey(digit = digit, enabled = enabled) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onDigit(digit)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Spacer(Modifier.size(KeySize))
            DigitKey(digit = '0', enabled = enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onDigit('0')
            }
            DeleteKey(enabled = enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onDelete()
            }
        }
    }
}

@Composable
private fun DigitKey(digit: Char, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(KeySize)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 30.sp),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun DeleteKey(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(KeySize)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}
