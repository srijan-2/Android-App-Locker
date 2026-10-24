package com.antimatter.applocker.lock

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.antimatter.applocker.AppLockerApp
import com.antimatter.applocker.ui.apps.AppIcon
import com.antimatter.applocker.ui.theme.AppLockerTheme
import com.antimatter.applocker.ui.unlock.UnlockScreen

/**
 * Full-screen PIN / fingerprint check shown on top of a protected app.
 * It is a FragmentActivity so the fingerprint prompt works here too.
 */
class LockActivity : FragmentActivity() {

    private var targetPackage by mutableStateOf("")
    private var unlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No screenshots, and a blank preview in the recents screen.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        targetPackage = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()

        // Back must never reveal the protected app underneath: go to the home screen instead.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goHome()
            }
        })

        setContent {
            AppLockerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LockContent(packageName = targetPackage, onUnlocked = ::handleUnlocked)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_PACKAGE)?.let { targetPackage = it }
    }

    override fun onStop() {
        super.onStop()
        if (unlocked || isFinishing || isChangingConfigurations) return
        // If the screen just turned off, stay put so the lock is still there when it turns on.
        val interactive = getSystemService(PowerManager::class.java)?.isInteractive ?: true
        if (interactive) {
            (application as AppLockerApp).lockEngine.onLockClosed(targetPackage)
            finishAndRemoveTask()
        }
    }

    private fun handleUnlocked() {
        if (unlocked) return
        unlocked = true
        (application as AppLockerApp).lockEngine.onUnlocked(targetPackage)
        finishAndRemoveTask()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun goHome() {
        (application as AppLockerApp).lockEngine.onLockClosed(targetPackage)
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finishAndRemoveTask()
    }

    companion object {
        const val EXTRA_PACKAGE = "package_name"
    }
}

@Composable
private fun LockContent(packageName: String, onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val label = remember(packageName) { appLabel(context.packageManager, packageName) }
    UnlockScreen(
        title = "Unlock $label",
        subtitle = "Enter your PIN to continue.",
        onUnlocked = onUnlocked,
        header = { if (packageName.isNotEmpty()) AppIcon(packageName = packageName, size = 64.dp) }
    )
}

@Suppress("DEPRECATION")
private fun appLabel(pm: PackageManager, packageName: String): String =
    runCatching { pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString() }
        .getOrDefault("this app")
