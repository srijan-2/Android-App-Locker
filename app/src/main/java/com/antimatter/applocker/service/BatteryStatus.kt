package com.antimatter.applocker.service

import android.content.Context
import android.os.PowerManager

/** True when Android is told not to restrict App Locker's background activity. */
fun isIgnoringBatteryOptimizations(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)
        ?.isIgnoringBatteryOptimizations(context.packageName) ?: false
