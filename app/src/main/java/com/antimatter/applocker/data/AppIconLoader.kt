package com.antimatter.applocker.data

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads app icons on demand and keeps recent ones in memory. */
class AppIconLoader(private val context: Context, private val sizePx: Int = 144) {
    private val cache = LruCache<String, ImageBitmap>(256)

    fun cached(packageName: String): ImageBitmap? = cache.get(packageName)

    suspend fun load(packageName: String): ImageBitmap? = withContext(Dispatchers.Default) {
        cache.get(packageName)?.let { return@withContext it }
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bitmap = drawable.toBitmap(sizePx, sizePx).asImageBitmap()
            cache.put(packageName, bitmap)
            bitmap
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}
