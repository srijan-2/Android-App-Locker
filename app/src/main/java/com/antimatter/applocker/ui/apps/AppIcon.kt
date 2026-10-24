package com.antimatter.applocker.ui.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.antimatter.applocker.AppLockerApp
import com.antimatter.applocker.ui.theme.LockerSurfaceRaised

/** An app icon loaded lazily (and cached), clipped to a rounded square. */
@Composable
fun AppIcon(packageName: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val loader = (context.applicationContext as AppLockerApp).appIconLoader
    val icon by produceState<ImageBitmap?>(
        initialValue = loader.cached(packageName),
        packageName
    ) {
        if (value == null) value = loader.load(packageName)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .background(LockerSurfaceRaised)
    ) {
        icon?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
