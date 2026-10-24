package com.antimatter.applocker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_store")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val RELOCK_SECONDS = intPreferencesKey("relock_seconds")
    }

    val biometricEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.BIOMETRIC_ENABLED] ?: false }

    val relockDelay: Flow<RelockDelay> =
        context.settingsDataStore.data.map {
            RelockDelay.fromSeconds(it[Keys.RELOCK_SECONDS] ?: RelockDelay.Immediately.seconds)
        }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setRelockDelay(delay: RelockDelay) {
        context.settingsDataStore.edit { it[Keys.RELOCK_SECONDS] = delay.seconds }
    }
}
