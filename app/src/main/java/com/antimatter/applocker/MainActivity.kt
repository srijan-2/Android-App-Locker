package com.antimatter.applocker

import android.graphics.Color
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.antimatter.applocker.ui.navigation.AppNavGraph
import com.antimatter.applocker.ui.theme.AppLockerTheme

// FragmentActivity (not plain ComponentActivity) because BiometricPrompt requires it.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            AppLockerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph()
                }
            }
        }
        holdSplashUntilAnimationEnds()
    }

    /**
     * Android removes the splash screen at the first drawn frame, which on a cold start is
     * earlier than the padlock animation ends. Hold that first frame until SPLASH_MILLIS have
     * passed since the process started. On warm starts that time has long passed, so nothing waits.
     */
    private fun holdSplashUntilAnimationEnds() {
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val sinceProcessStart =
                    SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()
                if (sinceProcessStart < SPLASH_MILLIS) return false
                if (content.viewTreeObserver.isAlive) {
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                }
                return true
            }
        })
    }

    override fun onStop() {
        super.onStop()
        // Re-lock Settings whenever the app leaves the foreground (not on rotation).
        if (!isChangingConfigurations) {
            (application as AppLockerApp).authSession.lock()
        }
    }

    private companion object {
        // Animation is 800 ms; the extra margin covers the start of the exit fade.
        const val SPLASH_MILLIS = 1_000L
    }
}