package com.antimatter.applocker.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.antimatter.applocker.AppLockerApp
import com.antimatter.applocker.lock.LockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Event-driven foreground detection: Android tells this service when a window changes,
 * so nothing polls and nothing runs in between. It asks for window-change events only and
 * cannot read screen content (see res/xml/accessibility_service_config.xml).
 *
 * Android re-starts enabled accessibility services by itself after a reboot, so no boot
 * receiver or foreground service is needed.
 */
class AppLockAccessibilityService : AccessibilityService() {

    private var scope: CoroutineScope? = null
    private var imePackages: Set<String> = emptySet()
    private var imeRefreshedAt = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val engine = app().lockEngine
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> engine.onScreenOff()
                Intent.ACTION_SCREEN_ON -> {
                    val keyguard = getSystemService(KeyguardManager::class.java)
                    if (keyguard != null && !keyguard.isKeyguardLocked) {
                        engine.onScreenOn()?.let(::showLock)
                    }
                }
                Intent.ACTION_USER_PRESENT -> engine.onScreenOn()?.let(::showLock)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val app = app()
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope?.cancel()
        scope = newScope

        newScope.launch {
            app.protectedAppsRepository.packages.collect { app.lockEngine.protectedPackages = it }
        }
        newScope.launch {
            app.settingsRepository.relockDelay.collect {
                app.lockEngine.relockDelayMillis = it.seconds * 1000L
            }
        }
        newScope.launch {
            // Without a PIN there is nothing to unlock with, so nothing is locked.
            app.pinRepository.state.collect { app.lockEngine.enabled = it.isSet }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        refreshImePackages()
        MonitoringRuntime.setConnected(true)
        Log.d(TAG, "monitoring connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val className = event.className?.toString()

        // Our own lock screen is not an app switch.
        if (pkg == packageName && className == LockActivity::class.java.name) return
        // Pop-ups, dialogs and the keyboard report framework class names; they are not app switches.
        if (className == null || className.startsWith("android.") || className.startsWith("androidx.")) return
        if (pkg in IGNORED_PACKAGES || pkg in imePackages()) return

        app().lockEngine.onForegroundChanged(pkg)?.let(::showLock)
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        MonitoringRuntime.setConnected(false)
        scope?.cancel()
        scope = null
        runCatching { unregisterReceiver(screenReceiver) }
        Log.d(TAG, "monitoring disconnected")
    }

    private fun showLock(pkg: String) {
        val intent = Intent(this, LockActivity::class.java)
            .putExtra(LockActivity.EXTRA_PACKAGE, pkg)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
        try {
            startActivity(intent)
            Log.d(TAG, "lock requested for $pkg")
        } catch (e: Exception) {
            Log.e(TAG, "Could not show the lock screen for $pkg", e)
        }
    }

    private fun imePackages(): Set<String> {
        if (SystemClock.elapsedRealtime() - imeRefreshedAt > IME_REFRESH_MILLIS) refreshImePackages()
        return imePackages
    }

    private fun refreshImePackages() {
        imeRefreshedAt = SystemClock.elapsedRealtime()
        imePackages = runCatching {
            getSystemService(InputMethodManager::class.java)
                .enabledInputMethodList
                .map { it.packageName }
                .toSet()
        }.getOrDefault(emptySet())
    }

    private fun app(): AppLockerApp = applicationContext as AppLockerApp

    private companion object {
        const val TAG = "AppLockService"
        const val IME_REFRESH_MILLIS = 60_000L
        val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller"
        )
    }
}
