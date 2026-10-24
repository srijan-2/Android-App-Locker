package com.antimatter.applocker.ui.apps

import com.antimatter.applocker.data.AppItem

/** Case-insensitive match on the app name. A blank query returns everything. */
fun filterApps(apps: List<AppItem>, query: String): List<AppItem> {
    val q = query.trim()
    return if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
}
