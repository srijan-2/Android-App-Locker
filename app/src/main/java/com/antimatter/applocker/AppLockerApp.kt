package com.antimatter.applocker

import android.app.Application
import com.antimatter.applocker.data.AppIconLoader
import com.antimatter.applocker.data.InstalledAppsRepository
import com.antimatter.applocker.data.PinRepository
import com.antimatter.applocker.data.ProtectedAppsRepository
import com.antimatter.applocker.data.SettingsRepository
import com.antimatter.applocker.lock.LockEngine
import com.antimatter.applocker.security.AuthSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppLockerApp : Application() {
    val pinRepository: PinRepository by lazy { PinRepository(applicationContext) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(applicationContext) }
    val protectedAppsRepository: ProtectedAppsRepository by lazy { ProtectedAppsRepository(applicationContext) }
    val installedAppsRepository: InstalledAppsRepository by lazy { InstalledAppsRepository(applicationContext) }
    val appIconLoader: AppIconLoader by lazy { AppIconLoader(applicationContext) }
    val authSession: AuthSession = AuthSession()

    /** Shared by the accessibility service and the lock screen (same process, main thread). */
    val lockEngine: LockEngine = LockEngine()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Pre-warm PIN crypto so the first PIN check is not slower than later ones.
        appScope.launch { pinRepository.warmUp() }
        // Read the installed apps now, so the Apps tab is ready by the time it is opened.
        appScope.launch { installedAppsRepository.refresh() }
    }
}
