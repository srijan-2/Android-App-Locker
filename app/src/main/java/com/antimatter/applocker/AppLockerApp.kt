package com.antimatter.applocker

import android.app.Application
import com.antimatter.applocker.data.PinRepository

class AppLockerApp : Application() {
    val pinRepository: PinRepository by lazy { PinRepository(applicationContext) }
}
