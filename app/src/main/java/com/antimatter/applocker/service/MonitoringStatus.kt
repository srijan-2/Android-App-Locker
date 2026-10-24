package com.antimatter.applocker.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/** True when the user has turned on the App Locker accessibility service in Android settings. */
fun isMonitoringEnabled(context: Context): Boolean {
    val resolver = context.contentResolver
    if (Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) != 1) return false
    val enabledServices =
        Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
    val ours = ComponentName(context, AppLockAccessibilityService::class.java)
    return enabledServices.split(':').any { ComponentName.unflattenFromString(it) == ours }
}
