package com.antimatter.applocker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.protectedAppsDataStore: DataStore<Preferences> by preferencesDataStore(name = "protected_apps_store")

/** The set of package names the user chose to protect. Survives restarts. */
class ProtectedAppsRepository(private val context: Context) {
    private object Keys {
        val PACKAGES = stringSetPreferencesKey("protected_packages")
    }

    val packages: Flow<Set<String>> =
        context.protectedAppsDataStore.data.map { it[Keys.PACKAGES] ?: emptySet() }

    suspend fun setProtected(packageName: String, isProtected: Boolean) {
        context.protectedAppsDataStore.edit { prefs ->
            val current = prefs[Keys.PACKAGES] ?: emptySet()
            prefs[Keys.PACKAGES] = if (isProtected) current + packageName else current - packageName
        }
    }
}
