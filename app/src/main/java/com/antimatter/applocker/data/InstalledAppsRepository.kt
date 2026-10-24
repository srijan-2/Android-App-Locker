package com.antimatter.applocker.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

data class AppItem(val packageName: String, val label: String)

/**
 * Lists apps that have a launcher icon. Relies on the <queries> entry in the manifest
 * (package visibility, Android 11+); without it this list comes back almost empty.
 *
 * The result is kept in memory ([apps]) so screens can show it instantly; [refresh]
 * updates it in the background. It is also called once at app start.
 */
class InstalledAppsRepository(private val context: Context) {

    private val _apps = MutableStateFlow<List<AppItem>?>(null)

    /** Null until the first load has finished. */
    val apps: StateFlow<List<AppItem>?> = _apps.asStateFlow()

    private val refreshing = Mutex()

    /** Re-reads the installed apps. Does nothing if a refresh is already running. */
    suspend fun refresh() {
        if (!refreshing.tryLock()) return
        try {
            val start = SystemClock.elapsedRealtime()
            _apps.value = load()
            Log.d(TAG, "refresh: ${_apps.value?.size} apps in ${SystemClock.elapsedRealtime() - start} ms")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Could not load installed apps", e)
            if (_apps.value == null) _apps.value = emptyList()
        } finally {
            refreshing.unlock()
        }
    }

    private suspend fun load(): List<AppItem> = withContext(Dispatchers.Default) {
        val pm = context.packageManager

        val queryStart = SystemClock.elapsedRealtime()
        val launchable = queryLaunchable(pm)
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
        val queryMillis = SystemClock.elapsedRealtime() - queryStart

        // Reading each app's label is the slow part (it parses that app's resources),
        // so do it for many apps at once.
        val labelStart = SystemClock.elapsedRealtime()
        val items = coroutineScope {
            launchable
                .map { info ->
                    async(Dispatchers.IO) {
                        AppItem(info.activityInfo.packageName, info.loadLabel(pm).toString())
                    }
                }
                .awaitAll()
        }
        Log.d(
            TAG,
            "query ${queryMillis} ms, labels ${SystemClock.elapsedRealtime() - labelStart} ms"
        )

        items.sortedBy { it.label.lowercase() }
    }

    @Suppress("DEPRECATION")
    private fun queryLaunchable(pm: PackageManager): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            pm.queryIntentActivities(intent, 0)
        }
    }

    private companion object {
        const val TAG = "InstalledApps"
    }
}
