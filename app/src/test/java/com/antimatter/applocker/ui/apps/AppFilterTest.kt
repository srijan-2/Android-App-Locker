package com.antimatter.applocker.ui.apps

import com.antimatter.applocker.data.AppItem
import org.junit.Assert.assertEquals
import org.junit.Test

class AppFilterTest {
    private val apps = listOf(
        AppItem("com.android.chrome", "Chrome"),
        AppItem("com.whatsapp", "WhatsApp"),
        AppItem("com.google.android.apps.photos", "Photos")
    )

    @Test
    fun blankQueryReturnsEverything() {
        assertEquals(apps, filterApps(apps, ""))
        assertEquals(apps, filterApps(apps, "   "))
    }

    @Test
    fun matchesIgnoringCase() {
        assertEquals(listOf(apps[1]), filterApps(apps, "whats"))
        assertEquals(listOf(apps[0]), filterApps(apps, "  CHROME "))
    }

    @Test
    fun noMatchGivesEmptyList() {
        assertEquals(emptyList<AppItem>(), filterApps(apps, "zzz"))
    }
}
